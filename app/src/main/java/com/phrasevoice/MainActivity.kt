package com.phrasevoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.phrasevoice.system.QuickReturnNotifier
import com.phrasevoice.ui.PhraseVoiceRoot

class MainActivity : ComponentActivity() {
    private val communicationRequestKey = mutableStateOf(0)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            QuickReturnNotifier.show(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureCommunicationWindow()
        handleIntent(intent)

        val appContainer = (application as PhraseVoiceApp).container
        setContent {
            PhraseVoiceRoot(
                container = appContainer,
                communicationRequestKey = communicationRequestKey.value,
            )
        }

        requestNotificationPermissionAndShow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        QuickReturnNotifier.show(this)
    }

    override fun onResume() {
        super.onResume()
        QuickReturnNotifier.show(this)
    }

    private fun configureCommunicationWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    private fun requestNotificationPermissionAndShow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            QuickReturnNotifier.show(this)
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            QuickReturnNotifier.show(this)
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (QuickReturnNotifier.isOpenCommunicationIntent(intent)) {
            communicationRequestKey.value += 1
        }
    }
}
