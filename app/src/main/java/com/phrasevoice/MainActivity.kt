package com.phrasevoice

import android.Manifest
import android.app.KeyguardManager
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
import androidx.lifecycle.lifecycleScope
import com.phrasevoice.system.QuickReturnNotifier
import com.phrasevoice.ui.PhraseVoiceRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val communicationRequestKey = mutableStateOf(0)
    private val notificationReplayRequestKey = mutableStateOf(0)
    private val notificationReplayText = mutableStateOf("")
    private val notificationStopRequestKey = mutableStateOf(0)
    private val lockScreenCommunicationEnabled = mutableStateOf(true)
    private val deviceLocked = mutableStateOf(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            QuickReturnNotifier.show(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as PhraseVoiceApp).container
        configureCommunicationWindow(lockScreenCommunicationEnabled.value)
        refreshDeviceLockedState()
        handleIntent(intent)

        lifecycleScope.launch {
            appContainer.settingsRepository.settings.collect { settings ->
                lockScreenCommunicationEnabled.value = settings.lockScreenCommunicationEnabled
                configureCommunicationWindow(settings.lockScreenCommunicationEnabled)
                refreshDeviceLockedState()
            }
        }

        setContent {
            PhraseVoiceRoot(
                container = appContainer,
                communicationRequestKey = communicationRequestKey.value,
                notificationReplayRequestKey = notificationReplayRequestKey.value,
                notificationReplayText = notificationReplayText.value,
                notificationStopRequestKey = notificationStopRequestKey.value,
                lockScreenActive = lockScreenCommunicationEnabled.value && deviceLocked.value,
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
        refreshDeviceLockedState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            refreshDeviceLockedState()
        }
    }

    private fun configureCommunicationWindow(enabled: Boolean) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(enabled)
            setTurnScreenOn(enabled)
        } else {
            @Suppress("DEPRECATION")
            if (enabled) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                )
            }
        }
    }

    private fun refreshDeviceLockedState() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        deviceLocked.value = keyguardManager?.isKeyguardLocked == true
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
        when {
            QuickReturnNotifier.isReplayIntent(intent) -> {
                notificationReplayText.value = QuickReturnNotifier.replayText(intent)
                notificationReplayRequestKey.value += 1
                communicationRequestKey.value += 1
            }
            QuickReturnNotifier.isStopIntent(intent) -> {
                notificationStopRequestKey.value += 1
                communicationRequestKey.value += 1
            }
            QuickReturnNotifier.isOpenCommunicationIntent(intent) -> {
                communicationRequestKey.value += 1
            }
        }
    }
}
