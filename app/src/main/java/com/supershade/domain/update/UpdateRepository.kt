package com.supershade.domain.update

import com.supershade.BuildConfig
import com.supershade.settings.ShadeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

class UpdateRepository(
    private val checker: UpdateChecker,
    private val settings: ShadeSettings,
) {

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    private val checkMutex = Mutex()

    suspend fun initSession() {
        val lastSeen = settings.lastSeenVersion.first()
        val current = BuildConfig.VERSION_NAME
        if (lastSeen != current) {          // "" != "1.1.0" on fresh install → true
            _showWhatsNew.value = true
        }
        settings.setLastSeenVersion(current)
    }

    suspend fun checkForUpdate() {
        if (!checkMutex.tryLock()) return   // already in-flight, skip
        try {
            val info = checker.check(BuildConfig.VERSION_NAME) ?: return
            _availableUpdate.value = if (info.isUpdateAvailable) info else null
        } finally {
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
