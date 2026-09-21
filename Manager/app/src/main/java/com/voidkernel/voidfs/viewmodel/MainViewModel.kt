package com.voidkernel.voidfs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidkernel.voidfs.data.model.SusfsKstatItem
import com.voidkernel.voidfs.data.model.SusfsMountItem
import com.voidkernel.voidfs.data.model.SusfsPathItem
import com.voidkernel.voidfs.data.model.SusfsStatus
import com.voidkernel.voidfs.data.repository.SusfsRepository
import com.voidkernel.voidfs.data.repository.SusfsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = true,
    val status: SusfsStatus = SusfsStatus(),
    val binaryPath: String = "susfs",
    val hiddenPaths: List<SusfsPathItem> = emptyList(),
    val hiddenMounts: List<SusfsMountItem> = emptyList(),
    val spoofedKstats: List<SusfsKstatItem> = emptyList(),
    val unameRelease: String = "",
    val unameVersion: String = "",
    val isLoggingEnabled: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MainViewModel(
    private val repository: SusfsRepository = SusfsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val binPath = repository.getBinaryPath()
            val status = repository.fetchStatus()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                status = status,
                binaryPath = binPath,
                unameRelease = if (status.features.setUname) status.kernel else _uiState.value.unameRelease
            )
        }
    }

    fun addPath(path: String) {
        if (path.isBlank()) return
        viewModelScope.launch {
            val result = repository.addSusPath(path.trim())
            if (result.isSuccess) {
                val newPaths = _uiState.value.hiddenPaths.toMutableList().apply {
                    add(SusfsPathItem(id = System.currentTimeMillis().toString(), path = path.trim()))
                }
                _uiState.value = _uiState.value.copy(
                    hiddenPaths = newPaths,
                    successMessage = "Path added: $path"
                )
                refreshAll()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.stderr.ifBlank { "Failed to add path" })
            }
        }
    }

    fun removePath(item: SusfsPathItem) {
        viewModelScope.launch {
            val result = repository.removeSusPath(item.path)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    hiddenPaths = _uiState.value.hiddenPaths.filter { it.id != item.id },
                    successMessage = "Path removed"
                )
                refreshAll()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.stderr.ifBlank { "Failed to remove path" })
            }
        }
    }

    fun addMount(mountPath: String) {
        if (mountPath.isBlank()) return
        viewModelScope.launch {
            val result = repository.addSusMount(mountPath.trim())
            if (result.isSuccess) {
                val newMounts = _uiState.value.hiddenMounts.toMutableList().apply {
                    add(SusfsMountItem(id = System.currentTimeMillis().toString(), mountPath = mountPath.trim()))
                }
                _uiState.value = _uiState.value.copy(
                    hiddenMounts = newMounts,
                    successMessage = "Mount added: $mountPath"
                )
                refreshAll()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.stderr.ifBlank { "Failed to add mount" })
            }
        }
    }

    fun removeMount(item: SusfsMountItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                hiddenMounts = _uiState.value.hiddenMounts.filter { it.id != item.id },
                successMessage = "Mount removed"
            )
            refreshAll()
        }
    }

    fun addKstat(targetPath: String) {
        if (targetPath.isBlank()) return
        viewModelScope.launch {
            val result = repository.addSusKstat(targetPath.trim())
            if (result.isSuccess) {
                val newKstats = _uiState.value.spoofedKstats.toMutableList().apply {
                    add(SusfsKstatItem(id = System.currentTimeMillis().toString(), targetPath = targetPath.trim()))
                }
                _uiState.value = _uiState.value.copy(
                    spoofedKstats = newKstats,
                    successMessage = "Kstat added: $targetPath"
                )
                refreshAll()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.stderr.ifBlank { "Failed to add kstat" })
            }
        }
    }

    fun removeKstat(item: SusfsKstatItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                spoofedKstats = _uiState.value.spoofedKstats.filter { it.id != item.id },
                successMessage = "Kstat removed"
            )
            refreshAll()
        }
    }

    fun applyUnameSpoof(release: String, version: String) {
        viewModelScope.launch {
            val result = repository.setUname(release, version)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    unameRelease = release,
                    unameVersion = version,
                    successMessage = "Uname spoof applied!"
                )
                refreshAll()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.stderr.ifBlank { "Failed to set uname" })
            }
        }
    }

    fun toggleLogging(enable: Boolean) {
        viewModelScope.launch {
            val result = repository.setLogging(enable)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoggingEnabled = enable,
                    successMessage = if (enable) "Logging enabled" else "Logging disabled"
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.stderr.ifBlank { "Failed to toggle log" })
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
