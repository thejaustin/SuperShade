package com.supershade.domain.update

import com.supershade.BuildConfig
import com.supershade.settings.ShadeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

class UpdateRepository(
    private val checker: UpdateChecker,
    private val settings: ShadeSettings,
) {

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    private val checkMutex = Mutex()

    suspend fun initSession() {
        val lastSeen = settings.lastSeenVersion.first()
        val current = BuildConfig.VERSION_NAME
        if (lastSeen.isNotBlank() && lastSeen != current) {
            _showWhatsNew.value = true
        }
        settings.setLastSeenVersion(current)
    }

    suspend fun checkForUpdate(): UpdateCheckResult {
        if (!checkMutex.tryLock()) {
            return UpdateCheckResult.Error("Check already in progress")
        }
        _isChecking.value = true
        try {
            val info = checker.check(BuildConfig.VERSION_NAME)
            return if (info == null) {
                UpdateCheckResult.Error("Could not connect to update server")
            } else if (info.isUpdateAvailable) {
                _availableUpdate.value = info
                UpdateCheckResult.UpdateAvailable(info)
            } else {
                _availableUpdate.value = null
                UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
        } finally {
            _isChecking.value = false
            checkMutex.unlock()
        }
    }

    fun showWhatsNewManual() {
        _showWhatsNew.value = true
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
    }
}
