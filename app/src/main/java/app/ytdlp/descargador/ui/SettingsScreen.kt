package app.ytdlp.descargador.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marcotc05.ytdlpdescargador.BuildConfig
import app.ytdlp.descargador.engine.AppLog
import app.ytdlp.descargador.engine.AppPrefs
import app.ytdlp.descargador.engine.AppSettings
import app.ytdlp.descargador.engine.DownloadManager
import app.ytdlp.descargador.engine.LogExporter
import app.ytdlp.descargador.engine.NativeEngine
import app.ytdlp.descargador.engine.Storage
import app.ytdlp.descargador.engine.ThemeMode
import app.ytdlp.descargador.engine.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs by AppSettings.state.collectAsStateWithLifecycle()
    val label by Storage.label.collectAsStateWithLifecycle()
    val tree by Storage.treeUri.collectAsStateWithLifecycle()
    val hasCookies by Storage.hasCookies.collectAsStateWithLifecycle()
    val info by NativeEngine.info.collectAsStateWithLifecycle()
    val update by DownloadManager.update.collectAsStateWithLifecycle()
    val engine by DownloadManager.engine.collectAsStateWithLifecycle()
    val jobs by DownloadManager.jobs.collectAsStateWithLifecycle()

    val busy = jobs.any { it.isActive }
    val engineReady = engine is DownloadManager.Engine.Ready
    val updating = update is DownloadManager.UpdateState.Working

    var logTick by remember { mutableIntStateOf(0) }
    var problemCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(logTick, update, jobs.size) {
        problemCount = withContext(Dispatchers.IO) { AppLog.problemCount() }
    }
    var confirmReset by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) Storage.setCustomFolder(uri)
    }
    val cookiesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = Storage.importCookies(uri)
            Toast.makeText(
                context,
                if (ok) "Cookies importadas" else "No se pudo leer el archivo de cookies",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
        ) {
            // ---------------- Apariencia ----------------
            SettingsCard("Apariencia") {
                Text("Tema", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(ThemeMode.SYSTEM to "Automático", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Oscuro")
                    options.forEachIndexed { i, (mode, text) ->
                        SegmentedButton(
                            selected = prefs.themeMode == mode,
                            onClick = { AppSettings.update { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            icon = {},
                            label = { Text(text, maxLines = 1) },
                        )
                    }
                }

                Text("Color de acento", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Accents.forEachIndexed { i, a ->
                        val selected = prefs.accent == i
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(a.light)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { AppSettings.update { it.copy(accent = i) } }
                                .semantics { contentDescription = "Acento ${a.name}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Text("Tamaño de fuente", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Pequeña" to 0.9f, "Normal" to 1f, "Grande" to 1.15f, "Muy grande" to 1.3f).forEach { (text, v) ->
                        FilterChip(
                            selected = abs(prefs.fontScale - v) < 0.01f,
                            onClick = { AppSettings.update { it.copy(fontScale = v) } },
                            label = { Text(text) },
                        )
                    }
                }
            }

            // ---------------- Descarga ----------------
            SettingsCard("Descarga") {
                Text("Contenedor de video", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPrefs.CONTAINERS.forEach { c ->
                        FilterChip(
                            selected = prefs.container == c,
                            onClick = { AppSettings.update { it.copy(container = c) } },
                            label = { Text(c.uppercase()) },
                        )
                    }
                }
                Hint("Se aplica al unir video y audio. El .bat original usa MP4. Si un video no se une bien en MP4, prueba MKV.")

                Text("Carpeta de descarga", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { folderPicker.launch(null) }) { Text("Elegir carpeta") }
                    if (tree != null) OutlinedButton(onClick = { Storage.resetFolder() }) { Text("Usar Descargas") }
                }

                LabeledSlider(
                    title = "Reintentos automáticos: ${prefs.retries}",
                    value = prefs.retries.toFloat(),
                    range = 0f..8f,
                    steps = 7,
                    onChange = { v -> AppSettings.update { it.copy(retries = v.roundToInt()) } },
                )
                Hint("Solo se reintentan errores de red o del servidor, con espera creciente (2, 4, 8… segundos).")

                LabeledSlider(
                    title = "Tiempo de espera de red: ${prefs.timeoutSec} s",
                    value = prefs.timeoutSec.toFloat(),
                    range = 10f..120f,
                    steps = 21,
                    onChange = { v -> AppSettings.update { it.copy(timeoutSec = ((v / 5f).roundToInt() * 5)) } },
                )

                SwitchRow(
                    title = "Forzar IPv4",
                    subtitle = "Útil si con datos móviles falla pero con Wi-Fi no.",
                    checked = prefs.forceIpv4,
                    onChange = { c -> AppSettings.update { it.copy(forceIpv4 = c) } },
                )

                Text("Cookies (opcional)", style = MaterialTheme.typography.titleSmall)
                Hint(
                    if (hasCookies) "Hay un cookies.txt importado y se usa en cada descarga."
                    else "Si YouTube pide confirmar que no eres un robot, importa un cookies.txt exportado de tu navegador."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { cookiesPicker.launch(arrayOf("text/plain", "*/*")) }) { Text("Importar") }
                    if (hasCookies) OutlinedButton(onClick = { Storage.clearCookies() }) { Text("Quitar") }
                }

                OutlinedTextField(
                    value = prefs.extraArgs,
                    onValueChange = { v -> AppSettings.update { it.copy(extraArgs = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Argumentos extra de yt-dlp") },
                    placeholder = { Text("--extractor-args youtube:player_client=web") },
                    supportingText = { Text("Avanzado. Se agregan al final de cada comando.") },
                    shape = RoundedCornerShape(16.dp),
                )
            }

            // ---------------- Motor ----------------
            SettingsCard("Motor de descarga") {
                InfoRow("App", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow("yt-dlp", info.ytdlp ?: "—")
                InfoRow("Python", info.python?.removePrefix("Python ") ?: "—")
                InfoRow("ffmpeg", info.ffmpeg?.removePrefix("ffmpeg version ")?.substringBefore(' ') ?: "—")
                InfoRow("Librería", BuildConfig.YTDLP_LIB)
                InfoRow("ABI instalada", NativeEngine.installedAbi())
                InfoRow("ABI del dispositivo", Build.SUPPORTED_ABIS.firstOrNull() ?: "—")
                Hint("El ABI se fija al instalar el APK y no se puede cambiar desde la app. Si no coincide con el del dispositivo, instala el APK que lleva ese nombre.")

                Text("Canal de actualización", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.channel == UpdateChannel.STABLE,
                        onClick = { AppSettings.update { it.copy(channel = UpdateChannel.STABLE) } },
                        label = { Text("Estable") },
                    )
                    FilterChip(
                        selected = prefs.channel == UpdateChannel.NIGHTLY,
                        onClick = { AppSettings.update { it.copy(channel = UpdateChannel.NIGHTLY) } },
                        label = { Text("Nightly") },
                    )
                }
                Hint("Nightly trae arreglos de YouTube antes que la versión estable.")

                SwitchRow(
                    title = "Actualizar yt-dlp automáticamente",
                    subtitle = "Al abrir la app, como máximo una vez al día.",
                    checked = prefs.autoUpdate,
                    onChange = { c -> AppSettings.update { it.copy(autoUpdate = c) } },
                )

                Button(
                    onClick = { DownloadManager.updateYtDlp() },
                    enabled = engineReady && !updating && !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (updating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(10.dp))
                        Text("Trabajando…")
                    } else {
                        Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Actualizar yt-dlp ahora")
                    }
                }
                OutlinedButton(
                    onClick = { DownloadManager.resetEngine() },
                    enabled = engineReady && !updating && !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Restablecer a la versión incluida")
                }
                if (busy) Hint("Hay descargas en curso: espera a que terminen para actualizar o restablecer.")
                when (val s = update) {
                    is DownloadManager.UpdateState.Done -> Text(s.message, style = MaterialTheme.typography.bodyMedium, color = Brand.Good)
                    is DownloadManager.UpdateState.Failed -> Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    else -> Unit
                }
            }

            // ---------------- Diagnóstico ----------------
            SettingsCard("Mantenimiento y diagnóstico") {
                Button(
                    onClick = {
                        if (!LogExporter.share(context)) {
                            Toast.makeText(context, "No se pudo generar el log", Toast.LENGTH_SHORT).show()
                        }
                        logTick++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Exportar log de errores")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Eventos con error o aviso: $problemCount",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { AppLog.clear(); logTick++ }, enabled = problemCount > 0) { Text("Borrar log") }
                }
                Hint("El reporte incluye versiones, dispositivo, ajustes y los últimos errores. Las cookies nunca se incluyen.")

                OutlinedButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Limpiar caché y reiniciar descargas")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Limpiar y reiniciar?") },
            text = { Text("Se cancelan las descargas en curso, se vacía la lista y se borran los archivos temporales. Los videos ya guardados no se tocan.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    val freed = DownloadManager.clearCacheAndReset()
                    Toast.makeText(context, "Listo: ${freed / (1024 * 1024)} MB liberados", Toast.LENGTH_SHORT).show()
                    logTick++
                }) { Text("Limpiar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(Dimens.Card),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InfoRow(name: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(subtitle)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LabeledSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}
