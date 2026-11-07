package com.marcotc05.ytdlpdescargador.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.marcotc05.ytdlpdescargador.DownAllApp
import com.marcotc05.ytdlpdescargador.model.DownloadConfig

@Composable
fun SettingsDialog(
    context: Context,
    config: DownloadConfig,
    onConfigChanged: (DownloadConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var forceIpv4 by remember { mutableStateOf(config.forceIpv4) }
    var timeout by remember { mutableStateOf(config.socketTimeoutSeconds.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141926))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "CONFIGURACION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    color = Color(0xFF00F0FF)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Forzar IPv4", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(
                        checked = forceIpv4,
                        onCheckedChange = {
                            forceIpv4 = it
                            onConfigChanged(config.copy(forceIpv4 = it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = timeout,
                    onValueChange = {
                        timeout = it
                        it.toIntOrNull()?.let { t -> onConfigChanged(config.copy(socketTimeoutSeconds = t)) }
                    },
                    label = { Text("Socket Timeout (segundos)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val logReport = StringBuilder()
                            .appendLine("=== DOWNALL SYSTEM DIAGNOSTIC ===")
                            .appendLine("Device: " + Build.MANUFACTURER + " " + Build.MODEL)
                            .appendLine("Android SDK: " + Build.VERSION.SDK_INT)
                            .appendLine("ABIs: " + Build.SUPPORTED_ABIS.joinToString(", "))
                            .appendLine("Engine Report: " + DownAllApp.engineInitLog)
                            .toString()

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, logReport)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Exportar Diagnostico"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282C3E))
                ) {
                    Text("EXPORTAR LOG DIAGNOSTICO", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}