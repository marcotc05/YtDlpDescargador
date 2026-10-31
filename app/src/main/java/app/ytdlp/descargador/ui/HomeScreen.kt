package app.ytdlp.descargador.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytdlp.descargador.engine.DownloadManager
import app.ytdlp.descargador.engine.FormatRow
import app.ytdlp.descargador.engine.Presets

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel, onQueued: () -> Unit) {
    val engine by DownloadManager.engine.collectAsStateWithLifecycle()
    val ready = engine is DownloadManager.Engine.Ready
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Descargador", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Pega un enlace y elige qué bajar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            EngineBanner(engine)

            OutlinedTextField(
                value = vm.url,
                onValueChange = { vm.url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enlace del video o la playlist") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                trailingIcon = {
                    if (vm.url.isEmpty()) {
                        IconButton(onClick = {
                            clipboard.getText()?.text?.let { vm.url = it.trim() }
                        }) { Icon(Icons.Rounded.ContentPaste, contentDescription = "Pegar enlace") }
                    } else {
                        IconButton(onClick = { vm.url = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Borrar enlace")
                        }
                    }
                },
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Mode.entries.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = vm.mode == m,
                        onClick = { vm.mode = m },
                        shape = SegmentedButtonDefaults.itemShape(index, Mode.entries.size),
                        icon = {},
                        label = { Text(m.label, maxLines = 1) },
                    )
                }
            }

            when (vm.mode) {
                Mode.VIDEO -> Section("Calidad") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Presets.videoQualities.forEachIndexed { i, q ->
                            FilterChip(
                                selected = vm.videoQuality == i,
                                onClick = { vm.videoQuality = i },
                                label = { Text(q.label) },
                            )
                        }
                    }
                    Hint("Se guarda en MP4. Si el enlace es de una lista, solo baja ese video.")
                }

                Mode.AUDIO -> Section("Formato de audio") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Presets.audioFormats.forEachIndexed { i, a ->
                            FilterChip(
                                selected = vm.audioFormat == i,
                                onClick = { vm.audioFormat = i },
                                label = { Text(a.label) },
                            )
                        }
                    }
                    Presets.audioFormats[vm.audioFormat].hint?.let { Hint(it) }
                }

                Mode.PLAYLIST -> Section("Qué bajar de la lista") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = !vm.playlistAudio,
                            onClick = { vm.playlistAudio = false },
                            label = { Text("Video (MP4)") },
                        )
                        FilterChip(
                            selected = vm.playlistAudio,
                            onClick = { vm.playlistAudio = true },
                            label = { Text("Audio (MP3)") },
                        )
                    }
                    Hint("Baja toda la lista y la guarda en una carpeta con su nombre.")
                }

                Mode.CUSTOM -> Section("Formato personalizado") {
                    OutlinedTextField(
                        value = vm.formatCode,
                        onValueChange = { vm.formatCode = it.filter { c -> !c.isWhitespace() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código de formato") },
                        placeholder = { Text("137+140") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    )
                    OutlinedButton(
                        onClick = { focus.clearFocus(); vm.loadFormats() },
                        enabled = ready && vm.url.isNotBlank() && vm.formats != FormatsState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ver formatos disponibles")
                    }
                    FormatsPanel(vm)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = {
                    focus.clearFocus()
                    vm.submit()
                    onQueued()
                },
                enabled = vm.canSubmit && ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    when (engine) {
                        is DownloadManager.Engine.Ready -> "Descargar"
                        is DownloadManager.Engine.Loading -> "Preparando…"
                        is DownloadManager.Engine.Failed -> "Motor no disponible"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EngineBanner(engine: DownloadManager.Engine) {
    when (engine) {
        is DownloadManager.Engine.Ready -> Unit
        is DownloadManager.Engine.Loading -> Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                Text(
                    "Preparando el motor de descarga. Solo tarda la primera vez.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        is DownloadManager.Engine.Failed -> Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "No se pudo iniciar el motor de descarga",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    engine.message.take(300),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun FormatsPanel(vm: HomeViewModel) {
    when (val state = vm.formats) {
        is FormatsState.Idle -> Hint("Toca los formatos para combinarlos, por ejemplo video y audio: 137+140.")
        is FormatsState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
            Text("Consultando formatos…", style = MaterialTheme.typography.bodyMedium)
        }

        is FormatsState.Error -> Text(
            state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )

        is FormatsState.Loaded -> {
            if (state.rows.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Text(
                        state.raw.ifBlank { "No se encontraron formatos." },
                        modifier = Modifier
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            } else {
                val chosen = vm.formatCode.split('+').map { it.trim() }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        state.rows.forEach { row ->
                            FormatItem(row, selected = row.id in chosen, onClick = { vm.toggleFormat(row.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatItem(row: FormatRow, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .widthIn(min = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) scheme.primary else scheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                row.id,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(row.resolution ?: row.kind, style = MaterialTheme.typography.titleSmall)
            val detail = listOfNotNull(
                if (row.resolution != null) row.kind else null,
                row.ext,
                row.size,
            ).joinToString(", ")
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Elegido", tint = scheme.primary)
    }
}
