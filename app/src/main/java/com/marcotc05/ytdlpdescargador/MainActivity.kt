package com.marcotc05.ytdlpdescargador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcotc05.ytdlpdescargador.engine.DownloadEngine
import com.marcotc05.ytdlpdescargador.model.DownloadConfig
import com.marcotc05.ytdlpdescargador.ui.GlassmorphicContainer
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
                    color = MaterialTheme.colorScheme.background
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
    val isEngineReady by DownAllApp.isEngineReady.collectAsState()

    var inputUrl by remember { mutableStateOf("") }
    var statusLog by remember { mutableStateOf("SISTEMA LISTO") }
    var progressPercent by remember { mutableFloatStateOf(0f) }
    var isProcessing by remember { mutableStateOf(false) }
    var queueNotice by remember { mutableStateOf<String?>(null) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DOWNALL v1.0",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color(0xFF00F0FF)
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color(0xFF00F0FF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0B0D17)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LÍNEA BASE - MOTOR yt-dlp NATIVO",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = if (isEngineReady) Color(0xFF00FF66) else Color(0xFFFF3333)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassmorphicContainer(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("URL de Video o Playlist", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0x6600F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = !config.isAudioOnly,
                            onClick = { config = config.copy(isAudioOnly = false) },
                            label = { Text("VIDEO (MP4)", fontFamily = FontFamily.Monospace) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = config.isAudioOnly,
                            onClick = { config = config.copy(isAudioOnly = true) },
                            label = { Text("AUDIO (MP3)", fontFamily = FontFamily.Monospace) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = queueNotice != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(Color(0x3300F0FF), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = queueNotice ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF00F0FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (inputUrl.isBlank()) return@Button
                    queueNotice = "Tarea agregada a la cola. Iniciando motor..."
                    isProcessing = true
                    statusLog = "Ejecutando yt-dlp..."
                    progressPercent = 0f

                    scope.launch {
                        DownloadEngine.executeDownload(context, inputUrl, config) { prog, line ->
                            progressPercent = prog
                            statusLog = line
                        }.onSuccess {
                            statusLog = "Descarga completada satisfactoriamente."
                            queueNotice = null
                            isProcessing = false
                        }.onFailure { err ->
                            statusLog = "FALLA: " + err.message
                            queueNotice = null
                            isProcessing = false
                        }
                    }
                },
                enabled = isEngineReady && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
            ) {
                Text(
                    text = if (isProcessing) "PROCESANDO..." else "DESCARGAR",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF00F0FF),
                    trackColor = Color(0x3300F0FF)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassmorphicContainer(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LOG DE CONSOLA",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusLog,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = Color(0xFFD0D0D0)
                    )
                }
            }
        }
    }
}