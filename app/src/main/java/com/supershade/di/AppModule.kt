package com.supershade.di

import com.supershade.domain.brightness.BrightnessRepository
import com.supershade.domain.media.MediaRepository
import com.supershade.domain.notification.NotificationRepository
import com.supershade.domain.tile.TileRepository
import com.supershade.domain.tile.TileToggler
import com.supershade.domain.update.UpdateChecker
import com.supershade.domain.update.UpdateRepository
import com.supershade.haptics.SuperHaptics
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.ShizukuPlusConnector
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.viewmodel.ShadeViewModel
import com.supershade.overlay.ShadeWindowManager
import com.supershade.service.HeadsUpOverlay
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { SuperHaptics(androidContext()) }
    single { ShadeSettings(androidContext()) }
    single { ShizukuPlusConnector(androidContext()) }
    single { StatusBarGovernor(androidContext(), get()) }
    single { NotificationRepository() }
    single { com.supershade.domain.system.SystemStatusRepository(androidContext()) }
    single { com.supershade.domain.audio.AudioRepository(androidContext()) }
    single { TileRepository(androidContext(), get(), get()) }
    single { TileToggler(androidContext(), get(), get(), get(), get()) }
    single { MediaRepository(androidContext()) }
    single { BrightnessRepository(androidContext(), get()) }
    single { UpdateChecker() }
    single { UpdateRepository(get(), get()) }
    // Singleton (not viewModel) because ShadeService — not an Activity — owns it.
    // viewModelScope still works; it's only cancelled if onCleared() is called.
    single {
        ShadeViewModel(
            context = androidContext(),
            notificationRepo = get(),
            tileRepo = get(),
            tileToggler = get(),
            mediaRepo = get(),
            brightnessRepo = get(),
            settings = get(),
            governor = get(),
            audioRepo = get(),
            systemStatusRepo = get(),
        )
    }
    single { ShadeWindowManager(androidContext(), get(), get(), get()) }
    single { HeadsUpOverlay(androidContext(), get(), get()) }
}
