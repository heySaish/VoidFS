package com.voidkernel.voidfs.data.repository

import com.voidkernel.voidfs.data.model.SusfsKstatItem
import com.voidkernel.voidfs.data.model.SusfsMountItem
import com.voidkernel.voidfs.data.model.SusfsPathItem
import com.voidkernel.voidfs.data.model.SusfsStatus
import com.voidkernel.voidfs.data.model.UnameConfig
import com.voidkernel.voidfs.data.shell.ShellResult
import kotlinx.coroutines.flow.Flow

interface SusfsRepository {
    suspend fun fetchStatus(): SusfsStatus
    suspend fun getBinaryPath(): String
    suspend fun addSusPath(path: String): ShellResult
    suspend fun removeSusPath(path: String): ShellResult
    suspend fun addSusMount(mountPath: String): ShellResult
    suspend fun removeSusMount(mountPath: String): ShellResult
    suspend fun addSusKstat(targetPath: String): ShellResult
    suspend fun removeSusKstat(targetPath: String): ShellResult
    suspend fun setUname(release: String, version: String): ShellResult
    suspend fun setLogging(enable: Boolean): ShellResult
}
