package app.ytdlp.descargador.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StatFs
import androidx.core.content.FileProvider
import app.ytdlp.descargador.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Genera un reporte de diagnóstico en texto y lo comparte con el selector de Android. */
object LogExporter {

    fun buildReport(context: Context): String = buildString {
        val now = Date()
        val local = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(now)
        val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(now)
        val info = NativeEngine.info.value
        val prefs = AppSettings.current

        appendLine("=== YT-DLP Descargador: reporte de diagnóstico ===")
        appendLine("Generado: $local ($utc)")
        appendLine()
        appendLine("--- App ---")
        appendLine("Versión: ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})")
        appendLine("Librería youtubedl-android: ${BuildConfig.YTDLP_LIB}")
        appendLine()
        appendLine("--- Motor ---")
        appendLine("yt-dlp: ${info.ytdlp ?: "desconocido"}")
        appendLine("Python: ${info.python ?: "desconocido"}")
        appendLine("ffmpeg: ${info.ffmpeg ?: "desconocido"}")
        appendLine("ABI instalada (librerías nativas): ${NativeEngine.installedAbi()}")
        appendLine()
        appendLine("--- Dispositivo ---")
        appendLine("Modelo: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("ABIs soportadas: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        appendLine("Red actual: ${DownloadManager.networkType()}")
        appendLine("Idioma: ${Locale.getDefault()}")
        try {
            val st = StatFs(context.filesDir.absolutePath)
            appendLine("Almacenamiento libre: ${st.availableBytes / (1024 * 1024)} MB de ${st.totalBytes / (1024 * 1024)} MB")
        } catch (ignored: Exception) {
        }
        appendLine()
        appendLine("--- Ajustes ---")
        appendLine("Contenedor: ${prefs.container} | Reintentos: ${prefs.retries} | Timeout: ${prefs.timeoutSec}s | Forzar IPv4: ${prefs.forceIpv4}")
        appendLine("Canal yt-dlp: ${prefs.channel} | Actualización automática: ${prefs.autoUpdate}")
        appendLine("Carpeta de destino: ${Storage.label.value}")
        appendLine("Cookies importadas: ${Storage.hasCookies.value} (el contenido nunca se incluye)")
        appendLine("Argumentos extra: ${prefs.extraArgs.ifBlank { "(ninguno)" }}")
        appendLine()

        appendLine("--- Descargas de esta sesión ---")
        val jobs = DownloadManager.jobs.value
        if (jobs.isEmpty()) appendLine("(ninguna)")
        jobs.forEach { j ->
            appendLine("#${j.id} [${j.status}] ${j.label} | ${j.url}")
            if (j.status == Status.ERROR) appendLine("    ${j.errorTitle}: ${j.detail}")
        }
        appendLine()

        appendLine("--- Últimos eventos (más antiguo → más reciente) ---")
        val entries = AppLog.entries(60)
        if (entries.isEmpty()) appendLine("(sin eventos registrados)")
        entries.forEach { append(AppLog.format(it)) }
    }

    /** Escribe el reporte en caché y abre el selector para compartirlo. */
    fun share(context: Context): Boolean = try {
        val dir = File(context.cacheDir, "logs").also { it.mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "ytdlp-descargador-log-$stamp.txt")
        file.writeText(buildReport(context))

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "YT-DLP Descargador: log de errores")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartir log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        AppLog.e("export", "No se pudo exportar el log", error = e)
        false
    }
}
