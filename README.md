# VoidFS (Void-SUSFS v1.4.2 for VoidKernel)

Custom SUSFS kernel patchset, standalone native CLI, and dedicated Android Jetpack Compose Manager tailored for **VoidKernel** & **VoidSU**.

---

## Repository Structure

```
VoidFS/
├── .github/                # GitHub Actions CI/CD (Automatic APK Builder)
├── docs/                   # Documentation & Architectural Specifications
│   ├── ARCHITECTURE.md     # VoidFS Compose App Architecture & Spec
│   └── SUSFS_CLI.md        # CLI Commands & JSON Schema Guide
├── scripts/                # Build & Tool Helper Scripts
│   ├── build_ksu_module.sh
│   ├── build_ksu_susfs_tool.sh
│   └── build_sus_su_tool.sh
├── Manager/                # Dedicated Android Jetpack Compose Control Center App
├── kernel_patches/         # VoidKernel Patches (SUSFS & VFS Hooks)
├── ksu_module_susfs/       # KernelSU Module Source
├── ksu_susfs/              # SUSFS Kernel Engine Headers
└── sus_su/                 # SUS_SU Engine Source
```

---

## Overview
VoidFS provides kernel-level hiding mechanisms (hiding custom mounts, su binaries, and overlayfs paths) to prevent anti-root and security detections.

## Features & Components
- **Dedicated Android Manager (`Manager/`):** Pure Jetpack Compose app with zero bloat and single-source-of-truth status monitoring.
- **SUSFS Engine v1.4.2:** Core kernel hiding driver (`fs/susfs.c`, `include/linux/susfs.h`).
- **SUS_SU Engine:** Kernel-level SU binary cloaking (`fs/sus_su.c`, `include/linux/sus_su.h`).
- **VFS Patch (4.14 / 5.15):** Tailored VFS hooks (`kernel_patches/`).
- **KSU Integration:** `10_enable_susfs_for_ksu.patch`.

---

## Patch Instructions for Void-Kernel
1. Copy core files to kernel tree:
   ```bash
   cp ./kernel_patches/fs/susfs.c $KERNEL_ROOT/fs/
   cp ./kernel_patches/fs/sus_su.c $KERNEL_ROOT/fs/
   cp ./kernel_patches/include/linux/susfs.h $KERNEL_ROOT/include/linux/
   cp ./kernel_patches/include/linux/sus_su.h $KERNEL_ROOT/include/linux/
   ```
2. Apply VFS patch:
   ```bash
   cd $KERNEL_ROOT
   patch -p1 < kernel_patches/50_add_susfs_in_kernel-4.14.patch
   ```
3. Enable configs in kernel config:
   ```config
   CONFIG_KSU=y
   CONFIG_KSU_SUSFS=y
   CONFIG_KSU_SUSFS_SUS_SU=y
   ```
4. Build and flash the kernel!

---

## Links
- **VoidFS Repo:** [github.com/heySaish/VoidFS](https://github.com/heySaish/VoidFS)
- **VoidSU Manager:** [github.com/heySaish/VoidSU](https://github.com/heySaish/VoidSU)
- **Void-Kernel:** [github.com/heySaish/Void-Kernel](https://github.com/heySaish/Void-Kernel)
- **Telegram Channel:** [t.me/VoidKernelOfficial](https://t.me/VoidKernelOfficial)

## Credits
- **simonpunk** for original SUSFS Addon
- **tiann** for KernelSU
