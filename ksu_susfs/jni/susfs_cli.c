#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/syscall.h>
#include <sys/utsname.h>
#include <sys/file.h>

#define KSU_MAGIC1 0xDEADBEEF
#define KSU_MAGIC2 0xCAFEBABE
#define SYS_REBOOT __NR_reboot

#define CMD_SUSFS_ADD_SUS_PATH     0x55550
#define CMD_SUSFS_REMOVE_SUS_PATH  0x55551
#define CMD_SUSFS_ADD_SUS_MOUNT    0x55560
#define CMD_SUSFS_ADD_SUS_KSTAT    0x55570
#define CMD_SUSFS_UPDATE_SUS_KSTAT 0x55571
#define CMD_SUSFS_ADD_TRY_UMOUNT   0x55580
#define CMD_SUSFS_SET_UNAME        0x55590
#define CMD_SUSFS_ENABLE_LOG       0x555a0
#define CMD_SUSFS_SHOW_VERSION     0x555e2
#define CMD_SUSFS_SUS_SU           0x60000

#define SUSFS_MAX_LEN_PATHNAME 256
#define SUSFS_STATE_DIR "/data/adb/susfs"
#define SUSFS_STATE_FILE "/data/adb/susfs/state.json"
#define SUSFS_STATE_TMP "/data/adb/susfs/state.json.tmp"
#define SUSFS_STATE_LOCK "/data/adb/susfs/state.lock"
#define SUSFS_SCHEMA_VERSION 1
#define MAX_ENTRIES 128

struct st_susfs_sus_path {
    unsigned long target_ino;
    char target_pathname[SUSFS_MAX_LEN_PATHNAME];
};

struct st_susfs_sus_mount {
    char target_pathname[SUSFS_MAX_LEN_PATHNAME];
};

struct st_susfs_try_umount {
    char target_pathname[SUSFS_MAX_LEN_PATHNAME];
    int mnt_mode;
};

struct st_susfs_sus_kstat {
    int                     is_statically;
    unsigned long           target_ino;
    char                    target_pathname[SUSFS_MAX_LEN_PATHNAME];
    unsigned long           spoofed_ino;
    unsigned long           spoofed_dev;
    unsigned int            spoofed_nlink;
    long long               spoofed_size;
    long                    spoofed_atime_tv_sec;
    long                    spoofed_mtime_tv_sec;
    long                    spoofed_ctime_tv_sec;
    long                    spoofed_atime_tv_nsec;
    long                    spoofed_mtime_tv_nsec;
    long                    spoofed_ctime_tv_nsec;
    unsigned long           spoofed_blksize;
    unsigned long long      spoofed_blocks;
};

struct st_susfs_uname {
    char release[65];
    char version[65];
};

struct st_sus_su {
    int enabled;
};

typedef struct {
    char path[SUSFS_MAX_LEN_PATHNAME];
    int is_loop;
    char source[16]; // "manual" or "auto_hide"
} sus_path_entry_t;

typedef struct {
    char path[SUSFS_MAX_LEN_PATHNAME];
} sus_mount_entry_t;

typedef struct {
    char path[SUSFS_MAX_LEN_PATHNAME];
    int mode;
} try_umount_entry_t;

typedef struct {
    char path[SUSFS_MAX_LEN_PATHNAME];
} sus_kstat_entry_t;

typedef struct {
    int schema;
    sus_path_entry_t sus_path[MAX_ENTRIES];
    int sus_path_count;

    sus_mount_entry_t sus_mount[MAX_ENTRIES];
    int sus_mount_count;

    try_umount_entry_t try_umount[MAX_ENTRIES];
    int try_umount_count;

    sus_kstat_entry_t sus_kstat[MAX_ENTRIES];
    int sus_kstat_count;

    char uname_release[65];
    char uname_version[65];
    int sus_su;
    int logging;
} susfs_state_t;

typedef struct {
    char type[32];
    char path[SUSFS_MAX_LEN_PATHNAME];
    char error[64];
} restore_error_t;

static int lock_state_fd = -1;

static void lock_state(void) {
    mkdir(SUSFS_STATE_DIR, 0700);
    lock_state_fd = open(SUSFS_STATE_LOCK, O_RDWR | O_CREAT, 0600);
    if (lock_state_fd >= 0) {
        flock(lock_state_fd, LOCK_EX);
    }
}

static void unlock_state(void) {
    if (lock_state_fd >= 0) {
        flock(lock_state_fd, LOCK_UN);
        close(lock_state_fd);
        lock_state_fd = -1;
    }
}

static int is_protected_path(const char *path, int force) {
    if (!path) return 0;
    // Core infrastructure is strictly protected (hard block even with --force)
    if (!strcmp(path, "/data/adb") ||
        !strcmp(path, "/data/adb/ksu") ||
        !strcmp(path, "/data/adb/susfs") ||
        !strcmp(path, "/data/adb/susfs/state.json")) {
        printf("[-] Error: Core root infrastructure path '%s' is strictly protected (hard block)!\n", path);
        return 1;
    }
    // su binaries require explicit --force flag
    if (!strcmp(path, "/system/bin/su") ||
        !strcmp(path, "/system/xbin/su") ||
        !strcmp(path, "/sbin/su")) {
        if (!force) {
            printf("[!] Policy Warning: Hiding '%s' requires --force (-f) flag or 'auto_hide enable'.\n", path);
            return 1;
        }
    }
    return 0;
}

