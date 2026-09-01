package com.supershade

import android.app.Application
import com.supershade.di.appModule
import io.sentry.android.core.SentryAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

class SuperShadeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        HiddenApiBypass.setHiddenApiExemptions("")
        initSentry()
        startKoin {
            androidContext(this@SuperShadeApplication)
            modules(appModule)
        }
    }

    private fun initSentry() {
        SentryAndroid.init(this) { options ->
            options.dsn = ""  // set via BuildConfig if needed
            options.environment = BuildConfig.BUILD_TYPE
            options.tracesSampleRate = 0.1
            options.isEnableAutoSessionTracking = true
            options.isSendDefaultPii = false
        }
    }
}
