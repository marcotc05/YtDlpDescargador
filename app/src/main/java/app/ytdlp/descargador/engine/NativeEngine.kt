package app.ytdlp.descargador.engine

import android.content.Context
import android.content.SharedPreferences
import app.ytdlp.descargador.BuildConfig
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class EngineInfo(
    val python: String? = null,
    val ytdlp: String? = null,
    val ffmpeg: String? = null,
)

sealed interface UpdateOutcome {
    data class Updated(val from: String?, val to: String) : UpdateOutcome
    data class UpToDate(val version: String) : UpdateOutcome
    data class Failed(val reason: String) : UpdateOutcome
}

/**
 * Acceso directo al Python / yt-dlp / ffmpeg que empaqueta youtubedl-android.
 * Reproduce el entorno que usa la librería (ver YoutubeDL.kt 0.18.x) para poder:
 *  - leer versiones reales,
 *  - validar y actualizar yt-dlp sin depender de la API de GitHub ni de los timeouts de 5 s/10 s de la librería.
 */
object NativeEngine {
    private lateinit var app: Context
    private lateinit var meta: SharedPreferences

    private val _info = MutableStateFlow(EngineInfo())
    val info: StateFlow<EngineInfo> = _info.asStateFlow()

    fun init(context: Context) {
        app = context.applicationContext
        meta = app.getSharedPreferences("engine_meta", Context.MODE_PRIVATE)
    }

    private val baseDir: File get() = File(app.noBackupFilesDir, YoutubeDL.baseName)
    private val ytdlpDir: File get() = File(baseDir, YoutubeDL.ytdlpDirName)
    private val ytdlpFile: File get() = File(ytdlpDir, YoutubeDL.ytdlpBin)
    private val pythonDir: File get() = File(File(baseDir, "packages"), "python")
    private val ffmpegDir: File get() = File(File(baseDir, "packages"), "ffmpeg")
    private val nativeDir: String get() = app.applicationInfo.nativeLibraryDir

    /** ABI de las librerías nativas realmente instaladas (arm64, arm, x86_64…). */
    fun installedAbi(): String = File(nativeDir).name

    private fun environment(): Map<String, String> = mapOf(
        "LD_LIBRARY_PATH" to "${pythonDir.absolutePath}/usr/lib:${ffmpegDir.absolutePath}/usr/lib",
        "SSL_CERT_FILE" to "${pythonDir.absolutePath}/usr/etc/tls/cert.pem",
        "PYTHONHOME" to "${pythonDir.absolutePath}/usr",
        "HOME" to "${pythonDir.absolutePath}/usr",
        "TMPDIR" to app.cacheDir.absolutePath,
        "PATH" to (System.getenv("PATH") ?: "") + ":" + nativeDir,
    )

