# VoidFS (Void-SUSFS v1.4.2 for VoidKernel)

A high-performance kernel hiding engine, standalone native ARM64 CLI tool, and KernelSU module tailored specifically for **VoidKernel** and **VoidSU**.

---

## 🚀 Key Highlights

- **Kernel-Level Enforcement**: Native VFS hooks for stealthy path hiding (`sus_path`), mount spoofing (`sus_mount`), kstat spoofing (`sus_kstat`), and root binary cloaking (`sus_su`).
- **Standalone Native ARM64 CLI**: Direct Bionic-linked C binary (`/data/adb/ksu/bin/susfs`) with zero external dependencies on Termux, Python, or legacy `ksud` layers.
- **Machine-Readable API Contract**: Structured JSON capability matrix via `susfs status --json` for seamless, non-blocking Android Manager orchestration.
- **Out-of-the-Box Integration**: Pre-integrated into VoidKernel source builds with zero manual patching required.

---

## 📁 Repository Structure

```text
VoidFS/
├── .github/                # CI/CD Workflows for automated builds
├── docs/                   # Documentation & Architectural Specifications
│   ├── ARCHITECTURE.md     # System Architecture & Capability Contract
│   └── SUSFS_CLI.md        # Native CLI Command Reference & JSON Schema
├── ksu_module_susfs/       # KernelSU Flashable Module Source
├── ksu_susfs/              # SUSFS Engine Source & Native C CLI
└── tools/                  # Compiled Standalone ARM64 Native Binaries
```

---

## ⚡ Native CLI Quick Start (`susfs`)

The standalone native binary is deployed at `/data/adb/ksu/bin/susfs` (and `/system/bin/susfs`).

```bash
# Query kernel capability matrix in JSON format
su -c "susfs status --json"

# Hide a file or directory from non-root app processes
su -c "susfs add_sus_path /path/to/hide"

# Remove a hidden path from sus_path registry
su -c "susfs remove_sus_path /path/to/hide"

# Hide a mountpoint from /proc/self/mountinfo
su -c "susfs add_sus_mount /mount/point"

# Spoof file stat attributes
su -c "susfs add_sus_kstat /path/to/spoof"

# Spoof kernel release and version strings
su -c 'susfs set_uname "5.15.178-VoidKernel" "#1 PREEMPT"'
```

---

## 📊 Machine JSON Contract (`susfs status --json`)

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

---

## 🔗 Related Resources

- **VoidFS Repo**: [github.com/heySaish/VoidFS](https://github.com/heySaish/VoidFS)
- **VoidSU Manager**: [github.com/heySaish/VoidSU](https://github.com/heySaish/VoidSU)
- **Void-Kernel**: [github.com/heySaish/Void-Kernel](https://github.com/heySaish/Void-Kernel)
- **Telegram Channel**: [t.me/VoidKernelOfficial](https://t.me/VoidKernelOfficial)

---

## 🤝 Credits & Acknowledgments

- **simonpunk** for original SUSFS Addon & Engine design
- **tiann** & KernelSU Team for KernelSU framework
