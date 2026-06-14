package com.phrasevoice.ui.communicate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phrasevoice.ui.i18n.t

@Composable
fun LargeTextDisplayDialog(
    text: String,
    display: CommunicationDisplayUiState,
    isPlaying: Boolean,
    canSpeak: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val toneColors = communicationTextToneColors(display.textTone)
    val textScale = display.textScale.coerceIn(0.85f, 1.35f)
    val baseStyle = MaterialTheme.typography.displayLarge

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = toneColors.containerColor,
            contentColor = toneColors.contentColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = t("大字展示", "Large Text"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = t("关闭", "Close"))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.15f),
                ) {
                    Text(
                        text = text.ifBlank { t("点按短语或输入文字", "Tap a phrase or enter text") },
                        style = baseStyle.copy(
                            fontSize = (baseStyle.fontSize.value * textScale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        ),
                        color = if (text.isBlank()) {
                            toneColors.contentColor.copy(alpha = 0.5f)
                        } else {
                            toneColors.contentColor
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = if (isPlaying) onStop else onSpeak,
                        enabled = isPlaying || canSpeak,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        colors = if (isPlaying) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        },
                        modifier = Modifier
                            .weight(1.15f)
                            .height(54.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(if (isPlaying) t("停止", "Stop") else t("朗读", "Speak"), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReplay,
                        enabled = canSpeak,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, toneColors.contentColor.copy(alpha = 0.32f)),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Replay,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(t("重播", "Replay"), fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

internal data class CommunicationTextToneColors(
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
internal fun communicationTextToneColors(tone: String): CommunicationTextToneColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    return when (tone) {
        "sky" -> if (isDark) {
            CommunicationTextToneColors(Color(0xFF0D3751), Color(0xFFDDF2FF))
        } else {
            CommunicationTextToneColors(Color(0xFFDCEEFF), Color(0xFF102F48))
        }
        "warm" -> if (isDark) {
            CommunicationTextToneColors(Color(0xFF4B351C), Color(0xFFFFE6BF))
        } else {
            CommunicationTextToneColors(Color(0xFFF7E5C8), Color(0xFF4B3216))
        }
        "lavender" -> if (isDark) {
            CommunicationTextToneColors(Color(0xFF33275D), Color(0xFFEDE6FF))
        } else {
            CommunicationTextToneColors(Color(0xFFE9E2FF), Color(0xFF2F245C))
        }
        else -> if (isDark) {
            CommunicationTextToneColors(Color(0xFF064E3B), Color(0xFFD1FAE5))
        } else {
            CommunicationTextToneColors(Color(0xFFD2E8DD), Color(0xFF0A2B18))
        }
    }
}
