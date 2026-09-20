#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/syscall.h>

#define KSU_MAGIC1 0xDEADBEEF
#define KSU_MAGIC2 0xCAFEBABE
#define SYS_REBOOT __NR_reboot

#define CMD_SUSFS_ADD_SUS_PATH 0x55550
#define CMD_SUSFS_ADD_SUS_MOUNT 0x55560
#define CMD_SUSFS_ADD_SUS_KSTAT 0x55570
#define CMD_SUSFS_UPDATE_SUS_KSTAT 0x55571
#define CMD_SUSFS_ADD_TRY_UMOUNT 0x55580
#define CMD_SUSFS_SET_UNAME 0x55590
#define CMD_SUSFS_ENABLE_LOG 0x555a0
#define CMD_SUSFS_SHOW_VERSION 0x555e2
#define CMD_SUSFS_SUS_SU 0x60000

#define SUSFS_MAX_LEN_PATHNAME 256

struct st_susfs_sus_path {
    unsigned long target_ino;
    char target_pathname[SUSFS_MAX_LEN_PATHNAME];
};

struct st_susfs_sus_mount {
    char target_pathname[SUSFS_MAX_LEN_PATHNAME];
};

struct st_susfs_uname {
    char release[65];
    char version[65];
};

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

static void print_help(void) {
    printf("VoidSU Standalone SUSFS CLI Tool v1.4.2 (Native ARM64)\n");
    printf("Usage: susfs <command> [args]\n\n");
    printf("Commands:\n");
    printf("  show version                 Show SUSFS kernel engine version\n");
    printf("  add_sus_path <path>          Hide file/directory from non-root app processes\n");
    printf("  add_sus_mount <mount_path>   Hide mountpoint from /proc/self/mountinfo\n");
    printf("  set_uname <release> <ver>    Spoof kernel release and version strings\n");
    printf("  enable_log <0|1>             Enable (1) or disable (0) kernel debug logging\n");
    printf("  status                       Check live SUSFS kernel engine status\n");
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        print_help();
        return 0;
    }

    if (!strcmp(argv[1], "show") && argc >= 3 && !strcmp(argv[2], "version")) {
        int fd = get_ksu_fd();
        char ver[32] = {0};
        if (ioctl(fd, CMD_SUSFS_SHOW_VERSION, ver) == 0) {
            printf("SUSFS Version: %s\n", ver);
        } else {
            printf("[-] Error fetching SUSFS version\n");
        }
        close(fd);
    } else if (!strcmp(argv[1], "add_sus_path") && argc >= 3) {
        struct stat sb;
        if (stat(argv[2], &sb) != 0) {
            printf("[-] Error: Target path '%s' does not exist.\n", argv[2]);
            return 1;
        }
        int fd = get_ksu_fd();
        struct st_susfs_sus_path info = {0};
        info.target_ino = sb.st_ino;
        strncpy(info.target_pathname, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
        int ret = ioctl(fd, CMD_SUSFS_ADD_SUS_PATH, &info);
        if (ret == 0) {
            printf("[+] Successfully added SUS Path (ino: %lu): %s\n", (unsigned long)sb.st_ino, argv[2]);
        } else {
            printf("[-] Failed adding SUS Path (ret: %d)\n", ret);
        }
        close(fd);
    } else if (!strcmp(argv[1], "add_sus_mount") && argc >= 3) {
        int fd = get_ksu_fd();
        struct st_susfs_sus_mount info = {0};
        strncpy(info.target_pathname, argv[2], SUSFS_MAX_LEN_PATHNAME - 1);
        int ret = ioctl(fd, CMD_SUSFS_ADD_SUS_MOUNT, &info);
        if (ret == 0) {
            printf("[+] Successfully added SUS Mount: %s\n", argv[2]);
        } else {
            printf("[-] Failed adding SUS Mount (ret: %d)\n", ret);
        }
        close(fd);
    } else if (!strcmp(argv[1], "set_uname") && argc >= 4) {
        int fd = get_ksu_fd();
        struct st_susfs_uname info = {0};
        strncpy(info.release, argv[2], 64);
        strncpy(info.version, argv[3], 64);
        int ret = ioctl(fd, CMD_SUSFS_SET_UNAME, &info);
        printf("[+] Set Uname Spoofing returned: %d\n", ret);
        close(fd);
    } else if (!strcmp(argv[1], "enable_log") && argc >= 3) {
        int fd = get_ksu_fd();
        int val = atoi(argv[2]);
        int ret = ioctl(fd, CMD_SUSFS_ENABLE_LOG, (unsigned long)val);
        printf("[+] Enable Log returned: %d\n", ret);
        close(fd);
    } else if (!strcmp(argv[1], "status")) {
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
    } else {
        print_help();
    }
    return 0;
}
