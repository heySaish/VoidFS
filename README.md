# VoidFS (Void-SUSFS v1.4.2 for Linux 4.14) #

Custom SUSFS kernel patchset and helper tools tailored for **Void-Kernel** (Linux 4.14) & **VoidSU Manager**.

## Overview
VoidFS provides kernel-level hiding mechanisms (hiding custom mounts, su binaries, and overlayfs paths) to prevent anti-root and security detections.

## Features & Components
- **SUSFS Engine v1.4.2:** Core kernel hiding driver (`fs/susfs.c`, `include/linux/susfs.h`).
- **SUS_SU Engine:** Kernel-level SU binary cloaking (`fs/sus_su.c`, `include/linux/sus_su.h`).
- **VFS Patch (4.14):** Tailored VFS hooks (`kernel_patches/50_add_susfs_in_kernel-4.14.patch`).
- **KSU Integration:** `10_enable_susfs_for_ksu.patch`.

## Patch Instructions for Void-Kernel (4.14)
1. Copy core files to kernel tree:
   ```bash
   cp ./kernel_patches/fs/susfs.c $KERNEL_ROOT/fs/
   cp ./kernel_patches/fs/sus_su.c $KERNEL_ROOT/fs/
   cp ./kernel_patches/include/linux/susfs.h $KERNEL_ROOT/include/linux/
   cp ./kernel_patches/include/linux/sus_su.h $KERNEL_ROOT/include/linux/
   ```
2. Apply 4.14 VFS patch:
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

## Links
- **VoidFS Repo:** [github.com/heySaish/VoidFS](https://github.com/heySaish/VoidFS)
- **VoidSU Manager:** [github.com/heySaish/VoidSU](https://github.com/heySaish/VoidSU)
- **Void-Kernel:** [github.com/heySaish/Void-Kernel](https://github.com/heySaish/Void-Kernel)
- **Telegram Channel:** [t.me/VoidKernelOfficial](https://t.me/VoidKernelOfficial)

## Credits
- **simonpunk** for original SUSFS Addon & 1.4.2-kernel-4.14 release
- **tiann** for KernelSU
