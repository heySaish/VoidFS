#!/system/bin/sh
# SUSFS 3-Stage Micro Lifecycle Test (No Reboot Required)

CLI="/data/adb/ksu/bin/susfs"
STATE_FILE="/data/adb/susfs/state.json"
TEST_FILE="/data/local/tmp/micro_test_dummy.txt"
MISSING_FILE="/data/local/tmp/micro_test_missing.txt"

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
echo "    SUSFS 3-STAGE MICRO LIFECYCLE TEST            "
echo "=================================================="

# Setup test environment
rm -f "$STATE_FILE"
touch "$TEST_FILE"
rm -f "$MISSING_FILE"

# --- Stage 1: Create -> Add -> List ---
echo "\n--- STAGE 1: Add SUS Path & Verify Active ---"
$CLI add_sus_path "$TEST_FILE" > /dev/null
STAGE1_LIST=$($CLI list --json)
echo "$STAGE1_LIST"

if echo "$STAGE1_LIST" | grep -q "\"path\": \"$TEST_FILE\"" && echo "$STAGE1_LIST" | grep -A5 "\"path\": \"$TEST_FILE\"" | grep -q '"active": true'; then
    log_pass "Stage 1: File successfully hidden and evaluated as active: true!"
else
    log_fail "Stage 1: Active status check failed!"
fi

# --- Stage 2: Remove -> List ---
echo "\n--- STAGE 2: Remove SUS Path & Verify Cleared ---"
$CLI remove_sus_path "$TEST_FILE" > /dev/null
STAGE2_LIST=$($CLI list --json)
if echo "$STAGE2_LIST" | grep -q "$TEST_FILE"; then
    log_fail "Stage 2: Path was not removed!"
else
    log_pass "Stage 2: Path successfully removed from state!"
fi

# --- Stage 3: Restore Simulation (Existing vs Missing) ---
echo "\n--- STAGE 3: Restore Simulation (No Reboot) ---"
# Set up state.json with both 1 existing path & 1 missing path
cat << EOF > "$STATE_FILE"
{
  "schema": 1,
  "sus_path": [
    {
      "path": "$MISSING_FILE",
      "is_loop": false,
      "source": "manual"
    },
    {
      "path": "$TEST_FILE",
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

echo "[+] Triggering Restore..."
RESTORE_OUT=$($CLI restore --json)
echo "Restore Result: $RESTORE_OUT"

echo "[+] Triggering List --json..."
STAGE3_LIST=$($CLI list --json)
echo "$STAGE3_LIST"

# Check Existing file active = true
if echo "$STAGE3_LIST" | grep -q "\"path\": \"$TEST_FILE\"" && echo "$STAGE3_LIST" | grep -A5 "\"path\": \"$TEST_FILE\"" | grep -q '"active": true'; then
    log_pass "Stage 3: Restored existing file evaluated as configured: true, active: true!"
else
    log_fail "Stage 3: Restored existing file active status failed!"
fi

# Check Missing file active = false
if echo "$STAGE3_LIST" | grep -q "\"path\": \"$MISSING_FILE\"" && echo "$STAGE3_LIST" | grep -A5 "\"path\": \"$MISSING_FILE\"" | grep -q '"active": false'; then
    log_pass "Stage 3: Non-existent file evaluated as configured: true, active: false!"
else
    log_fail "Stage 3: Missing file active status failed!"
fi

# Clean up
$CLI remove_sus_path "$TEST_FILE" > /dev/null 2>&1 || true
rm -f "$TEST_FILE" "$STATE_FILE"

echo "\n=================================================="
echo "MICRO-TEST RESULTS: $PASSED PASSED, $FAILED FAILED"
echo "=================================================="
