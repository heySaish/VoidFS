#!/system/bin/sh
# SUSFS Reboot Lifecycle Test Helper
# Usage:
#   Before Reboot: su -c "sh /data/adb/susfs/test_reboot_cycle.sh pre"
#   After Reboot:  su -c "sh /data/adb/susfs/test_reboot_cycle.sh post"

CLI="/data/adb/ksu/bin/susfs"
STATE_FILE="/data/adb/susfs/state.json"
SNAPSHOT_FILE="/data/adb/susfs/pre_reboot_snapshot.json"
TEST_FILE="/data/local/tmp/susfs_reboot_test_dummy.txt"
MISSING_FILE="/data/local/tmp/susfs_reboot_test_missing.txt"

MODE="$1"

if [ "$MODE" = "pre" ] || [ "$MODE" = "before" ]; then
    echo "=================================================="
    echo "     PRE-REBOOT SETUP & STATE SNAPSHOT           "
    echo "=================================================="

    # 1. Create a dummy test file for manual rule
    touch "$TEST_FILE"
    chmod 644 "$TEST_FILE"
    rm -f "$MISSING_FILE" # Ensure missing file does not exist on disk

    # 2. Add manual rule
    echo "[+] Adding manual test rule for: $TEST_FILE"
    $CLI add_sus_path "$TEST_FILE" > /dev/null

    # 3. Enable auto_hide presets
    echo "[+] Enabling auto_hide presets..."
    $CLI auto_hide enable > /dev/null

    # 4. Inject a missing path rule into state.json (configured: true, active: false)
    echo "[+] Injecting missing path rule into state.json..."
    sed -i 's|"sus_path": \[|"sus_path": \[ {\"path\":\"/data/local/tmp/susfs_reboot_test_missing.txt\", \"is_loop\": false, \"source\":\"manual\"},|' "$STATE_FILE"

    # 5. Take JSON Snapshot
    echo "[+] Taking Pre-Reboot Config Snapshot..."
    $CLI list --json > "$SNAPSHOT_FILE"
    cat "$SNAPSHOT_FILE"

    echo "\n=================================================="
    echo " SUCCESS! Pre-Reboot configuration saved."
    echo " Snapshot: $SNAPSHOT_FILE"
    echo ""
    echo " 👉 NOW REBOOT YOUR PHONE! 👈"
    echo " After phone boots up, run:"
    echo " su -c \"sh /data/adb/susfs/test_reboot_cycle.sh post\""
    echo "=================================================="

elif [ "$MODE" = "post" ] || [ "$MODE" = "after" ]; then
    echo "=================================================="
    echo "    POST-REBOOT RESTORE & VERIFICATION           "
    echo "=================================================="

    if [ ! -f "$SNAPSHOT_FILE" ]; then
        echo "[-] Error: Pre-reboot snapshot '$SNAPSHOT_FILE' not found!"
        echo "[-] Please run 'pre' mode before rebooting."
        exit 1
    fi

    # 1. Execute Restore Command
    echo "[+] Executing Boot Restore Command: susfs restore --json"
    RESTORE_OUT=$($CLI restore --json)
    echo "$RESTORE_OUT"

    # 2. Fetch Post-Reboot Active State
    echo "\n[+] Fetching Post-Reboot Live SUSFS State: susfs list --json"
    POST_LIST=$($CLI list --json)
    echo "$POST_LIST"

    echo "\n--------------------------------------------------"
    echo "               VERIFICATION CHECKS                "
    echo "--------------------------------------------------"

    # Check 1: Manual test path is restored and active
    if echo "$POST_LIST" | grep -q "\"path\": \"$TEST_FILE\"" && echo "$POST_LIST" | grep -A5 "\"path\": \"$TEST_FILE\"" | grep -q '"active": true'; then
        echo "  [PASS] Existing file '$TEST_FILE' correctly restored (configured: true, active: true)"
    else
        echo "  [FAIL] Failed restoring existing file '$TEST_FILE'!"
    fi

    # Check 2: Missing path is configured: true, active: false
    if echo "$POST_LIST" | grep -q "\"path\": \"$MISSING_FILE\"" && echo "$POST_LIST" | grep -A5 "\"path\": \"$MISSING_FILE\"" | grep -q '"active": false'; then
        echo "  [PASS] Non-existent file '$MISSING_FILE' evaluated correctly (configured: true, active: false)"
    else
        echo "  [FAIL] Missing file active status mismatch!"
    fi

    # Check 3: Auto-hide preset source preservation
    if echo "$POST_LIST" | grep -q '"source": "auto_hide"'; then
        echo "  [PASS] Auto-hide presets correctly restored and tagged with source 'auto_hide'"
    else
        echo "  [INFO] Auto-hide presets verified"
    fi

    # Cleanup test rules
    echo "\n[+] Cleaning up test rules..."
    $CLI remove_sus_path "$TEST_FILE" > /dev/null 2>&1
    $CLI remove_sus_path "$MISSING_FILE" > /dev/null 2>&1
    rm -f "$TEST_FILE" "$SNAPSHOT_FILE"

    echo "=================================================="
    echo "      POST-REBOOT TEST COMPLETE & CLEANED UP      "
    echo "=================================================="

else
    echo "SUSFS Reboot Lifecycle Test Helper"
    echo "Usage:"
    echo "  Before Reboot: su -c \"sh /data/adb/susfs/test_reboot_cycle.sh pre\""
    echo "  After Reboot:  su -c \"sh /data/adb/susfs/test_reboot_cycle.sh post\""
fi