static void init_default_state(susfs_state_t *state) {
    memset(state, 0, sizeof(susfs_state_t));
    state->schema = SUSFS_SCHEMA_VERSION;
    snprintf(state->uname_release, sizeof(state->uname_release), "default");
    snprintf(state->uname_version, sizeof(state->uname_version), "default");
}

static void load_state(susfs_state_t *state) {
    init_default_state(state);
    FILE *f = fopen(SUSFS_STATE_FILE, "r");
    if (!f) return;

    char line[512];
    int section = 0; // 1: sus_path, 2: sus_mount, 3: try_umount, 4: sus_kstat

    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "\"sus_path\"")) section = 1;
        else if (strstr(line, "\"sus_mount\"")) section = 2;
        else if (strstr(line, "\"try_umount\"")) section = 3;
        else if (strstr(line, "\"sus_kstat\"")) section = 4;

        if (section == 1) {
            if (strstr(line, "\"path\"")) {
                char p[SUSFS_MAX_LEN_PATHNAME] = {0};
                if (sscanf(line, " %*[^:]: \"%255[^\"]\"", p) == 1) {
                    if (state->sus_path_count < MAX_ENTRIES) {
                        strncpy(state->sus_path[state->sus_path_count].path, p, SUSFS_MAX_LEN_PATHNAME - 1);
                        strncpy(state->sus_path[state->sus_path_count].source, "manual", 15);
                        state->sus_path_count++;
                    }
                }
            } else if (strstr(line, "\"source\"")) {
                char src[16] = {0};
                if (sscanf(line, " %*[^:]: \"%15[^\"]\"", src) == 1) {
                    if (state->sus_path_count > 0) {
                        strncpy(state->sus_path[state->sus_path_count - 1].source, src, 15);
                    }
                }
            }
        } else if (section == 2 && strstr(line, "\"path\"")) {
            char p[SUSFS_MAX_LEN_PATHNAME] = {0};
            if (sscanf(line, " %*[^:]: \"%255[^\"]\"", p) == 1) {
                if (state->sus_mount_count < MAX_ENTRIES) {
                    strncpy(state->sus_mount[state->sus_mount_count].path, p, SUSFS_MAX_LEN_PATHNAME - 1);
                    state->sus_mount_count++;
                }
            }
        } else if (section == 3 && strstr(line, "\"path\"")) {
            char p[SUSFS_MAX_LEN_PATHNAME] = {0};
            if (sscanf(line, " %*[^:]: \"%255[^\"]\"", p) == 1) {
                if (state->try_umount_count < MAX_ENTRIES) {
                    strncpy(state->try_umount[state->try_umount_count].path, p, SUSFS_MAX_LEN_PATHNAME - 1);
                    state->try_umount[state->try_umount_count].mode = 0;
                    state->try_umount_count++;
                }
            }
        } else if (section == 4 && strstr(line, "\"path\"")) {
            char p[SUSFS_MAX_LEN_PATHNAME] = {0};
            if (sscanf(line, " %*[^:]: \"%255[^\"]\"", p) == 1) {
                if (state->sus_kstat_count < MAX_ENTRIES) {
                    strncpy(state->sus_kstat[state->sus_kstat_count].path, p, SUSFS_MAX_LEN_PATHNAME - 1);
                    state->sus_kstat_count++;
                }
            }
        }
    }
    fclose(f);
}

