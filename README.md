# VoidFS (Void-SUSFS Addon) #

Custom SUSFS kernel patchset and helper tools tailored for **Void-Kernel** (Linux 4.14 / 5.15) & **VoidSU Manager**.

## Overview
VoidFS provides kernel-level hiding mechanisms (hiding custom mounts, su binaries, and overlayfs paths) to prevent anti-root and security detections.

## Features
- Optimized 4.14 Kernel VFS patches (`50_add_susfs_in_kernel-4.14.patch`)
- Native support for **Void-Kernel**
- Integrated status reporting in **VoidSU Manager**

## Patch Instructions for Void-Kernel
1. Copy core files to kernel tree:
   ```bash
   cp ./kernel_patches/fs/susfs.c $KERNEL_ROOT/fs/
   cp ./kernel_patches/include/linux/susfs.h $KERNEL_ROOT/include/linux/
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
   ```
4. Build and flash the kernel!

## Links
- **VoidSU Manager:** [github.com/heySaish/VoidSU](https://github.com/heySaish/VoidSU)
- **Void-Kernel:** [github.com/heySaish/Void-Kernel](https://github.com/heySaish/Void-Kernel)
- **Telegram Channel:** [t.me/VoidKernelOfficial](https://t.me/VoidKernelOfficial)

## Credits
- **simonpunk** for original SUSFS Addon
- **tiann** for KernelSU
