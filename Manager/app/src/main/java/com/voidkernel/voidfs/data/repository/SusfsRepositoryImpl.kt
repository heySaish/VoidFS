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

        val locations = listOf(
            "/data/adb/ksu/bin/susfs",
            "/system/bin/susfs",
            "/system/xbin/susfs",
            "susfs"
        )

        for (loc in locations) {
            val check = RootShellExecutor.runCommand("which $loc || [ -f $loc ] && echo $loc")
            if (check.isSuccess && check.stdout.isNotBlank()) {
                val path = check.stdout.lines().firstOrNull { it.isNotBlank() } ?: loc
                cachedBinaryPath = path
                return path
            }
        }

        val defaultPath = "susfs"
        cachedBinaryPath = defaultPath
        return defaultPath
    }

    override suspend fun fetchStatus(): SusfsStatus {
        val bin = getBinaryPath()
        val result = RootShellExecutor.runCommand("$bin status --json")

        if (result.isSuccess && result.stdout.isNotBlank()) {
            return try {
                json.decodeFromString<SusfsStatus>(result.stdout)
            } catch (e: Exception) {
                // Fallback parsing if JSON has extra text
                val jsonStart = result.stdout.indexOf('{')
                val jsonEnd = result.stdout.lastIndexOf('}')
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    val jsonSub = result.stdout.substring(jsonStart, jsonEnd + 1)
                    json.decodeFromString<SusfsStatus>(jsonSub)
                } else {
                    SusfsStatus(supported = false)
                }
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
        // Standard removal or unmount
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
