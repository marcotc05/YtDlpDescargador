package com.marcotc05.ytdlpdescargador.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.marcotc05.ytdlpdescargador.DownAllApp
import com.marcotc05.ytdlpdescargador.model.SettingsManager
import com.marcotc05.ytdlpdescargador.ui.theme.AeroBlue
import com.marcotc05.ytdlpdescargador.ui.theme.AeroCyanAccent
import com.marcotc05.ytdlpdescargador.ui.theme.AeroGreenSuccess
import com.marcotc05.ytdlpdescargador.ui.theme.AeroTeal
import com.marcotc05.ytdlpdescargador.ui.theme.AeroTextPrimaryDark
import com.marcotc05.ytdlpdescargador.ui.theme.AeroTextSecondaryDark
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsView(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val prefs by settingsManager.state.collectAsState()

    var isUpdatingYtDlp by remember { mutableStateOf(false) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var ytDlpVersion by remember {
        mutableStateOf(
            try {
                YoutubeDL.getInstance().version(context) ?: "0.18.1"
            } catch (e: Exception) {
                "0.18.1"
            }
        )
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (_: Exception) {}
            val path = it.path?.substringAfterLast(':') ?: "Seleccionada"
            settingsManager.updateFolder(it.toString(), "Almacenamiento/$path")
            Toast.makeText(context, "Carpeta: $path", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = AeroCyanAccent
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "CONFIGURACIÓN",
                    color = AeroTextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Aero Liquid Glass Engine",
                    color = AeroTeal,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AeroLiquidCard(cornerRadius = 24.dp) {
            Text(
                text = "ALMACENAMIENTO Y DESCARGAS",
                color = AeroCyanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { folderPickerLauncher.launch(null) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Carpeta de destino", color = AeroTextPrimaryDark, fontSize = 14.sp)
                    Text(
                        text = prefs.downloadFolderDisplay,
                        color = AeroTeal,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Elegir carpeta",
                    tint = AeroCyanAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Formato preferido", color = AeroTextSecondaryDark, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MP4", "MKV", "MP3").forEach { format ->
                    val selected = prefs.defaultFormat.equals(format, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) AeroTeal else Color.White.copy(alpha = 0.08f))
                            .clickable { settingsManager.updateDefaultFormat(format) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = format,
                            color = if (selected) Color.Black else AeroTextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Reintentos automáticos: ${prefs.retries}",
                color = AeroTextSecondaryDark,
                fontSize = 12.sp
            )
            Slider(
                value = prefs.retries.toFloat(),
                onValueChange = { settingsManager.updateRetries(it.toInt()) },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(thumbColor = AeroTeal, activeTrackColor = AeroCyanAccent)
            )

            Text(
                text = "Tiempo de espera (Socket timeout): ${prefs.socketTimeoutSeconds}s",
                color = AeroTextSecondaryDark,
                fontSize = 12.sp
            )
            Slider(
                value = prefs.socketTimeoutSeconds.toFloat(),
                onValueChange = { settingsManager.updateTimeout(it.toInt()) },
                valueRange = 10f..120f,
                colors = SliderDefaults.colors(thumbColor = AeroBlue, activeTrackColor = AeroCyanAccent)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Forzar IPv4", color = AeroTextPrimaryDark, fontSize = 14.sp)
                    Text(
                        text = "Evita cuelgues en redes móviles",
                        color = AeroTextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = prefs.forceIpv4,
                    onCheckedChange = { settingsManager.updateForceIpv4(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = AeroTeal)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AeroLiquidCard(cornerRadius = 24.dp) {
            Text(
                text = "MOTOR NATIVO YT-DLP",
                color = AeroCyanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "yt-dlp versión", color = AeroTextSecondaryDark, fontSize = 13.sp)
                Text(text = ytDlpVersion, color = AeroGreenSuccess, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Python embebido", color = AeroTextSecondaryDark, fontSize = 13.sp)
                Text(text = "3.8 (youtubedl-android)", color = AeroTextPrimaryDark, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AeroTeal.copy(alpha = 0.25f))
                    .clickable(enabled = !isUpdatingYtDlp) {
                        isUpdatingYtDlp = true
                        updateStatusMessage = "Comprobando actualizaciones..."
                        scope.launch(Dispatchers.IO) {
                            try {
                                val result = YoutubeDL.getInstance().updateYoutubeDL(context)
                                withContext(Dispatchers.Main) {
                                    ytDlpVersion = YoutubeDL.getInstance().version(context) ?: "Actualizado"
                                    updateStatusMessage = "Resultado: ${result?.name ?: "Al día"}"
                                    Toast.makeText(context, "yt-dlp actualizado con éxito", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    updateStatusMessage = "Error: ${e.message}"
                                    Toast.makeText(context, "Fallo al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isUpdatingYtDlp = false
                                }
                            }
                        }
                    }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUpdatingYtDlp) {
                        CircularProgressIndicator(color = AeroCyanAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = AeroCyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isUpdatingYtDlp) "ACTUALIZANDO..." else "ACTUALIZAR YT-DLP",
                        color = AeroTextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            AnimatedVisibility(visible = updateStatusMessage != null) {
                Text(
                    text = updateStatusMessage.orEmpty(),
                    color = AeroTeal,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AeroLiquidCard(cornerRadius = 24.dp) {
            Text(
                text = "APARIENCIA Y RENDIMIENTO",
                color = AeroCyanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Modo de color", color = AeroTextSecondaryDark, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SYSTEM" to "Auto", "DARK" to "Oscuro", "LIGHT" to "Claro").forEach { (key, label) ->
                    val selected = prefs.themeMode == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) AeroBlue else Color.White.copy(alpha = 0.08f))
                            .clickable { settingsManager.updateThemeMode(key) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else AeroTextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Desenfoque Liquid Glass", color = AeroTextPrimaryDark, fontSize = 14.sp)
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Activo con RenderEffect" else "Fallback sin coste de GPU (API < 31)",
                        color = AeroTextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = prefs.glassBlurEnabled,
                    onCheckedChange = { settingsManager.updateGlassBlur(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = AeroTeal)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AeroLiquidCard(cornerRadius = 24.dp) {
            Text(
                text = "DIAGNÓSTICO Y LOGS",
                color = AeroCyanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Exporta especificaciones de hardware, versión del motor nativo y trazas de error sin incluir credenciales ni enlaces privados.",
                color = AeroTextSecondaryDark,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable {
                        exportDiagnosticLog(context, ytDlpVersion, prefs.downloadFolderDisplay)
                    }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = AeroCyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXPORTAR LOG DE ERRORES",
                        color = AeroTextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun exportDiagnosticLog(context: Context, ytDlpVer: String, folder: String) {
    try {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val logReport = buildString {
            appendLine("=== DOWNALL DIAGNOSTIC REPORT ===")
            appendLine("Fecha: $timeStamp")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("ABIs soportadas: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Versión yt-dlp: $ytDlpVer")
            appendLine("Carpeta configurada: $folder")
            appendLine("Estado de inicialización: ${DownAllApp.initLog}")
            appendLine("=================================")
        }

        val cacheDir = File(context.cacheDir, "logs").apply { mkdirs() }
        val logFile = File(cacheDir, "downall_diagnostic_$timeStamp.txt").apply {
            writeText(logReport)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            logFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DownAll Diagnostic - $timeStamp")
            putExtra(Intent.EXTRA_TEXT, logReport)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir diagnóstico DownAll"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error exportando log: ${e.message}", Toast.LENGTH_LONG).show()
    }
}