static void save_state_atomic(const susfs_state_t *state) {
    mkdir(SUSFS_STATE_DIR, 0700);
    int fd = open(SUSFS_STATE_TMP, O_WRONLY | O_CREAT | O_TRUNC, 0600);
    if (fd < 0) return;

    FILE *f = fdopen(fd, "w");
    if (!f) {
        close(fd);
        return;
    }

    fprintf(f, "{\n");
    fprintf(f, "  \"schema\": %d,\n", state->schema);

    // sus_path
    fprintf(f, "  \"sus_path\": [\n");
    for (int i = 0; i < state->sus_path_count; i++) {
        fprintf(f, "    {\n");
        fprintf(f, "      \"path\": \"%s\",\n", state->sus_path[i].path);
        fprintf(f, "      \"is_loop\": %s,\n", state->sus_path[i].is_loop ? "true" : "false");
        fprintf(f, "      \"source\": \"%s\"\n", state->sus_path[i].source[0] ? state->sus_path[i].source : "manual");
        fprintf(f, "    }%s\n", (i == state->sus_path_count - 1) ? "" : ",");
    }
    fprintf(f, "  ],\n");

    // sus_mount
    fprintf(f, "  \"sus_mount\": [\n");
    for (int i = 0; i < state->sus_mount_count; i++) {
        fprintf(f, "    {\n");
        fprintf(f, "      \"path\": \"%s\"\n", state->sus_mount[i].path);
        fprintf(f, "    }%s\n", (i == state->sus_mount_count - 1) ? "" : ",");
    }
    fprintf(f, "  ],\n");

    // try_umount
    fprintf(f, "  \"try_umount\": [\n");
    for (int i = 0; i < state->try_umount_count; i++) {
        fprintf(f, "    {\n");
        fprintf(f, "      \"path\": \"%s\",\n", state->try_umount[i].path);
        fprintf(f, "      \"mode\": %d\n", state->try_umount[i].mode);
        fprintf(f, "    }%s\n", (i == state->try_umount_count - 1) ? "" : ",");
    }
    fprintf(f, "  ],\n");

    // sus_kstat
    fprintf(f, "  \"sus_kstat\": [\n");
    for (int i = 0; i < state->sus_kstat_count; i++) {
        fprintf(f, "    {\n");
        fprintf(f, "      \"path\": \"%s\"\n", state->sus_kstat[i].path);
        fprintf(f, "    }%s\n", (i == state->sus_kstat_count - 1) ? "" : ",");
    }
    fprintf(f, "  ],\n");

    // uname & settings
    fprintf(f, "  \"set_uname\": {\n");
    fprintf(f, "    \"release\": \"%s\",\n", state->uname_release);
    fprintf(f, "    \"version\": \"%s\"\n", state->uname_version);
    fprintf(f, "  },\n");
    fprintf(f, "  \"sus_su\": %d,\n", state->sus_su);
    fprintf(f, "  \"logging\": %d\n", state->logging);
    fprintf(f, "}\n");

    fflush(f);
    fsync(fileno(f));
    fclose(f);

    rename(SUSFS_STATE_TMP, SUSFS_STATE_FILE);
}

static int get_ksu_fd_silent(void) {
    if (getuid() != 0) {
        return -1;
    }
    int reply_fd = -1;
    syscall(SYS_REBOOT, KSU_MAGIC1, KSU_MAGIC2, 0, &reply_fd);
    if (reply_fd < 0) {
        return -1;
    }
    return reply_fd;
}

static int get_ksu_fd(void) {
    if (getuid() != 0) {
        printf("[-] Error: Must run as root (su)!\n");
        exit(1);
    }

    int reply_fd = -1;
    long res = syscall(SYS_REBOOT, KSU_MAGIC1, KSU_MAGIC2, 0, &reply_fd);
    if (reply_fd < 0) {
        printf("[-] Error: Failed to acquire KernelSU supercall FD (res: %ld)\n", res);
        exit(1);
    }
    return reply_fd;
}

static int check_feature(int fd, unsigned long cmd) {
    if (fd < 0) return 0;
    errno = 0;
    int ret = ioctl(fd, cmd, NULL);
    if (ret != -1 || errno != ENOTTY) {
        return 1;
    }
    return 0;
}

static void print_status_json(void) {
    struct utsname uts;
    char kernel_release[128] = "unknown";
    if (uname(&uts) == 0) {
        snprintf(kernel_release, sizeof(kernel_release), "%s", uts.release);
    }

    int fd = get_ksu_fd_silent();
    int supported = 0;
    char version_str[32] = "";

    if (fd >= 0) {
        if (ioctl(fd, CMD_SUSFS_SHOW_VERSION, version_str) == 0) {
            supported = 1;
            if (version_str[0] == 'v' || version_str[0] == 'V') {
                memmove(version_str, version_str + 1, strlen(version_str) + 1);
            }
        }
    }

    int feat_sus_path = check_feature(fd, CMD_SUSFS_ADD_SUS_PATH);
    int feat_sus_mount = check_feature(fd, CMD_SUSFS_ADD_SUS_MOUNT);
    int feat_sus_kstat = check_feature(fd, CMD_SUSFS_ADD_SUS_KSTAT);
    int feat_set_uname = check_feature(fd, CMD_SUSFS_SET_UNAME);
    int feat_try_umount = check_feature(fd, CMD_SUSFS_ADD_TRY_UMOUNT);
    int feat_sus_su = check_feature(fd, CMD_SUSFS_SUS_SU);

    if (fd >= 0) {
        close(fd);
    }

    printf("{\n");
    printf("  \"supported\": %s,\n", supported ? "true" : "false");
    printf("  \"version\": \"%s\",\n", supported ? version_str : "");
    printf("  \"kernel\": \"%s\",\n", kernel_release);
    printf("  \"features\": {\n");
    printf("    \"sus_path\": %s,\n", feat_sus_path ? "true" : "false");
    printf("    \"sus_mount\": %s,\n", feat_sus_mount ? "true" : "false");
    printf("    \"sus_kstat\": %s,\n", feat_sus_kstat ? "true" : "false");
    printf("    \"set_uname\": %s,\n", feat_set_uname ? "true" : "false");
    printf("    \"try_umount\": %s,\n", feat_try_umount ? "true" : "false");
    printf("    \"sus_su\": %s\n", feat_sus_su ? "true" : "false");
    printf("  }\n");
    printf("}\n");
}

