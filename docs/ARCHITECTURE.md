# VoidFS Manager - System Architecture & Design Spec

## Overview
**VoidFS** is a dedicated, lightweight, native Android frontend (built with Kotlin & Jetpack Compose) for managing the **SUSFS Kernel Engine** on VoidKernel ecosystems.

Unlike generic root managers, VoidFS is strictly focused on filesystem kernel control without bloat.

---

## Architectural Principles

```
┌─────────────────────────────────────────┐
│              VoidFS Compose UI          │
│   (HomeScreen, PathsScreen, Kstat, etc) │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│               MainViewModel             │
│       (Reactive StateFlow State)        │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│            SusfsRepository              │
│  (Status Parser & CLI Command Mapper)   │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│           RootShellExecutor             │
│        (su -c process runner)           │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│          Standalone susfs CLI           │
│        (/data/adb/ksu/bin/susfs)        │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│           SUSFS Kernel Engine           │
│        (5.15.178-VoidKernel)            │
└─────────────────────────────────────────┘
```

1. **Zero Internals Knowledge in UI**: The Compose UI layer has no direct dependency on low-level kernel ioctl calls.
2. **Single Source of Truth**: All status checking relies on `susfs status --json`.
3. **MVI / MVVM Pattern**: UI reacts strictly to `UiState` via Kotlin `StateFlow`.
4. **Binary Resolution**: Automatic discovery order:
   - `/data/adb/ksu/bin/susfs`
   - `/system/bin/susfs`
   - `which susfs`

---

## Screen & Feature Hierarchy

- **🏠 Home (Dashboard)**: Big active status card (Engine Version, Kernel, Integration) + Live engine feature status checklist.
- **📁 Paths (SUS Path & SUS Mount)**: Dedicated lists with trash removal buttons & "+ Add Path / Mount" dialogs.
- **📊 Kstat (SUS Kstat)**: Target path spoofing list & dialog.
- **⚙️ Settings (Settings & Debug)**:
  - Uname Spoof inputs & Apply handler.
  - Debug card with Kernel Logging toggle (`enable_log 0|1`), live binary path info, and refresh triggers.
