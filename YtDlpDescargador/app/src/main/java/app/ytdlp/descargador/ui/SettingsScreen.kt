package app.ytdlp.descargador.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytdlp.descargador.engine.DownloadManager
import app.ytdlp.descargador.engine.Storage

@Composable
fun SettingsScreen() {
    val label by Storage.label.collectAsStateWithLifecycle()
    val tree by Storage.treeUri.collectAsStateWithLifecycle()
    val version by DownloadManager.ytdlpVersion.collectAsStateWithLifecycle()
    val update by DownloadManager.update.collectAsStateWithLifecycle()
    val engine by DownloadManager.engine.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) Storage.setCustomFolder(uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Ajustes",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 8.dp),
        )

        Card("Carpeta de descarga") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cada archivo se guarda aquí al terminar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { picker.launch(null) }) { Text("Elegir carpeta") }
                if (tree != null) {
                    OutlinedButton(onClick = { Storage.resetFolder() }) { Text("Usar Descargas") }
                }
            }
        }

        Card("yt-dlp") {
            Text("Versión: $version", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Si un sitio deja de funcionar, actualiza yt-dlp. Necesita conexión a internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val working = update is DownloadManager.UpdateState.Working
            Button(
                onClick = { DownloadManager.updateYtDlp() },
                enabled = engine is DownloadManager.Engine.Ready && !working,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (working) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Buscando actualización…")
                } else {
                    Text("Buscar actualización")
                }
            }
            when (val s = update) {
                is DownloadManager.UpdateState.Done -> Text(
                    s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Brand.Good,
                )

                is DownloadManager.UpdateState.Failed -> Text(
                    s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                else -> Unit
            }
        }

        Card("Acerca de") {
            Text(
                "yt-dlp y ffmpeg vienen dentro de la app, así que no hay que instalar nada más. " +
                    "Las descargas siguen en segundo plano. También puedes compartir un enlace " +
                    "desde otra app y elegir YT-DLP Descargador.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Card(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
