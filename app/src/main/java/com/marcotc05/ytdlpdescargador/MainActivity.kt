package com.marcotc05.ytdlpdescargador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcotc05.ytdlpdescargador.engine.DownloadEngine
import com.marcotc05.ytdlpdescargador.model.DownloadConfig
import com.marcotc05.ytdlpdescargador.ui.AeroLiquidCard
import com.marcotc05.ytdlpdescargador.ui.SettingsDialog
import com.marcotc05.ytdlpdescargador.ui.theme.DownAllTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownAllTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF060913)
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val isEngineReady by DownAllApp.isEngineReady.collectAsState()

    var inputUrl by remember { mutableStateOf("") }
    var statusLog by remember { mutableStateOf("SISTEMA LISTO: Introduce un enlace para comenzar.") }
    var progressPercent by remember { mutableFloatStateOf(0f) }
    var isProcessing by remember { mutableStateOf(false) }
    var validationNotice by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf(DownloadConfig()) }

    if (showSettings) {
        SettingsDialog(
            context = context,
            config = config,
            onConfigChanged = { config = it },
            onDismiss = { showSettings = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080D1A),
                        Color(0xFF060913),
                        Color(0xFF03050A)
                    )
                )
            )
    ) {
        // Orbes de luz ambiental de fondo (Aero Ambient Glow)
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-40).dp, y = (-20).dp)
                .background(Brush.radialGradient(listOf(Color(0x2600F0FF), Color.Transparent)), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp, y = 120.dp)
                .background(Brush.radialGradient(listOf(Color(0x20FF007F), Color.Transparent)), CircleShape)
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DOWNALL",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                fontSize = 22.sp,
                                color = Color(0xFF00F0FF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3300F0FF))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "v1.0",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF00F0FF)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x1A00F0FF))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color(0xFF00F0FF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador de Estado del Motor Nativo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1F141F36))
                        .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                color = if (isEngineReady) Color(0xFF00FF66) else Color(0xFFFF3B30),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEngineReady) "MOTOR yt-dlp ONLINE" else "INICIALIZANDO MOTOR...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEngineReady) Color(0xFF00FF66) else Color(0xFFFF8888)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tarjeta 1: Entrada de Enlace (Liquid Glass)
                AeroLiquidCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF00F0FF)
                ) {
                    Text(
                        text = "ENLACE DE DESCARGA",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = {
                            inputUrl = it
                            if (validationNotice != null) validationNotice = null
                        },
                        placeholder = { Text("Pega un enlace de YouTube, video o audio...", color = Color(0xFF6B7A99), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                if (inputUrl.isNotBlank()) {
                                    IconButton(onClick = { inputUrl = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                                    }
                                }
                                // Botón PEGAR directo del portapapeles
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x3300F0FF))
                                        .clickable {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                inputUrl = clip.trim()
                                                validationNotice = null
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PEGAR", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0x4400F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x22050B14),
                            unfocusedContainerColor = Color(0x18050B14)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selector de Formato (Pill Tabs Aero)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!config.isAudioOnly) Color(0x4000F0FF) else Color(0x140E182A))
                                .border(1.dp, if (!config.isAudioOnly) Color(0xFF00F0FF) else Color(0x2200F0FF), RoundedCornerShape(12.dp))
                                .clickable { config = config.copy(isAudioOnly = false) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (!config.isAudioOnly) Color.White else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("VIDEO (MP4)", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (!config.isAudioOnly) Color.White else Color.Gray)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (config.isAudioOnly) Color(0x40FF007F) else Color(0x140E182A))
                                .border(1.dp, if (config.isAudioOnly) Color(0xFFFF007F) else Color(0x22FF007F), RoundedCornerShape(12.dp))
                                .clickable { config = config.copy(isAudioOnly = true) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = if (config.isAudioOnly) Color.White else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AUDIO (MP3)", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (config.isAudioOnly) Color.White else Color.Gray)
                            }
                        }
                    }
                }

                // Alerta de validación
                AnimatedVisibility(
                    visible = validationNotice != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FF3B30))
                            .border(1.dp, Color(0xFFFF3B30), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = validationNotice ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botón DESCARGAR con borde y brillo especular
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = if (isEngineReady && !isProcessing) {
                                Brush.horizontalGradient(listOf(Color(0xFFFF0055), Color(0xFFFF0080), Color(0xFF7928CA)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF333D4D), Color(0xFF222834)))
                            }
                        )
                        .border(
                            BorderStroke(
                                1.2.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x80FFFFFF), Color(0x33FFFFFF), Color(0x00000000))
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = isEngineReady && !isProcessing) {
                            if (inputUrl.isBlank()) {
                                validationNotice = "POR FAVOR INGRESA O PEGA UN ENLACE DE VIDEO"
                                return@clickable
                            }
                            validationNotice = null
                            isProcessing = true
                            progressPercent = 0f
                            statusLog = "Iniciando motor de extraccion..."

                            scope.launch {
                                DownloadEngine.executeDownload(context, inputUrl, config) { prog, line ->
                                    progressPercent = prog
                                    statusLog = line
                                }.onSuccess { resultMsg ->
                                    statusLog = "DESCARGA COMPLETADA CON EXITO!\n$resultMsg"
                                    isProcessing = false
                                }.onFailure { err ->
                                    statusLog = "FALLA EN DESCARGA:\n" + (err.message ?: err.toString())
                                    isProcessing = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color.Transparent)))
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProcessing) "PROCESANDO DESCARGA..." else "INICIAR DESCARGA",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    }
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PROGRESO:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF00F0FF))
                            Text("${progressPercent.toInt()}%", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF00F0FF),
                            trackColor = Color(0x3300F0FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tarjeta 2: Terminal de Consola y Logs
                AeroLiquidCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = Color(0xFF7928CA)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TERMINAL DE ESTADO",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F0FF)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2200F0FF))
                                .clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(statusLog))
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("COPIAR LOG", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF00F0FF))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33000000))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = statusLog,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFFD4E0F5)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}