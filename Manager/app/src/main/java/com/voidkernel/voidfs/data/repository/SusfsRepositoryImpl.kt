package com.voidkernel.voidfs.data.repository

import com.voidkernel.voidfs.data.model.SusfsStatus
import com.voidkernel.voidfs.data.shell.RootShellExecutor
import com.voidkernel.voidfs.data.shell.ShellResult
import kotlinx.serialization.json.Json

class SusfsRepositoryImpl : SusfsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedBinaryPath: String? = null

    override suspend fun getBinaryPath(): String {
        cachedBinaryPath?.let { return it }

        val check = RootShellExecutor.runCommand("if [ -x /data/adb/ksu/bin/susfs ]; then echo /data/adb/ksu/bin/susfs; elif [ -x /system/bin/susfs ]; then echo /system/bin/susfs; elif command -v susfs >/dev/null 2>&1; then command -v susfs; else echo susfs; fi")
        if (check.isSuccess && check.stdout.isNotBlank()) {
            val path = check.stdout.lines().firstOrNull { it.isNotBlank() } ?: "/data/adb/ksu/bin/susfs"
            cachedBinaryPath = path
            return path
        }

        val defaultPath = "/data/adb/ksu/bin/susfs"
        cachedBinaryPath = defaultPath
        return defaultPath
    }

    override suspend fun fetchStatus(): SusfsStatus {
        val bin = getBinaryPath()
        val result = RootShellExecutor.runCommand("$bin status --json")

        if (result.stdout.isNotBlank()) {
            return try {
                val jsonStart = result.stdout.indexOf('{')
                val jsonEnd = result.stdout.lastIndexOf('}')
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    val jsonSub = result.stdout.substring(jsonStart, jsonEnd + 1)
                    json.decodeFromString<SusfsStatus>(jsonSub)
                } else {
                    json.decodeFromString<SusfsStatus>(result.stdout)
                }
            } catch (e: Exception) {
                SusfsStatus(supported = false)
            }
        }
        return SusfsStatus(supported = false)
    }

    override suspend fun addSusPath(path: String): ShellResult {
        val bin = getBinaryPath()
        return RootShellExecutor.runCommand("$bin add_sus_path \"$path\"")
    }

    override suspend fun removeSusPath(path: String): ShellResult {
        val bin = getBinaryPath()
        return RootShellExecutor.runCommand("$bin remove_sus_path \"$path\"")
    }

    override suspend fun addSusMount(mountPath: String): ShellResult {
        val bin = getBinaryPath()
        return RootShellExecutor.runCommand("$bin add_sus_mount \"$mountPath\"")
    }

    override suspend fun removeSusMount(mountPath: String): ShellResult {
        val bin = getBinaryPath()
        return RootShellExecutor.runCommand("$bin add_sus_mount \"$mountPath\"")
    }

    override suspend fun addSusKstat(targetPath: String): ShellResult {
        val bin = getBinaryPath()
        return RootShellExecutor.runCommand("$bin add_sus_kstat \"$targetPath\"")
    }

    override suspend fun removeSusKstat(targetPath: String): ShellResult {
        val bin = getBinaryPath()
        return RootShellExecutor.runCommand("$bin add_sus_kstat \"$targetPath\"")
    }

    override suspend fun setUname(release: String, version: String): ShellResult {
        val bin = getBinaryPath()
        return RootShellExecutor.runCommand("$bin set_uname \"$release\" \"$version\"")
    }

    override suspend fun setLogging(enable: Boolean): ShellResult {
        val bin = getBinaryPath()
        val flag = if (enable) "1" else "0"
        return RootShellExecutor.runCommand("$bin enable_log $flag")
    }
}
