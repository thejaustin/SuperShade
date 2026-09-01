package com.supershade.domain.update

import com.supershade.BuildConfig
import com.supershade.settings.ShadeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class UpdateRepository(
    private val checker: UpdateChecker,
    private val settings: ShadeSettings,
) {

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    suspend fun initSession() {
        val lastSeen = settings.lastSeenVersion.first()
        val current = BuildConfig.VERSION_NAME
        if (lastSeen.isNotEmpty() && lastSeen != current) {
            _showWhatsNew.value = true
        }
        settings.setLastSeenVersion(current)
    }

    suspend fun checkForUpdate() {
        val info = checker.check(BuildConfig.VERSION_NAME) ?: return
        if (info.isUpdateAvailable) {
            _availableUpdate.value = info
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
