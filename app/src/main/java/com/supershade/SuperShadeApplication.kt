package com.supershade

import android.app.Application
import com.supershade.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

class SuperShadeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        HiddenApiBypass.setHiddenApiExemptions("")
        startKoin {
            androidContext(this@SuperShadeApplication)
            modules(appModule)
        }
    }
}
