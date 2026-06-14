package com.phrasevoice

import android.app.Application
import com.phrasevoice.di.AppContainer
import com.phrasevoice.system.QuickReturnNotifier

class PhraseVoiceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        QuickReturnNotifier.ensureChannel(this)
    }
}