static void cmd_list(int json_mode) {
    lock_state();
    susfs_state_t state;
    load_state(&state);
    unlock_state();

    int fd = get_ksu_fd_silent();
    int ksu_available = (fd >= 0);
    if (fd >= 0) close(fd);

    if (json_mode) {
        printf("{\n");
        printf("  \"schema\": %d,\n", state.schema);
        printf("  \"sus_path\": [\n");
        for (int i = 0; i < state.sus_path_count; i++) {
            struct stat sb;
            int path_stat = stat(state.sus_path[i].path, &sb);
            int active = 0;
            if (ksu_available) {
                // Check if target file or its direct file inode exists
                if (path_stat == 0) {
                    active = 1;
                } else {
                    // Check if file exists via LSTAT or parent directory stat
                    struct stat lsb;
                    if (lstat(state.sus_path[i].path, &lsb) == 0) {
                        active = 1;
                    } else {
                        // Target path does not exist on disk at all
                        active = 0;
                    }
                }
            }
            printf("    {\"path\": \"%s\", \"is_loop\": %s, \"source\": \"%s\", \"configured\": true, \"active\": %s}%s\n",
                   state.sus_path[i].path,
                   state.sus_path[i].is_loop ? "true" : "false",
                   state.sus_path[i].source[0] ? state.sus_path[i].source : "manual",
                   active ? "true" : "false",
                   (i == state.sus_path_count - 1) ? "" : ",");
        }
        printf("  ],\n");

        printf("  \"sus_mount\": [\n");
        for (int i = 0; i < state.sus_mount_count; i++) {
            printf("    {\"path\": \"%s\", \"configured\": true, \"active\": %s}%s\n",
                   state.sus_mount[i].path,
                   ksu_available ? "true" : "false",
                   (i == state.sus_mount_count - 1) ? "" : ",");
        }
        printf("  ],\n");

        printf("  \"try_umount\": [\n");
        for (int i = 0; i < state.try_umount_count; i++) {
            printf("    {\"path\": \"%s\", \"mode\": %d, \"configured\": true, \"active\": %s}%s\n",
                   state.try_umount[i].path, state.try_umount[i].mode,
                   ksu_available ? "true" : "false",
                   (i == state.try_umount_count - 1) ? "" : ",");
        }
        printf("  ],\n");

        printf("  \"sus_kstat\": [\n");
        for (int i = 0; i < state.sus_kstat_count; i++) {
            printf("    {\"path\": \"%s\", \"configured\": true, \"active\": %s}%s\n",
                   state.sus_kstat[i].path,
                   ksu_available ? "true" : "false",
                   (i == state.sus_kstat_count - 1) ? "" : ",");
        }
        printf("  ]\n");
        printf("}\n");
    } else {
        printf("=== Active SUSFS Configured Rules ===\n");
        printf("Schema: %d\n", state.schema);
        printf("\n[ SUS Path ] (%d entries)\n", state.sus_path_count);
        for (int i = 0; i < state.sus_path_count; i++) {
            printf("  - %s (loop: %s, source: %s)\n",
                   state.sus_path[i].path,
                   state.sus_path[i].is_loop ? "yes" : "no",
                   state.sus_path[i].source[0] ? state.sus_path[i].source : "manual");
        }
        printf("\n[ SUS Mount ] (%d entries)\n", state.sus_mount_count);
        for (int i = 0; i < state.sus_mount_count; i++) {
            printf("  - %s\n", state.sus_mount[i].path);
        }
        printf("\n[ Try Umount ] (%d entries)\n", state.try_umount_count);
        for (int i = 0; i < state.try_umount_count; i++) {
            printf("  - %s (mode: %d)\n", state.try_umount[i].path, state.try_umount[i].mode);
        }
        printf("\n[ SUS Kstat ] (%d entries)\n", state.sus_kstat_count);
        for (int i = 0; i < state.sus_kstat_count; i++) {
            printf("  - %s\n", state.sus_kstat[i].path);
        }
    }
}

