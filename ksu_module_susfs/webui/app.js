/* ==========================================================================
   VoidSU SUSFS WebUI - Logic & KernelSU Bridge (app.js)
   ========================================================================== */

// State Storage
const state = {
  susPaths: [],
  susMounts: [],
  isLogEnabled: false,
  isHideMountsNonSu: false
};

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
  logTerminal('VoidSU SUSFS Bridge initialized.');
  refreshSystemState();
});

// Tab Navigation Switcher
function switchTab(tabId, navBtn) {
  document.querySelectorAll('.view-page').forEach(page => page.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(btn => btn.classList.remove('active'));
  
  const targetPage = document.getElementById(`view-${tabId}`);
  if (targetPage) {
    targetPage.classList.add('active');
  }
  if (navBtn) {
    navBtn.classList.add('active');
  }
}

// Execute Shell Command via KernelSU WebUI Bridge
async function execCmd(command) {
  logTerminal(`$ ${command}`);
  
  // 1. KernelSU WebUI Bridge Check
  if (window.ksu && typeof window.ksu.exec === 'function') {
    return new Promise((resolve) => {
      window.ksu.exec(command, '{}', (stdout, stderr) => {
        if (stdout && stdout.trim()) logTerminal(`[out] ${stdout.trim()}`);
        if (stderr && stderr.trim()) logTerminal(`[err] ${stderr.trim()}`);
        resolve({ stdout, stderr });
      });
    });
  }
  
  // 2. Mock Fallback for Browser/Simulation
  console.log('[Simulation Exec]', command);
  logTerminal(`[simulated] Command executed successfully.`);
  return { stdout: 'Success (Simulated)', stderr: '' };
}

// Log Output Handler
function logTerminal(message) {
  const terminal = document.getElementById('terminalLogOutput');
  if (terminal) {
    const time = new Date().toLocaleTimeString();
    terminal.textContent += `\n[${time}] ${message}`;
    terminal.scrollTop = terminal.scrollHeight;
  }
}

// Refresh Dashboard & State
async function refreshSystemState() {
  logTerminal('Refreshing system state...');
  
  // Get Kernel Version
  const unameRes = await execCmd('uname -r');
  if (unameRes.stdout) {
    const ver = unameRes.stdout.trim();
    document.getElementById('kernelVerText').textContent = `Linux ${ver}`;
  }
  
  // Get SUSFS Version via IOCTL 0x555e2
  const verPyCmd = `/data/data/com.termux/files/usr/bin/python3 -c "import ctypes, fcntl, os; libc=ctypes.CDLL(None); fd=ctypes.c_int(-1); libc.syscall(142, ctypes.c_uint32(0xDEADBEEF), ctypes.c_uint32(0xCAFEBABE), 0, ctypes.byref(fd)); b=bytearray(32); fcntl.ioctl(fd.value, 0x555e2, b); print(b.decode('utf-8', errors='ignore').strip(chr(0)))"`;
  const verRes = await execCmd(verPyCmd);
  if (verRes.stdout && verRes.stdout.includes('v1.')) {
    document.getElementById('susfsEngineVer').textContent = verRes.stdout.trim();
  }

  loadSusPathsList();
  loadSusMountsList();
}

// Add Custom SUS Path (IOCTL 0x55550)
async function addCustomSusPath() {
  const input = document.getElementById('inputPathToHide');
  const path = input.value.trim();
  if (!path) {
    alert('Please enter a valid path!');
    return;
  }

  logTerminal(`Adding SUS Path: ${path}`);
  
  const pyScript = `/data/data/com.termux/files/usr/bin/python3 -c "import ctypes, fcntl, os; st=os.stat('${path}'); libc=ctypes.CDLL(None); fd=ctypes.c_int(-1); libc.syscall(142, ctypes.c_uint32(0xDEADBEEF), ctypes.c_uint32(0xCAFEBABE), 0, ctypes.byref(fd)); b=bytearray(264); b[0:8]=ctypes.c_ulong(st.st_ino); target='${path}'.encode('utf-8'); b[8:8+len(target)]=target; ret=fcntl.ioctl(fd.value, 0x55550, b); print('IOCTL_RET:', ret)"`;
  
  const res = await execCmd(pyScript);
  if (!state.susPaths.includes(path)) {
    state.susPaths.push(path);
  }
  input.value = '';
  renderSusPaths();
}

// Load & Render SUS Paths
function loadSusPathsList() {
  renderSusPaths();
}

function renderSusPaths() {
  const listEl = document.getElementById('susPathsList');
  document.getElementById('pathCountStat').textContent = state.susPaths.length;

  if (state.susPaths.length === 0) {
    listEl.innerHTML = '<li class="empty-state">No paths added yet. Add one above!</li>';
    return;
  }

  listEl.innerHTML = state.susPaths.map((p, idx) => `
    <li>
      <span>📁 ${p}</span>
      <button class="btn-icon" onclick="removeSusPath(${idx})">🗑️</button>
    </li>
  `).join('');
}

function removeSusPath(idx) {
  state.susPaths.splice(idx, 1);
  renderSusPaths();
}

// Add Custom SUS Mount (IOCTL 0x55560)
async function addCustomSusMount() {
  const input = document.getElementById('inputMountToHide');
  const mountPath = input.value.trim();
  if (!mountPath) {
    alert('Please enter a valid mount path!');
    return;
  }
  addMountPreset(mountPath);
  input.value = '';
}

async function addMountPreset(mountPath) {
  logTerminal(`Adding SUS Mount: ${mountPath}`);
  
  const pyScript = `/data/data/com.termux/files/usr/bin/python3 -c "import ctypes, fcntl, os; libc=ctypes.CDLL(None); fd=ctypes.c_int(-1); libc.syscall(142, ctypes.c_uint32(0xDEADBEEF), ctypes.c_uint32(0xCAFEBABE), 0, ctypes.byref(fd)); b=bytearray(256); target='${mountPath}'.encode('utf-8'); b[0:len(target)]=target; ret=fcntl.ioctl(fd.value, 0x55560, b); print('IOCTL_RET:', ret)"`;
  
  await execCmd(pyScript);
  if (!state.susMounts.includes(mountPath)) {
    state.susMounts.push(mountPath);
  }
  renderSusMounts();
}

function loadSusMountsList() {
  renderSusMounts();
}

function renderSusMounts() {
  const listEl = document.getElementById('susMountsList');
  document.getElementById('mountCountStat').textContent = state.susMounts.length;

  if (state.susMounts.length === 0) {
    listEl.innerHTML = '<li class="empty-state">No mounts added yet. Select a preset above!</li>';
    return;
  }

  listEl.innerHTML = state.susMounts.map((m, idx) => `
    <li>
      <span>🏔️ ${m}</span>
      <button class="btn-icon" onclick="removeSusMount(${idx})">🗑️</button>
    </li>
  `).join('');
}

function removeSusMount(idx) {
  state.susMounts.splice(idx, 1);
  renderSusMounts();
}

// Toggle Settings
async function toggleKernelLogSetting(enabled) {
  logTerminal(`Setting Kernel Debug Log: ${enabled}`);
  const pyScript = `/data/data/com.termux/files/usr/bin/python3 -c "import ctypes, fcntl, os; libc=ctypes.CDLL(None); fd=ctypes.c_int(-1); libc.syscall(142, ctypes.c_uint32(0xDEADBEEF), ctypes.c_uint32(0xCAFEBABE), 0, ctypes.byref(fd)); fcntl.ioctl(fd.value, 0x555a0, ctypes.c_ulong(${enabled ? 1 : 0}))"`;
  await execCmd(pyScript);
}

async function toggleHideMountsNonSuSetting(enabled) {
  logTerminal(`Setting Hide Mounts Non-SU: ${enabled}`);
}

// Apply Standard Presets
async function applyStandardHidingPreset() {
  logTerminal('Applying standard security hiding presets...');
  await addMountPreset('/data/adb/modules');
  await addMountPreset('/system/etc/hosts');
  await addMountPreset('/debug_ramdisk');
  alert('Standard Security Presets Applied!');
}

// Apply Uname Spoofing (0x55590)
async function applyUnameSpoofing() {
  const rel = document.getElementById('inputUnameRelease').value.trim();
  const ver = document.getElementById('inputUnameVersion').value.trim();
  
  if (!rel || !ver) {
    alert('Please fill out both release and version strings!');
    return;
  }
  
  logTerminal(`Applying Uname Spoofing: Release=${rel}, Version=${ver}`);
  const pyScript = `/data/data/com.termux/files/usr/bin/python3 -c "import ctypes, fcntl, os; libc=ctypes.CDLL(None); fd=ctypes.c_int(-1); libc.syscall(142, ctypes.c_uint32(0xDEADBEEF), ctypes.c_uint32(0xCAFEBABE), 0, ctypes.byref(fd)); b=bytearray(130); b[0:${len(rel)}]=b'${rel}'; b[65:65+${len(ver)}]=b'${ver}'; fcntl.ioctl(fd.value, 0x55590, b)"`;
  await execCmd(pyScript);
}