    /** Ejecuta un comando con el entorno de la librería. Devuelve la salida o null si falla/expira. */
    private fun run(cmd: List<String>, timeoutMs: Long = 20_000): String? = try {
        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        pb.environment().putAll(environment())
        val p = pb.start()
        val out = StringBuilder()
        val reader = thread(isDaemon = true) {
            try {
                p.inputStream.bufferedReader().forEachLine { if (out.length < 8000) out.appendLine(it) }
            } catch (ignored: Exception) {
            }
        }
        if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            p.destroyForcibly()
            null
        } else {
            reader.join(1000)
            if (p.exitValue() == 0) out.toString().trim() else null
        }
    } catch (e: Exception) {
        null
    }

    private fun pythonBin() = "$nativeDir/libpython.so"

    fun refreshInfo() {
        val py = run(listOf(pythonBin(), "--version"))?.lines()?.lastOrNull()
        val yt = run(listOf(pythonBin(), ytdlpFile.absolutePath, "--version"), 40_000)?.lines()?.lastOrNull()
        val ff = run(listOf("$nativeDir/libffmpeg.so", "-version"))?.lines()?.firstOrNull()
        _info.value = EngineInfo(py, yt, ff)
    }

    // ---------- Versión empaquetada vs. instalada ----------

    /**
     * La librería solo copia su yt-dlp embebido si no existe uno. Si no hacemos esto, al instalar un APK
     * nuevo sobre uno viejo seguiría usándose el yt-dlp antiguo guardado en noBackupFilesDir.
     */
    fun purgeIfBuildChanged() {
        val stamp = "${BuildConfig.YTDLP_LIB}-${BuildConfig.VERSION_CODE}"
        if (meta.getString("build_stamp", null) != stamp) {
            AppLog.i("engine", "Build nuevo ($stamp): se descarta el yt-dlp guardado para usar el incluido")
            ytdlpDir.deleteRecursively()
            meta.edit().putString("build_stamp", stamp).putLong("last_check", 0L).apply()
        }
    }

    fun updateDue(): Boolean {
        val last = meta.getLong("last_check", 0L)
        return last == 0L || System.currentTimeMillis() - last > TimeUnit.HOURS.toMillis(24)
    }

    private fun markChecked() {
        meta.edit().putLong("last_check", System.currentTimeMillis()).apply()
    }

    /** Vuelve al yt-dlp que viene dentro del APK. */
    fun resetToBundled() {
        ytdlpDir.deleteRecursively()
        YoutubeDL.init_ytdlp(app, ytdlpDir)
        meta.edit().putLong("last_check", 0L).apply()
        AppLog.i("engine", "yt-dlp restablecido a la versión incluida")
        refreshInfo()
    }

    // ---------- Actualización ----------

    private fun urlFor(channel: UpdateChannel) = when (channel) {
        UpdateChannel.STABLE -> "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
        UpdateChannel.NIGHTLY -> "https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp"
    }

    /** Bloqueante: llamar desde un hilo de fondo. No usar mientras haya descargas en curso. */
    fun update(channel: UpdateChannel): UpdateOutcome {
        val tmp = File(app.cacheDir, "yt-dlp.new")
        return try {
            val before = _info.value.ytdlp ?: run(listOf(pythonBin(), ytdlpFile.absolutePath, "--version"), 40_000)?.lines()?.lastOrNull()
            download(urlFor(channel), tmp)
            if (!looksLikeYtdlp(tmp)) return failed("El archivo descargado no parece un yt-dlp válido")

            val newVersion = run(listOf(pythonBin(), tmp.absolutePath, "--version"), 40_000)?.lines()?.lastOrNull()
            if (newVersion != null && !Regex("""\d{4}\.\d{2}\.\d{2}.*""").matches(newVersion)) {
                return failed("La versión descargada no se pudo validar ($newVersion)")
            }
            if (newVersion != null && newVersion == before) {
                markChecked()
                refreshInfo()
                return UpdateOutcome.UpToDate(newVersion)
            }

            ytdlpDir.mkdirs()
            val staged = File(ytdlpDir, "yt-dlp.staged")
            tmp.copyTo(staged, overwrite = true)
            val backup = File(ytdlpDir, "yt-dlp.bak")
            if (ytdlpFile.exists()) ytdlpFile.copyTo(backup, overwrite = true)
            ytdlpFile.delete()
            if (!staged.renameTo(ytdlpFile)) {
                if (backup.exists()) backup.copyTo(ytdlpFile, overwrite = true)
                return failed("No se pudo instalar el yt-dlp nuevo")
            }
            backup.delete()
            markChecked()
            refreshInfo()
            AppLog.i("engine", "yt-dlp actualizado", mapOf("from" to before, "to" to (newVersion ?: "?"), "channel" to channel.name))
            UpdateOutcome.Updated(before, newVersion ?: "?")
        } catch (e: Exception) {
            failed(e.message ?: e.javaClass.simpleName, e)
        } finally {
            tmp.delete()
        }
    }

    private fun failed(reason: String, error: Throwable? = null): UpdateOutcome.Failed {
        AppLog.e("engine", "Falló la actualización de yt-dlp: $reason", error = error)
        return UpdateOutcome.Failed(reason)
    }

    private fun download(urlString: String, dest: File) {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "YtDlpDescargador/${BuildConfig.VERSION_NAME}")
            val code = conn.responseCode
            if (code !in 200..299) error("El servidor respondió HTTP $code")
            conn.inputStream.use { input -> dest.outputStream().use { input.copyTo(it, 64 * 1024) } }
        } finally {
            conn.disconnect()
        }
    }

    /** yt-dlp "zipimport": empieza con #! y termina con el fin de directorio central de un zip. */
    private fun looksLikeYtdlp(f: File): Boolean {
        if (f.length() < 1_000_000) return false
        RandomAccessFile(f, "r").use { raf ->
            val head = ByteArray(2)
            raf.readFully(head)
            if (head[0] != '#'.code.toByte() || head[1] != '!'.code.toByte()) return false
            val tailLen = minOf(raf.length(), 70_000L).toInt()
            val tail = ByteArray(tailLen)
            raf.seek(raf.length() - tailLen)
            raf.readFully(tail)
            for (i in tailLen - 4 downTo 0) {
                if (tail[i] == 0x50.toByte() && tail[i + 1] == 0x4B.toByte() &&
                    tail[i + 2] == 0x05.toByte() && tail[i + 3] == 0x06.toByte()
                ) return true
            }
        }
        return false
    }
}