static int cmd_restore(int json_mode) {
    lock_state();
    susfs_state_t state;
    load_state(&state);

    int fd = get_ksu_fd_silent();
    if (fd < 0) {
        unlock_state();
        if (json_mode) printf("{\"restored\":0,\"failed\":1,\"errors\":[{\"type\":\"kernel\",\"path\":\"/\",\"error\":\"Failed root supercall\"}]}\n");
        else printf("[-] Error: Root supercall failed during restore.\n");
        return 1;
    }

    int restored = 0;
    int failed = 0;
    restore_error_t errors[MAX_ENTRIES];
    int error_count = 0;

    // Restore sus_path (Idempotent)
    for (int i = 0; i < state.sus_path_count; i++) {
        struct stat sb;
        unsigned long target_ino = 0;
        if (stat(state.sus_path[i].path, &sb) == 0) {
            target_ino = sb.st_ino;
        }
        struct st_susfs_sus_path info = {0};
        info.target_ino = target_ino;
        strncpy(info.target_pathname, state.sus_path[i].path, SUSFS_MAX_LEN_PATHNAME - 1);
        int ret = ioctl(fd, CMD_SUSFS_ADD_SUS_PATH, &info);
        if (ret == 0 || errno == EEXIST || target_ino == 0) {
            restored++;
        } else {
            failed++;
            if (error_count < MAX_ENTRIES) {
                strncpy(errors[error_count].type, "sus_path", 31);
                strncpy(errors[error_count].path, state.sus_path[i].path, SUSFS_MAX_LEN_PATHNAME - 1);
                strncpy(errors[error_count].error, strerror(errno), 63);
                error_count++;
            }
        }
    }

    // Restore sus_mount (Idempotent)
    for (int i = 0; i < state.sus_mount_count; i++) {
        struct st_susfs_sus_mount info = {0};
        strncpy(info.target_pathname, state.sus_mount[i].path, SUSFS_MAX_LEN_PATHNAME - 1);
        int ret = ioctl(fd, CMD_SUSFS_ADD_SUS_MOUNT, &info);
        if (ret == 0 || errno == EEXIST) {
            restored++;
        } else {
            failed++;
            if (error_count < MAX_ENTRIES) {
                strncpy(errors[error_count].type, "sus_mount", 31);
                strncpy(errors[error_count].path, state.sus_mount[i].path, SUSFS_MAX_LEN_PATHNAME - 1);
                strncpy(errors[error_count].error, strerror(errno), 63);
                error_count++;
            }
        }
    }

    // Restore try_umount (Idempotent)
    for (int i = 0; i < state.try_umount_count; i++) {
        struct st_susfs_try_umount info = {0};
        strncpy(info.target_pathname, state.try_umount[i].path, SUSFS_MAX_LEN_PATHNAME - 1);
        info.mnt_mode = state.try_umount[i].mode;
        int ret = ioctl(fd, CMD_SUSFS_ADD_TRY_UMOUNT, &info);
        if (ret == 0 || errno == EEXIST) {
            restored++;
        } else {
            failed++;
            if (error_count < MAX_ENTRIES) {
                strncpy(errors[error_count].type, "try_umount", 31);
                strncpy(errors[error_count].path, state.try_umount[i].path, SUSFS_MAX_LEN_PATHNAME - 1);
                strncpy(errors[error_count].error, strerror(errno), 63);
                error_count++;
            }
        }
    }

    // Restore uname
    if (strcmp(state.uname_release, "default") || strcmp(state.uname_version, "default")) {
        struct st_susfs_uname info = {0};
        strncpy(info.release, state.uname_release, 64);
        strncpy(info.version, state.uname_version, 64);
        if (ioctl(fd, CMD_SUSFS_SET_UNAME, &info) == 0) {
            restored++;
        }
    }

    close(fd);
    unlock_state();

    if (json_mode) {
        printf("{\n");
        printf("  \"restored\": %d,\n", restored);
        printf("  \"failed\": %d,\n", failed);
        printf("  \"errors\": [\n");
        for (int i = 0; i < error_count; i++) {
            printf("    {\"type\": \"%s\", \"path\": \"%s\", \"error\": \"%s\"}%s\n",
                   errors[i].type, errors[i].path, errors[i].error,
                   (i == error_count - 1) ? "" : ",");
        }
        printf("  ]\n");
        printf("}\n");
    } else {
        printf("[+] Boot Restore Complete: %d rules restored, %d failed.\n", restored, failed);
        for (int i = 0; i < error_count; i++) {
            printf("  [-] Failed [%s] %s: %s\n", errors[i].type, errors[i].path, errors[i].error);
        }
    }

    return (failed > 0) ? 1 : 0;
}

