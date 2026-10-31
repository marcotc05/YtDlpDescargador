package app.ytdlp.descargador.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytdlp.descargador.engine.DownloadJob
import app.ytdlp.descargador.engine.DownloadManager
import app.ytdlp.descargador.engine.Status

@Composable
fun DownloadsScreen(onGoHome: () -> Unit) {
    val jobs by DownloadManager.jobs.collectAsStateWithLifecycle()
    val sorted = remember(jobs) {
        jobs.sortedWith(
            compareBy<DownloadJob>(
                { if (it.isRunning) 0 else if (it.status == Status.QUEUED) 1 else 2 },
                { if (it.isActive) it.id else -it.id },
            )
        )
    }
    val hasFinished = jobs.any { !it.isActive }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Descargas",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f),
            )
            if (hasFinished) {
                TextButton(onClick = { DownloadManager.clearFinished() }) { Text("Limpiar terminadas") }
            }
        }

        if (sorted.isEmpty()) {
            EmptyState(onGoHome)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sorted, key = { it.id }) { job ->
                    if (job.isRunning) HeroCard(job) else JobRow(job)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onGoHome: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Aún no hay descargas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Pega un enlace en la pestaña Descargar y lo verás avanzar aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        FilledTonalButton(onClick = onGoHome) { Text("Pegar un enlace") }
    }
}

/** La descarga activa: la cifra grande es lo primero que se ve al abrir la pestaña. */
@Composable
private fun HeroCard(job: DownloadJob) {
    val target = if (job.progress < 0f) 0f else (job.progress / 100f).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(target, label = "progreso")
    val percent = (fraction * 100f).toInt()

    Surface(shape = RoundedCornerShape(28.dp), color = Brand.Ink) {
        Column(Modifier.padding(22.dp)) {
            Text(job.label, style = MaterialTheme.typography.labelLarge, color = Brand.OnInkMuted)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$percent", style = HeroNumber, color = Brand.OnInk)
                Text(
                    "%",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = Brand.Signal,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                )
                Spacer(Modifier.weight(1f))
                if (job.status == Status.RUNNING && job.etaSeconds >= 0) {
                    Text(
                        "quedan ${formatEta(job.etaSeconds)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Brand.OnInkMuted,
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                job.title ?: job.url,
                style = MaterialTheme.typography.titleMedium,
                color = Brand.OnInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brand.Signal)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    job.detail ?: "Descargando…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.OnInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (job.status == Status.RUNNING) {
                    TextButton(onClick = { DownloadManager.cancel(job.id) }) {
                        Text("Cancelar", color = Brand.Signal)
                    }
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: DownloadJob) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    Surface(shape = RoundedCornerShape(22.dp), color = scheme.surfaceContainerLowest) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(job.status)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        job.title ?: job.url,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        statusLine(job),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                when (job.status) {
                    Status.QUEUED -> RowIcon(Icons.Rounded.Close, "Cancelar") { DownloadManager.cancel(job.id) }
                    Status.DONE -> {
                        val uri = job.openUri
                        if (job.savedCount == 1 && uri != null) {
                            RowIcon(Icons.AutoMirrored.Rounded.OpenInNew, "Abrir") { openFile(context, uri) }
                        }
                        RowIcon(Icons.Rounded.Close, "Quitar de la lista") { DownloadManager.remove(job.id) }
                    }

                    Status.ERROR, Status.CANCELED -> {
                        RowIcon(Icons.Rounded.Refresh, "Reintentar") { DownloadManager.retry(job.id) }
                        RowIcon(Icons.Rounded.Close, "Quitar de la lista") { DownloadManager.remove(job.id) }
                    }

                    else -> Unit
                }
            }
            if (job.status == Status.ERROR && !job.detail.isNullOrBlank()) {
                Text(
                    job.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun RowIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusBadge(status: Status) {
    val scheme = MaterialTheme.colorScheme
    val (icon, bg, fg) = when (status) {
        Status.DONE -> Triple(Icons.Rounded.Check, Brand.Good.copy(alpha = 0.16f), Brand.Good)
        Status.ERROR -> Triple(Icons.Rounded.ErrorOutline, scheme.errorContainer, scheme.error)
        Status.CANCELED -> Triple(Icons.Rounded.Close, scheme.surfaceVariant, scheme.onSurfaceVariant)
        else -> Triple(Icons.Rounded.Schedule, scheme.primaryContainer, scheme.primary)
    }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
    }
}

private fun statusLine(job: DownloadJob): String = when (job.status) {
    Status.QUEUED -> "${job.label}, en espera"
    Status.DONE ->
        if (job.savedCount > 1) "${job.savedCount} archivos guardados" else "${job.label}, guardado"
    Status.ERROR -> "${job.label}, con error"
    Status.CANCELED -> "${job.label}, cancelada"
    else -> job.label
}

private fun formatEta(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun openFile(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No hay una app que pueda abrir este archivo", Toast.LENGTH_SHORT).show()
    }
}
