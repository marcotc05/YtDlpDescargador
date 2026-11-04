package app.ytdlp.descargador.engine

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import app.ytdlp.descargador.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Mantiene viva la app mientras hay descargas activas, aunque la persona cambie de app
 * o apague la pantalla. Se detiene sola cuando la cola queda vacía.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.ensureChannels(this)
        ServiceCompat.startForeground(
            this,
            Notifier.ID_SERVICE,
            Notifier.progress(this, DownloadManager.jobs.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        watcher?.cancel()
        watcher = scope.launch {
            DownloadManager.jobs.collect { jobs ->
                if (jobs.none { it.isActive }) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    NotificationManagerCompat.from(this@DownloadService)
                        .notify(Notifier.ID_SERVICE, Notifier.progress(this@DownloadService, jobs))
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

object Notifier {
    private const val CH_PROGRESS = "progress"
    private const val CH_DONE = "done"
    const val ID_SERVICE = 1

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Descargas en curso", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Descargas terminadas", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun progress(ctx: Context, jobs: List<DownloadJob>): Notification {
        val running = jobs.firstOrNull { it.isRunning }
        val waiting = jobs.count { it.status == Status.QUEUED }
        val pct = running?.progress ?: -1f

        val title = if (running != null) "Descargando" else "En espera"
        val body = buildString {
            append(running?.let { it.title ?: it.label } ?: "Preparando la cola")
            if (waiting > 0) append("  (+$waiting en cola)")
        }
        return NotificationCompat.Builder(ctx, CH_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, pct.toInt().coerceIn(0, 100), pct < 0f)
            .setContentIntent(openApp(ctx))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun finished(ctx: Context, job: DownloadJob) {
        val ok = job.status == Status.DONE
        val notification = NotificationCompat.Builder(ctx, CH_DONE)
            .setSmallIcon(
                if (ok) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error
            )
            .setContentTitle(if (ok) "Descarga lista" else "No se pudo descargar")
            .setContentText(if (ok) (job.title ?: job.label) else (job.errorTitle ?: job.detail ?: job.label))
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1000 + job.id.toInt(), notification)
        } catch (ignored: Exception) {
        }
    }
}