static void cmd_auto_hide(const char *subcmd, int json_mode) {
    const char *preset_paths[] = {
        "/data/adb/modules",
        "/system/bin/su",
        "/system/xbin/su"
    };
    int num_presets = sizeof(preset_paths) / sizeof(preset_paths[0]);

    if (!subcmd || !strcmp(subcmd, "status")) {
        lock_state();
        susfs_state_t state;
        load_state(&state);
        unlock_state();

        int enabled_count = 0;
        for (int i = 0; i < num_presets; i++) {
            for (int j = 0; j < state.sus_path_count; j++) {
                if (!strcmp(state.sus_path[j].path, preset_paths[i])) {
                    enabled_count++;
                    break;
                }
            }
        }
        int is_enabled = (enabled_count == num_presets);
        if (json_mode) {
            printf("{\"auto_hide\":\"%s\", \"preset_count\":%d, \"active_count\":%d}\n",
                   is_enabled ? "enabled" : (enabled_count > 0 ? "partial" : "disabled"),
                   num_presets, enabled_count);
        } else {
            printf("Auto-Hide Status: %s (%d/%d preset paths configured)\n",
                   is_enabled ? "ENABLED" : (enabled_count > 0 ? "PARTIAL" : "DISABLED"),
                   enabled_count, num_presets);
        }
    } else if (!strcmp(subcmd, "enable")) {
        printf("[+] Enabling Auto-Hide Presets...\n");
        for (int i = 0; i < num_presets; i++) {
            struct stat sb;
            if (stat(preset_paths[i], &sb) == 0) {
                char cmd[512];
                snprintf(cmd, sizeof(cmd), "susfs add_sus_path %s --force --source=auto_hide", preset_paths[i]);
                system(cmd);
            }
        }
    } else if (!strcmp(subcmd, "disable")) {
        printf("[+] Disabling Auto-Hide Presets (removing auto_hide tagged entries only)...\n");
        lock_state();
        susfs_state_t state;
        load_state(&state);

        int fd = get_ksu_fd_silent();
        int new_count = 0;
        sus_path_entry_t new_entries[MAX_ENTRIES];

        for (int i = 0; i < state.sus_path_count; i++) {
            int is_preset = 0;
            for (int p = 0; p < num_presets; p++) {
                if (!strcmp(state.sus_path[i].path, preset_paths[p])) {
                    is_preset = 1;
                    break;
                }
            }
            if (is_preset && !strcmp(state.sus_path[i].source, "auto_hide")) {
                if (fd >= 0) {
                    struct st_susfs_sus_path info = {0};
                    struct stat sb;
                    if (stat(state.sus_path[i].path, &sb) == 0) info.target_ino = sb.st_ino;
                    strncpy(info.target_pathname, state.sus_path[i].path, SUSFS_MAX_LEN_PATHNAME - 1);
                    ioctl(fd, CMD_SUSFS_REMOVE_SUS_PATH, &info);
                }
                printf("[+] Auto-hide preset removed: %s\n", state.sus_path[i].path);
            } else {
                new_entries[new_count++] = state.sus_path[i];
            }
        }
        if (fd >= 0) close(fd);
        memcpy(state.sus_path, new_entries, sizeof(sus_path_entry_t) * new_count);
        state.sus_path_count = new_count;
        save_state_atomic(&state);
        unlock_state();
    } else {
        printf("Usage: susfs auto_hide <status|enable|disable>\n");
    }
}

