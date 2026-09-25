#!/system/bin/sh
# SUSFS CLI Test Suite v1.6 (Root Execution Script)

CLI="/data/adb/ksu/bin/susfs"
STATE_FILE="/data/adb/susfs/state.json"
RAND_ID=$(date +%s)
PASSED=0
FAILED=0

log_pass() {
    echo "  [PASS] $1"
    PASSED=$((PASSED + 1))
}

log_fail() {
    echo "  [FAIL] $1"
    FAILED=$((FAILED + 1))
}

echo "=================================================="
echo "      SUSFS CLI COMPREHENSIVE TEST SUITE          "
echo "=================================================="

# Backup current state.json if exists
if [ -f "$STATE_FILE" ]; then
    cp "$STATE_FILE" "${STATE_FILE}.bak"
fi
rm -f "$STATE_FILE"

# 1. Basic Add -> List -> JSON
echo ""
echo "--- Test 1: Add SUS Path & List JSON ---"
T1="/data/local/tmp/t1_${RAND_ID}.txt"
touch "$T1"
$CLI add_sus_path "$T1" > /dev/null
LIST_OUT=$($CLI list --json)
if echo "$LIST_OUT" | grep -q "$T1" && echo "$LIST_OUT" | grep -q '"source": "manual"'; then
    log_pass "Path added and correctly tagged with source 'manual'"
else
    log_fail "Failed adding path or incorrect JSON output: $LIST_OUT"
fi

# 2. Duplicate Add
echo ""
echo "--- Test 2: Duplicate Rule Handling ---"
DUP_OUT=$($CLI add_sus_path "$T1")
if echo "$DUP_OUT" | grep -q "Already configured"; then
    log_pass "Duplicate entry cleanly rejected without duplicate state entries"
else
    log_fail "Duplicate entry error: $DUP_OUT"
fi

# 3. Remove Rule
echo ""
echo "--- Test 3: Remove SUS Path ---"
$CLI remove_sus_path "$T1" > /dev/null
LIST_OUT=$($CLI list --json)
if echo "$LIST_OUT" | grep -q "$T1"; then
    log_fail "Path was not removed from state file"
else
    log_pass "Path successfully removed from state"
fi
rm -f "$T1"

# 4. Probing missing path (configured: true, active: false)
echo ""
echo "--- Test 4: Missing Target Path Active Probing ---"
T4_MISSING="/data/local/tmp/t4_missing_${RAND_ID}.txt"
rm -f "$T4_MISSING"
# Inject missing path rule directly into state file
cat << EOF > "$STATE_FILE"
{
  "schema": 1,
  "sus_path": [
    {
      "path": "$T4_MISSING",
      "is_loop": false,
      "source": "manual"
    }
  ],
  "sus_mount": [],
  "try_umount": [],
  "sus_kstat": [],
  "set_uname": { "release": "default", "version": "default" },
  "sus_su": 0,
  "logging": 0
}
EOF
LIST_OUT=$($CLI list --json)
if echo "$LIST_OUT" | grep -q '"path": "'"$T4_MISSING"'"' && echo "$LIST_OUT" | grep -A5 '"path": "'"$T4_MISSING"'"' | grep -q '"active": false'; then
    log_pass "Non-existent path correctly evaluated as configured: true, active: false"
else
    log_fail "Active probing failed for missing path: $LIST_OUT"
fi
$CLI remove_sus_path "$T4_MISSING" > /dev/null 2>&1 || true

# 5. Core Infra Hard Block Policy Protection
echo ""
echo "--- Test 5: Core Infrastructure Hard Block Guard ---"
BLOCK_OUT=$($CLI add_sus_path /data/adb/susfs --force)
if echo "$BLOCK_OUT" | grep -q "strictly protected (hard block)"; then
    log_pass "Core infrastructure /data/adb/susfs hard blocked even with --force flag"
else
    log_fail "Core infrastructure protection failed! Output: $BLOCK_OUT"
fi

# 6. Policy Warning and --force Flag
echo ""
echo "--- Test 6: Policy Warning & --force Flag for Protected Paths ---"
WARN_OUT=$($CLI add_sus_path /system/bin/su)
if echo "$WARN_OUT" | grep -q "Policy Warning"; then
    log_pass "Policy warning displayed when hiding protected path without --force"
else
    log_fail "Policy warning failed: $WARN_OUT"
fi

FORCE_OUT=$($CLI add_sus_path /system/bin/su --force)
if echo "$FORCE_OUT" | grep -q "Successfully added" || echo "$FORCE_OUT" | grep -q "Already configured" || echo "$FORCE_OUT" | grep -q "does not exist"; then
    log_pass "Policy guard handles --force flag correctly"
else
    log_fail "Force flag execution failed: $FORCE_OUT"
fi
$CLI remove_sus_path /system/bin/su > /dev/null 2>&1 || true

# 7. Auto Hide Presets + Manual Path Protection
echo ""
echo "--- Test 7: Auto-Hide Enable/Disable + Manual Path Preservation ---"
T7_MANUAL="/data/local/tmp/t7_manual_${RAND_ID}.txt"
touch "$T7_MANUAL"
$CLI add_sus_path "$T7_MANUAL" > /dev/null

# Enable auto hide
$CLI auto_hide enable > /dev/null
STATUS_OUT=$($CLI auto_hide status --json)
if echo "$STATUS_OUT" | grep -q '"auto_hide"'; then
    log_pass "Auto-hide status check passed"
else
    log_fail "Auto-hide status check failed: $STATUS_OUT"
fi

# Disable auto hide
$CLI auto_hide disable > /dev/null
LIST_AFTER=$($CLI list --json)
if echo "$LIST_AFTER" | grep -q "$T7_MANUAL"; then
    log_pass "Manual rule '$T7_MANUAL' PRESERVED after auto_hide disable!"
else
    log_fail "Manual rule was accidentally wiped by auto_hide disable! Output: $LIST_AFTER"
fi
$CLI remove_sus_path "$T7_MANUAL" > /dev/null 2>&1 || true
rm -f "$T7_MANUAL"

# 8. Concurrency Test (Flock file locking)
echo ""
echo "--- Test 8: Concurrency File Locking Test ---"
rm -f "$STATE_FILE"
T8_1="/data/local/tmp/t8_conc1_${RAND_ID}.txt"
T8_2="/data/local/tmp/t8_conc2_${RAND_ID}.txt"
touch "$T8_1" "$T8_2"
$CLI add_sus_path "$T8_1" >/dev/null 2>&1 &
PID1=$!
$CLI add_sus_path "$T8_2" >/dev/null 2>&1 &
PID2=$!
wait $PID1 $PID2
LIST_PARALLEL=$($CLI list --json)
if echo "$LIST_PARALLEL" | grep -q "$T8_1" && echo "$LIST_PARALLEL" | grep -q "$T8_2"; then
    log_pass "Parallel CLI invocations safely serialized state file writes using flock"
else
    log_fail "State file corrupted or incomplete after concurrent writes: $LIST_PARALLEL"
fi
$CLI remove_sus_path "$T8_1" > /dev/null 2>&1 || true
$CLI remove_sus_path "$T8_2" > /dev/null 2>&1 || true
rm -f "$T8_1" "$T8_2"

# Restore state.json backup if existed
if [ -f "${STATE_FILE}.bak" ]; then
    mv "${STATE_FILE}.bak" "$STATE_FILE"
fi

echo ""
echo "=================================================="
echo "TEST RESULTS: $PASSED PASSED, $FAILED FAILED"
echo "=================================================="
