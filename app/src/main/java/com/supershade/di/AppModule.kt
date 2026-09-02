package com.supershade.di

import com.supershade.domain.brightness.BrightnessRepository
import com.supershade.domain.media.MediaRepository
import com.supershade.domain.notification.NotificationRepository
import com.supershade.domain.tile.TileRepository
import com.supershade.domain.tile.TileToggler
import com.supershade.domain.update.UpdateChecker
import com.supershade.domain.update.UpdateRepository
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.ShizukuPlusConnector
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.viewmodel.ShadeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { ShadeSettings(androidContext()) }
    single { ShizukuPlusConnector(androidContext()) }
    single { StatusBarGovernor(androidContext(), get()) }
    single { NotificationRepository() }
    single { TileRepository(androidContext(), get()) }
    single { TileToggler(androidContext(), get()) }
    single { MediaRepository(androidContext()) }
    single { BrightnessRepository(androidContext()) }
    single { UpdateChecker() }
    single { UpdateRepository(get(), get()) }
    // Singleton (not viewModel) because ShadeService — not an Activity — owns it.
    // viewModelScope still works; it's only cancelled if onCleared() is called.
    single {
        ShadeViewModel(
            notificationRepo = get(),
            tileRepo = get(),
            tileToggler = get(),
            mediaRepo = get(),
            brightnessRepo = get(),
            settings = get(),
            governor = get(),
        )
    }
}