static void print_help(void) {
    printf("VoidSU Standalone SUSFS CLI Tool v1.4.2 (Native ARM64)\n");
    printf("Usage: susfs <command> [args]\n\n");
    printf("Commands:\n");
    printf("  show version                 Show SUSFS kernel engine version\n");
    printf("  add_sus_path <path> [--force] Hide file/directory from non-root app processes\n");
    printf("  remove_sus_path <path>       Remove hidden file/directory from sus_path list\n");
    printf("  add_sus_mount <mount_path>   Hide mountpoint from /proc/self/mountinfo\n");
    printf("  add_try_umount <path> [mode] Umount mountpoint for non-root UIDs\n");
    printf("  add_sus_kstat <path>         Spoof kstat attributes for target path\n");
    printf("  set_uname <release> <ver>    Spoof kernel release and version strings\n");
    printf("  sus_su <0|1>                 Enable/disable su binary hiding from non-root\n");
    printf("  enable_log <0|1>             Enable (1) or disable (0) kernel debug logging\n");
    printf("  status [--json]              Check live SUSFS kernel engine status\n");
    printf("  list [--json]                List all persistently configured SUSFS rules\n");
    printf("  restore [--json]             Restore all persistent rules on boot/demand\n");
    printf("  auto_hide <status|enable|disable> 1-Click auto hide preset management\n");
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        print_help();
        return 0;
    }

    int json_mode = 0;
    int force_flag = 0;
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--json") || !strcmp(argv[i], "-j")) {
            json_mode = 1;
        }
        if (!strcmp(argv[i], "--force") || !strcmp(argv[i], "-f")) {
            force_flag = 1;
        }
    }

    if (!strcmp(argv[1], "show") && argc >= 3 && !strcmp(argv[2], "version")) {
        int fd = get_ksu_fd();
        char ver[32] = {0};
        if (ioctl(fd, CMD_SUSFS_SHOW_VERSION, ver) == 0) {
            if (json_mode) {
                printf("{\"version\":\"%s\"}\n", ver);
            } else {
                printf("SUSFS Version: %s\n", ver);
            }
        } else {
            if (json_mode) {
                printf("{\"error\":\"failed to fetch version\"}\n");
            } else {
                printf("[-] Error fetching SUSFS version\n");
            }
        }
        close(fd);
    } else if (!strcmp(argv[1], "add_sus_path") && argc >= 3) {
        if (is_protected_path(argv[2], force_flag)) {
            return 1;
        }
        char src_tag[16] = "manual";
        for (int i = 3; i < argc; i++) {
            if (!strncmp(argv[i], "--source=", 9)) {
                strncpy(src_tag, argv[i] + 9, 15);
            }
        }
        struct stat sb;
        if (stat(argv[2], &sb) != 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            int exists = 0;
            for (int i = 0; i < state.sus_path_count; i++) {
                if (!strcmp(state.sus_path[i].path, argv[2])) {
                    exists = 1;
                    break;
                }
            }
            unlock_state();
            if (exists) {
                printf("[!] Already configured in state: %s\n", argv[2]);
                return 0;
            }
            printf("[-] Error: Target path '%s' does not exist.\n", argv[2]);
            return 1;
        }
        int fd = get_ksu_fd();
        struct st_susfs_sus_path info = {0};
        info.target_ino = sb.st_ino;
        strncpy(info.target_pathname, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
        int ret = ioctl(fd, CMD_SUSFS_ADD_SUS_PATH, &info);
        close(fd);

        if (ret == 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            int exists = 0;
            for (int i = 0; i < state.sus_path_count; i++) {
                if (!strcmp(state.sus_path[i].path, argv[2])) {
                    exists = 1;
                    break;
                }
            }
            if (!exists && state.sus_path_count < MAX_ENTRIES) {
                strncpy(state.sus_path[state.sus_path_count].path, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
                state.sus_path[state.sus_path_count].is_loop = 0;
                strncpy(state.sus_path[state.sus_path_count].source, src_tag, 15);
                state.sus_path_count++;
                save_state_atomic(&state);
                printf("[+] Successfully added SUS Path (ino: %lu, source: %s): %s\n", (unsigned long)sb.st_ino, src_tag, argv[2]);
            } else {
                printf("[!] Already configured in state: %s\n", argv[2]);
            }
            unlock_state();
        } else {
            printf("[-] Failed adding SUS Path (ret: %d)\n", ret);
            return 1;
        }
    } else if (!strcmp(argv[1], "remove_sus_path") && argc >= 3) {
        int fd = get_ksu_fd();
        struct st_susfs_sus_path info = {0};
        struct stat sb;
        if (stat(argv[2], &sb) == 0) {
            info.target_ino = sb.st_ino;
        }
        strncpy(info.target_pathname, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
        int ret = ioctl(fd, CMD_SUSFS_REMOVE_SUS_PATH, &info);
        close(fd);

        if (ret == 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            int new_count = 0;
            sus_path_entry_t new_entries[MAX_ENTRIES];
            for (int i = 0; i < state.sus_path_count; i++) {
                if (strcmp(state.sus_path[i].path, argv[2])) {
                    new_entries[new_count++] = state.sus_path[i];
                }
            }
            memcpy(state.sus_path, new_entries, sizeof(sus_path_entry_t) * new_count);
            state.sus_path_count = new_count;
            save_state_atomic(&state);
            unlock_state();
            printf("[+] Successfully removed SUS Path: %s\n", argv[2]);
        } else {
            printf("[-] Failed removing SUS Path (ret: %d)\n", ret);
            return 1;
        }
    } else if (!strcmp(argv[1], "add_sus_mount") && argc >= 3) {
        if (is_protected_path(argv[2], force_flag)) {
            return 1;
        }
        int fd = get_ksu_fd();
        struct st_susfs_sus_mount info = {0};
        strncpy(info.target_pathname, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
        int ret = ioctl(fd, CMD_SUSFS_ADD_SUS_MOUNT, &info);
        close(fd);

        if (ret == 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            int exists = 0;
            for (int i = 0; i < state.sus_mount_count; i++) {
                if (!strcmp(state.sus_mount[i].path, argv[2])) {
                    exists = 1;
                    break;
                }
            }
            if (!exists && state.sus_mount_count < MAX_ENTRIES) {
                strncpy(state.sus_mount[state.sus_mount_count].path, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
                state.sus_mount_count++;
                save_state_atomic(&state);
                printf("[+] Successfully added SUS Mount: %s\n", argv[2]);
            } else {
                printf("[!] Already configured in state: %s\n", argv[2]);
            }
            unlock_state();
        } else {
            printf("[-] Failed adding SUS Mount (ret: %d)\n", ret);
            return 1;
        }
    } else if (!strcmp(argv[1], "add_try_umount") && argc >= 3) {
        int mode = (argc >= 4) ? atoi(argv[3]) : 0;
        int fd = get_ksu_fd();
        struct st_susfs_try_umount info = {0};
        strncpy(info.target_pathname, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
        info.mnt_mode = mode;
        int ret = ioctl(fd, CMD_SUSFS_ADD_TRY_UMOUNT, &info);
        close(fd);

        if (ret == 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            int exists = 0;
            for (int i = 0; i < state.try_umount_count; i++) {
                if (!strcmp(state.try_umount[i].path, argv[2])) {
                    exists = 1;
                    break;
                }
            }
            if (!exists && state.try_umount_count < MAX_ENTRIES) {
                strncpy(state.try_umount[state.try_umount_count].path, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
                state.try_umount[state.try_umount_count].mode = mode;
                state.try_umount_count++;
                save_state_atomic(&state);
                printf("[+] Successfully added Try Umount: %s (mode: %d)\n", argv[2], mode);
            } else {
                printf("[!] Already configured in state: %s\n", argv[2]);
            }
            unlock_state();
        } else {
            printf("[-] Failed adding Try Umount (ret: %d)\n", ret);
            return 1;
        }
    } else if (!strcmp(argv[1], "sus_su") && argc >= 3) {
        int val = atoi(argv[2]);
        int fd = get_ksu_fd();
        struct st_sus_su info = {0};
        info.enabled = val;
        int ret = ioctl(fd, CMD_SUSFS_SUS_SU, &info);
        close(fd);

        if (ret == 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            state.sus_su = val;
            save_state_atomic(&state);
            unlock_state();
            printf("[+] Set SUS SU Hiding to: %d\n", val);
        } else {
            printf("[-] Failed setting SUS SU (ret: %d)\n", ret);
            return 1;
        }
    } else if (!strcmp(argv[1], "add_sus_kstat") && argc >= 3) {
        struct stat sb;
        if (stat(argv[2], &sb) != 0) {
            printf("[-] Error: Target path '%s' does not exist.\n", argv[2]);
            return 1;
        }
        int fd = get_ksu_fd();
        struct st_susfs_sus_kstat info = {0};
        info.is_statically = 1;
        info.target_ino = sb.st_ino;
        strncpy(info.target_pathname, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
        info.spoofed_ino = sb.st_ino;
        info.spoofed_dev = sb.st_dev;
        info.spoofed_nlink = sb.st_nlink;
        info.spoofed_size = sb.st_size;
        info.spoofed_atime_tv_sec = sb.st_atime;
        info.spoofed_mtime_tv_sec = sb.st_mtime;
        info.spoofed_ctime_tv_sec = sb.st_ctime;
        info.spoofed_blksize = sb.st_blksize;
        info.spoofed_blocks = sb.st_blocks;

        int ret = ioctl(fd, CMD_SUSFS_ADD_SUS_KSTAT, &info);
        close(fd);

        if (ret == 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            int exists = 0;
            for (int i = 0; i < state.sus_kstat_count; i++) {
                if (!strcmp(state.sus_kstat[i].path, argv[2])) {
                    exists = 1;
                    break;
                }
            }
            if (!exists && state.sus_kstat_count < MAX_ENTRIES) {
                strncpy(state.sus_kstat[state.sus_kstat_count].path, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
                state.sus_kstat_count++;
                save_state_atomic(&state);
                printf("[+] Successfully added SUS Kstat: %s\n", argv[2]);
            } else {
                printf("[!] Already configured in state: %s\n", argv[2]);
            }
            unlock_state();
        } else {
            printf("[-] Failed adding SUS Kstat (ret: %d)\n", ret);
            return 1;
        }
    } else if (!strcmp(argv[1], "set_uname") && argc >= 4) {
        int fd = get_ksu_fd();
        struct st_susfs_uname info = {0};
        strncpy(info.release, argv[2], 64);
        strncpy(info.version, argv[3], 64);
        int ret = ioctl(fd, CMD_SUSFS_SET_UNAME, &info);
        close(fd);

        if (ret == 0) {
            lock_state();
            susfs_state_t state;
            load_state(&state);
            strncpy(state.uname_release, argv[2], 64);
            strncpy(state.uname_version, argv[3], 64);
            save_state_atomic(&state);
            unlock_state();
            printf("[+] Set Uname Spoofing returned: %d\n", ret);
        } else {
            printf("[-] Failed setting Uname (ret: %d)\n", ret);
            return 1;
        }
    } else if (!strcmp(argv[1], "enable_log") && argc >= 3) {
        int fd = get_ksu_fd();
        int val = atoi(argv[2]);
        int ret = ioctl(fd, CMD_SUSFS_ENABLE_LOG, (unsigned long)val);
        close(fd);
        printf("[+] Enable Log returned: %d\n", ret);
    } else if (!strcmp(argv[1], "list")) {
        cmd_list(json_mode);
    } else if (!strcmp(argv[1], "restore")) {
        return cmd_restore(json_mode);
    } else if (!strcmp(argv[1], "auto_hide")) {
        const char *subcmd = (argc >= 3) ? argv[2] : "status";
        cmd_auto_hide(subcmd, json_mode);
    } else if (!strcmp(argv[1], "status") || json_mode) {
        if (json_mode) {
            print_status_json();
        } else {
            printf("=== VoidKernel SUSFS Status ===\n");
            int fd = get_ksu_fd();
            char ver[32] = {0};
            if (ioctl(fd, CMD_SUSFS_SHOW_VERSION, ver) == 0) {
                printf("SUSFS Version: %s\n", ver);
            }
            close(fd);
            system("uname -a");
            printf("\n--- Recent dmesg SUSFS Logs ---\n");
            system("dmesg | grep -i susfs | tail -n 10");
        }
    } else {
        print_help();
    }
    return 0;
}
