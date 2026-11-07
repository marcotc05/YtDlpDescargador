package com.marcotc05.ytdlpdescargador.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.marcotc05.ytdlpdescargador.model.DownloadConfig

@Composable
fun SettingsDialog(
    context: Context,
    config: DownloadConfig,
    onConfigChanged: (DownloadConfig) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AeroLiquidCard(cornerRadius = 24.dp) {
            SettingsView(onBack = onDismiss)
        }
    }
}