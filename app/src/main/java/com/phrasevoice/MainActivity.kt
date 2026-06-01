package com.phrasevoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.phrasevoice.ui.PhraseVoiceRoot
import com.phrasevoice.ui.theme.PhraseVoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as PhraseVoiceApp).container
        setContent {
            PhraseVoiceTheme {
                PhraseVoiceRoot(container = appContainer)
            }
        }
    }
}
