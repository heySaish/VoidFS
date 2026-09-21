package com.voidkernel.voidfs.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SusfsFeatures(
    @SerialName("sus_path") val susPath: Boolean = false,
    @SerialName("sus_mount") val susMount: Boolean = false,
    @SerialName("sus_kstat") val susKstat: Boolean = false,
    @SerialName("set_uname") val setUname: Boolean = false,
    @SerialName("try_umount") val tryUmount: Boolean = false,
    @SerialName("sus_su") val susSu: Boolean = false
)

@Serializable
data class SusfsStatus(
    val supported: Boolean = false,
    val version: String = "Unknown",
    val kernel: String = "Unknown",
    val features: SusfsFeatures = SusfsFeatures()
)

data class SusfsPathItem(
    val id: String,
    val path: String,
    val enabled: Boolean = true
)

data class SusfsMountItem(
    val id: String,
    val mountPath: String,
    val enabled: Boolean = true
)

data class SusfsKstatItem(
    val id: String,
    val targetPath: String,
    val spoofPath: String = "",
    val enabled: Boolean = true
)

data class UnameConfig(
    val release: String = "",
    val version: String = "",
    val enabled: Boolean = false
)
