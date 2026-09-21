# VoidSU Standalone SUSFS CLI v1.4.2 Documentation

The `susfs` CLI tool provides a command-line interface for interacting with the SUSFS kernel engine on VoidKernel.

---

## Usage

```bash
su -c "susfs <command> [args]"
```

---

## Available Commands

| Command | Arguments | Description |
| :--- | :--- | :--- |
| `status` | `[--json]` | Check live SUSFS kernel engine status. |
| `show version` | None | Show SUSFS kernel engine version. |
| `add_sus_path` | `<path>` | Hide target file/directory from non-root processes. |
| `remove_sus_path` | `<path>` | Remove target path from hidden sus_path list. |
| `add_sus_mount` | `<mount_path>` | Hide mountpoint from `/proc/self/mountinfo`. |
| `add_sus_kstat` | `<target_path>` | Spoof kstat attributes for target path. |
| `set_uname` | `<release> <version>` | Spoof kernel release and version strings. |
| `enable_log` | `<0\|1>` | Enable (1) or disable (0) kernel debug logging. |

---

## JSON Output Schema (`susfs status --json`)

```json
{
  "supported": true,
  "version": "1.4.2",
  "kernel": "5.15.178-VoidKernel",
  "features": {
    "sus_path": true,
    "sus_mount": true,
    "sus_kstat": true,
    "set_uname": true,
    "try_umount": false,
    "sus_su": true
  }
}
```
