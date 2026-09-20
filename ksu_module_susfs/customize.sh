DEST_BIN_DIR=/data/adb/ksu/bin

if [ ! -d ${DEST_BIN_DIR} ]; then
    ui_print "'${DEST_BIN_DIR}' not existed, installation aborted."
    rm -rf ${MODPATH}
    exit 1
fi

unzip ${ZIPFILE} -d ${TMPDIR}/susfs

if [ ${ARCH} = "arm64" ]; then
        if [ -f ${TMPDIR}/susfs/tools/susfs_standalone ]; then
                cp ${TMPDIR}/susfs/tools/susfs_standalone ${DEST_BIN_DIR}/susfs
                cp ${TMPDIR}/susfs/tools/susfs_standalone ${DEST_BIN_DIR}/ksu_susfs
                chmod 755 ${DEST_BIN_DIR}/susfs ${DEST_BIN_DIR}/ksu_susfs
        else
                cp ${TMPDIR}/susfs/tools/ksu_susfs_arm64 ${DEST_BIN_DIR}/ksu_susfs
                cp ${TMPDIR}/susfs/tools/sus_su_arm64 ${DEST_BIN_DIR}/sus_su
        fi
elif [ ${ARCH} = "arm" ]; then
        cp ${TMPDIR}/susfs/tools/ksu_susfs_arm ${DEST_BIN_DIR}/ksu_susfs
        cp ${TMPDIR}/susfs/tools/sus_su_arm ${DEST_BIN_DIR}/sus_su
fi

chmod 755 ${DEST_BIN_DIR}/ksu_susfs ${DEST_BIN_DIR}/susfs 2>/dev/null || true
chmod 644 ${MODPATH}/post-fs-data.sh ${MODPATH}/service.sh ${MODPATH}/uninstall.sh

rm -rf ${MODPATH}/tools
rm ${MODPATH}/customize.sh ${MODPATH}/README.md


