package com.marcotc05.ytdlpdescargador

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcotc05.ytdlpdescargador.engine.DownloadEngine
import com.marcotc05.ytdlpdescargador.model.DownloadConfig
import com.marcotc05.ytdlpdescargador.model.SettingsManager
import com.marcotc05.ytdlpdescargador.ui.AeroLiquidCard
import com.marcotc05.ytdlpdescargador.ui.SettingsView
import com.marcotc05.ytdlpdescargador.ui.theme.AeroBlue
import com.marcotc05.ytdlpdescargador.ui.theme.AeroCyanAccent
import com.marcotc05.ytdlpdescargador.ui.theme.AeroDarkBgBottom
import com.marcotc05.ytdlpdescargador.ui.theme.AeroDarkBgMid
import com.marcotc05.ytdlpdescargador.ui.theme.AeroDarkBgTop
import com.marcotc05.ytdlpdescargador.ui.theme.AeroGreenSuccess
import com.marcotc05.ytdlpdescargador.ui.theme.AeroLightBgBottom
import com.marcotc05.ytdlpdescargador.ui.theme.AeroLightBgMid
import com.marcotc05.ytdlpdescargador.ui.theme.AeroLightBgTop
import com.marcotc05.ytdlpdescargador.ui.theme.AeroMagentaAccent
import com.marcotc05.ytdlpdescargador.ui.theme.AeroTeal
import com.marcotc05.ytdlpdescargador.ui.theme.AeroTextPrimaryDark
import com.marcotc05.ytdlpdescargador.ui.theme.AeroTextSecondaryDark
import com.marcotc05.ytdlpdescargador.ui.theme.DownAllTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager.getInstance(context) }
            val prefs by settingsManager.state.collectAsState()

            val useDark = when (prefs.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            DownAllTheme(darkTheme = useDark) {
                MainContent(isDark = useDark)
            }
        }
    }
}

@Composable
fun MainContent(isDark: Boolean) {
    var showSettings by remember { mutableStateOf(false) }

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(AeroDarkBgTop, AeroDarkBgMid, AeroDarkBgBottom))
    } else {
        Brush.verticalGradient(listOf(AeroLightBgTop, AeroLightBgMid, AeroLightBgBottom))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AeroCyanAccent.copy(alpha = if (isDark) 0.16f else 0.25f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(
                        listOf(AeroBlue.copy(alpha = if (isDark) 0.14f else 0.22f), Color.Transparent)
                    )
                )
        )

        Crossfade(targetState = showSettings, label = "view_transition") { inSettings ->
            if (inSettings) {
                SettingsView(onBack = { showSettings = false })
            } else {
                HomeScreen(onOpenSettings = { showSettings = true })
            }
        }
    }
}

@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEngineReady by DownAllApp.engineReady.collectAsState()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val prefs by settingsManager.state.collectAsState()

    var urlText by remember { mutableStateOf("") }
    var isAudioOnly by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadLog by remember { mutableStateOf("Sistema listo.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isEngineReady) AeroGreenSuccess else AeroMagentaAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DOWNALL",
                    color = AeroTeal,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración",
                    tint = AeroCyanAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AeroLiquidCard(cornerRadius = 26.dp) {
            Text(
                text = "ENLACE MULTIMEDIA",
                color = AeroCyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, AeroCyanAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = AeroTextPrimaryDark,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(AeroCyanAccent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (urlText.isEmpty()) {
                            Text(
                                text = "Pega un enlace de YouTube u otro sitio...",
                                color = AeroTextSecondaryDark.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )

                if (urlText.isNotEmpty()) {
                    IconButton(onClick = { urlText = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Borrar", tint = AeroTextSecondaryDark)
                    }
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            urlText = clip.getItemAt(0).text?.toString() ?: ""
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Pegar", tint = AeroCyanAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AeroLiquidCard(cornerRadius = 26.dp) {
            Text(
                text = "MODO DE DESCARGA",
                color = AeroCyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (!isAudioOnly) AeroTeal.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f))
                        .border(1.2.dp, if (!isAudioOnly) AeroTeal else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable { isAudioOnly = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VIDEO (${prefs.defaultFormat})",
                        color = if (!isAudioOnly) AeroCyanAccent else AeroTextSecondaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isAudioOnly) AeroBlue.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f))
                        .border(1.2.dp, if (isAudioOnly) AeroBlue else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable { isAudioOnly = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SOLO AUDIO (MP3)",
                        color = if (isAudioOnly) AeroCyanAccent else AeroTextSecondaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val buttonBrush = if (urlText.isNotBlank() && isEngineReady && !isDownloading) {
                Brush.horizontalGradient(listOf(AeroTeal, AeroBlue))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFF2A3644), Color(0xFF1E2733)))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(buttonBrush)
                    .border(
                        1.dp,
                        if (urlText.isNotBlank()) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(enabled = urlText.isNotBlank() && isEngineReady && !isDownloading) {
                        isDownloading = true
                        downloadProgress = 0f
                        downloadLog = "Iniciando descarga..."

                        scope.launch(Dispatchers.IO) {
                            val config = DownloadConfig(
                                isAudioOnly = isAudioOnly,
                                forceIpv4 = prefs.forceIpv4,
                                socketTimeoutSeconds = prefs.socketTimeoutSeconds,
                                maxRetries = prefs.retries
                            )
                            DownloadEngine.download(
                                context = context,
                                url = urlText.trim(),
                                config = config,
                                onProgress = { progress, line ->
                                    scope.launch(Dispatchers.Main) {
                                        downloadProgress = progress / 100f
                                        if (!line.isBlank()) {
                                            downloadLog = line
                                        }
                                    }
                                },
                                onComplete = { success, message ->
                                    scope.launch(Dispatchers.Main) {
                                        isDownloading = false
                                        downloadLog = message
                                        Toast.makeText(
                                            context,
                                            if (success) "Descarga finalizada" else "Fallo en descarga",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDownloading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("DESCARGANDO...", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    } else {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEngineReady) "DESCARGAR" else "PREPARANDO MOTOR...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = isDownloading || downloadProgress > 0f) {
            AeroLiquidCard(cornerRadius = 24.dp) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "PROGRESO", color = AeroCyanAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        color = AeroTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = AeroTeal,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AeroLiquidCard(cornerRadius = 24.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "REGISTRO DE TERMINAL", color = AeroCyanAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("DownAll Log", downloadLog))
                        Toast.makeText(context, "Log copiado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = AeroTextSecondaryDark)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(10.dp)
            ) {
                Text(
                    text = downloadLog,
                    color = AeroTextSecondaryDark,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 6
                )
            }
        }
    }
}