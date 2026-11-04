package app.ytdlp.descargador.engine

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/** Fallo ya diagnosticado de una descarga. */
class DownloadFailure(val diagnosis: Diagnosis, cause: Throwable?) : Exception(diagnosis.line, cause)

/**
 * Cola de descargas. Se procesa de una en una (como el .bat) y sigue en segundo plano.
 * Los errores de red se reintentan con backoff exponencial; todo error queda en AppLog con contexto.
 */
object DownloadManager {

    sealed interface Engine {
        data class Loading(val message: String) : Engine
        data object Ready : Engine
        data class Failed(val message: String) : Engine
    }

    sealed interface UpdateState {
        data object Idle : UpdateState
        data object Working : UpdateState
        data class Done(val message: String) : UpdateState
        data class Failed(val message: String) : UpdateState
    }

    private lateinit var app: Application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<Long>(Channel.UNLIMITED)
    private val nextId = AtomicLong(1)
    private val canceled: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val _engine = MutableStateFlow<Engine>(Engine.Loading("Preparando el motor…"))
    val engine: StateFlow<Engine> = _engine.asStateFlow()

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    private val destRegex = Regex("""\[(?:download|ExtractAudio)] Destination: (.+)""")
    private val itemRegex = Regex("""Downloading (?:item|video) (\d+) of (\d+)""")
    private val fragmentSuffix = Regex("""\.f[0-9]+$""")

    fun init(application: Application) {
        app = application
        Storage.init(application)
        NativeEngine.init(application)
        scope.launch { initEngine() }
        scope.launch {
            for (id in queue) {
                try {
                    runJob(id)
                } catch (e: Throwable) {
                    // Si algo inesperado escapa, la cola no debe quedar muerta.
                    AppLog.e("queue", "runJob falló de forma inesperada", mapOf("job" to id.toString()), e)
                    fail(id, ErrorClassifier.classify(e.message ?: e.toString()))
                }
            }
        }
    }

    // ---------- Motor ----------

    private fun initEngine() {
        _engine.value = Engine.Loading("Preparando el motor…")
        try {
            NativeEngine.purgeIfBuildChanged()
            YoutubeDL.init(app)
            FFmpeg.init(app)
            Storage.cleanTemp()
            NativeEngine.refreshInfo()

            val prefs = AppSettings.current
            if (prefs.autoUpdate && NativeEngine.updateDue()) {
                _engine.value = Engine.Loading("Actualizando yt-dlp…")
                runUpdate(prefs.channel)
            }
            _engine.value = Engine.Ready
        } catch (e: Throwable) {
            AppLog.e("engine", "No se pudo iniciar el motor", error = e)
            _engine.value = Engine.Failed(e.message ?: e.toString())
        }
    }

    /** Bloqueante. Intenta nuestro actualizador y, si falla, el de la librería. */
    private fun runUpdate(channel: UpdateChannel) {
        _update.value = UpdateState.Working
        var outcome = NativeEngine.update(channel)
        if (outcome is UpdateOutcome.Failed) {
            try {
                YoutubeDL.updateYoutubeDL(app)
                NativeEngine.refreshInfo()
                outcome = UpdateOutcome.UpToDate(NativeEngine.info.value.ytdlp ?: "?")
            } catch (e: Throwable) {
                AppLog.e("engine", "También falló el actualizador de la librería", error = e)
            }
        }
        _update.value = when (val o = outcome) {
            is UpdateOutcome.Updated -> UpdateState.Done("yt-dlp actualizado a ${o.to}")
            is UpdateOutcome.UpToDate -> UpdateState.Done("Ya tienes la última versión (${o.version})")
            is UpdateOutcome.Failed -> UpdateState.Failed("No se pudo actualizar: ${o.reason}")
        }
    }

    /** Opción [7] del menú. */
    fun updateYtDlp() {
        if (_update.value is UpdateState.Working) return
        if (_jobs.value.any { it.isActive }) {
            _update.value = UpdateState.Failed("Hay descargas en curso; espera a que terminen.")
            return
        }
        scope.launch { runUpdate(AppSettings.current.channel) }
    }

    fun resetEngine() {
        if (_jobs.value.any { it.isActive }) {
            _update.value = UpdateState.Failed("Hay descargas en curso; espera a que terminen.")
            return
        }
        scope.launch {
            _update.value = UpdateState.Working
            _update.value = try {
                NativeEngine.resetToBundled()
                UpdateState.Done("Se restauró la versión incluida (${NativeEngine.info.value.ytdlp ?: "?"})")
            } catch (e: Throwable) {
                AppLog.e("engine", "No se pudo restablecer yt-dlp", error = e)
                UpdateState.Failed("No se pudo restablecer: ${e.message}")
            }
        }
    }

    /** Opciones de red/robustez comunes a descargas y consulta de formatos. */
    private fun addCommonOptions(request: YoutubeDLRequest, prefs: AppPrefs) {
        request.addOption("--socket-timeout", prefs.timeoutSec)
        if (prefs.forceIpv4) request.addOption("--force-ipv4")
        if (Storage.hasCookies.value) request.addOption("--cookies", Storage.cookiesPath())
        if (prefs.extraArgs.isNotBlank()) request.addCommands(ArgsTokenizer.split(prefs.extraArgs))
    }

    /** Opción [4] del menú: yt-dlp -F */
    suspend fun listFormats(url: String): String = withContext(Dispatchers.IO) {
        val prefs = AppSettings.current
        val request = YoutubeDLRequest(url)
        request.addOption("--no-playlist")
        request.addOption("-F")
        addCommonOptions(request, prefs)
        try {
            YoutubeDL.execute(request, null, false, null).out
        } catch (e: Throwable) {
            val dx = ErrorClassifier.classify(e.message)
            AppLog.e("formats", dx.kind.title, mapOf("url" to url, "stderr" to e.message), e)
            throw DownloadFailure(dx, e)
        }
    }

    // ---------- Cola ----------

    fun enqueue(url: String, label: String, options: List<List<String>>, playlist: Boolean) {
        val job = DownloadJob(
            id = nextId.getAndIncrement(),
            url = url,
            label = label,
            options = options,
            playlist = playlist,
        )
        _jobs.update { it + job }
        queue.trySend(job.id)
        startService()
    }

    fun cancel(id: Long) {
        val job = find(id) ?: return
        when (job.status) {
            Status.QUEUED -> patch(id) { it.copy(status = Status.CANCELED, detail = "Cancelada") }
            Status.RUNNING -> {
                canceled.add(id)
                YoutubeDL.destroyProcessById("job$id")
            }
            else -> Unit
        }
    }

    fun retry(id: Long) {
        val job = find(id) ?: return
        if (job.isActive) return
        canceled.remove(id)
        patch(id) {
            it.copy(
                status = Status.QUEUED, progress = -1f, etaSeconds = -1, detail = null, savedCount = 0,
                openUri = null, errorTitle = null, hint = null, attempt = 0,
            )
        }
        queue.trySend(id)
        startService()
    }

    fun remove(id: Long) {
        _jobs.update { list -> list.filterNot { it.id == id && !it.isActive } }
    }

    fun clearFinished() {
        _jobs.update { list -> list.filter { it.isActive } }
    }

    /** Cancela todo, borra temporales y caché. Devuelve los bytes liberados. */
    fun clearCacheAndReset(): Long {
        _jobs.value.filter { it.isActive }.forEach { cancel(it.id) }
        clearFinished()
        val running = _jobs.value.any { it.isRunning }
        var freed = 0L
        if (!running) {
            freed += Storage.cleanTemp()
            freed += app.cacheDir.walkBottomUp().filter { it.isFile }.sumOf { f -> val n = f.length(); if (f.delete()) n else 0L }
        }
        AppLog.i("maintenance", "Caché limpiada y cola reiniciada", mapOf("freedBytes" to freed.toString()))
        return freed
    }

    // ---------- Ejecución ----------

    private fun backoffMs(attempt: Int): Long =
        (2_000L shl (attempt - 1).coerceIn(0, 5)).coerceAtMost(60_000L) + Random.nextLong(0, 1_000)

    private suspend fun waitCancellable(id: Long, ms: Long): Boolean {
        var left = ms
        while (left > 0) {
            if (id in canceled) return false
            delay(250)
            left -= 250
        }
        return id !in canceled
    }

    private suspend fun runJob(id: Long) {
        val job = find(id) ?: return
        if (job.status != Status.QUEUED) return

        _engine.first { it !is Engine.Loading }
        val engineState = _engine.value
        if (engineState is Engine.Failed) {
            fail(id, Diagnosis(ErrorKind.ENGINE, engineState.message.take(260)))
            return
        }

        val prefs = AppSettings.current
        val maxAttempts = prefs.retries + 1
        canceled.remove(id)
        patch(id) {
            it.copy(
                status = Status.RUNNING, progress = -1f, detail = "Preparando…", attempt = 1,
                maxAttempts = maxAttempts, errorTitle = null, hint = null,
            )
        }

        val tmp = Storage.newTempDir(id)
        try {
            if (Storage.freeBytes() < 300L * 1024 * 1024) {
                AppLog.w("download", "Poco espacio libre", mapOf("freeBytes" to Storage.freeBytes().toString()))
            }

            var attempt = 1
            while (true) {
                try {
                    executeOnce(job, prefs, tmp, attempt, maxAttempts)
                    break
                } catch (e: YoutubeDL.CanceledException) {
                    throw e
                } catch (e: Throwable) {
                    if (id in canceled) throw YoutubeDL.CanceledException()
                    val dx = ErrorClassifier.classify(e.message)
                    if (!dx.kind.retryable || attempt >= maxAttempts) throw DownloadFailure(dx, e)
                    val wait = backoffMs(attempt)
                    patch(id) {
                        it.copy(
                            detail = "Reintentando (${attempt}/${maxAttempts - 1}) en ${wait / 1000} s…",
                            attempt = attempt + 1, progress = -1f,
                        )
                    }
                    if (!waitCancellable(id, wait)) throw YoutubeDL.CanceledException()
                    attempt++
                }
            }

            patch(id) {
                it.copy(status = Status.SAVING, progress = 100f, detail = "Guardando en ${Storage.label.value}…")
            }
            val saved = try {
                Storage.saveAll(tmp)
            } catch (e: Throwable) {
                throw DownloadFailure(Diagnosis(ErrorKind.STORAGE, (e.message ?: "Error al guardar").take(260)), e)
            }
            if (saved.isEmpty()) {
                throw DownloadFailure(Diagnosis(ErrorKind.UNKNOWN, "yt-dlp terminó, pero no generó ningún archivo"), null)
            }
            patch(id) {
                it.copy(
                    status = Status.DONE, progress = 100f, detail = null, savedCount = saved.size,
                    openUri = saved.first(),
                )
            }
            AppLog.i("download", "Descarga completada", jobFields(job, attempt = attempt, maxAttempts = maxAttempts))
            find(id)?.let { Notifier.finished(app, it) }
        } catch (e: YoutubeDL.CanceledException) {
            patch(id) { it.copy(status = Status.CANCELED, detail = "Cancelada") }
        } catch (e: DownloadFailure) {
            fail(id, e.diagnosis)
        } catch (e: Throwable) {
            AppLog.e("download", "Error inesperado", jobFields(job), e)
            fail(id, ErrorClassifier.classify(e.message ?: e.toString()))
        } finally {
            canceled.remove(id)
            tmp.deleteRecursively()
        }
    }

    private fun executeOnce(job: DownloadJob, prefs: AppPrefs, tmp: File, attempt: Int, maxAttempts: Int) {
        val id = job.id
        val request = YoutubeDLRequest(job.url)
        job.options.forEach { o ->
            val flag = o[0]
            when {
                o.size == 1 -> request.addOption(flag)
                flag == "--merge-output-format" -> request.addOption(flag, prefs.container)
                else -> request.addOption(flag, o[1])
            }
        }
        addCommonOptions(request, prefs)
        val template = if (job.playlist) {
            "%(playlist)s/%(playlist_index)s - %(title)s.%(ext)s"
        } else {
            "%(title)s.%(ext)s"
        }
        request.addOption("-o", File(tmp, template).absolutePath)

        val tail = ArrayDeque<String>()
        var lastTick = 0L
        try {
            YoutubeDL.execute(request, "job$id", false) { progress, eta, line ->
                if (line.isNotBlank()) {
                    if (tail.size >= 25) tail.removeFirst()
                    tail.addLast(line)
                }
                var title: String? = null
                var detail: String? = null

                destRegex.find(line)?.let { title = titleFrom(it.groupValues[1]) }
                itemRegex.find(line)?.let {
                    detail = "Elemento ${it.groupValues[1]} de ${it.groupValues[2]}"
                }
                if (line.contains("[Merger]")) detail = "Uniendo audio y video…"
                if (line.contains("[ExtractAudio]")) detail = "Convirtiendo audio…"

                val hasProgress = progress >= 0f
                val now = SystemClock.elapsedRealtime()
                if (title != null || detail != null || (hasProgress && now - lastTick > 200)) {
                    if (hasProgress) lastTick = now
                    patch(id) { cur ->
                        cur.copy(
                            title = title ?: cur.title,
                            detail = detail ?: cur.detail,
                            progress = if (hasProgress) progress else cur.progress,
                            etaSeconds = if (hasProgress) eta else cur.etaSeconds,
                        )
                    }
                }
            }
        } catch (e: YoutubeDL.CanceledException) {
            throw e
        } catch (e: Throwable) {
            val dx = ErrorClassifier.classify(e.message)
            AppLog.e(
                "download",
                "${dx.kind.title} (intento $attempt/$maxAttempts)",
                jobFields(job, attempt, maxAttempts) + mapOf(
                    "kind" to dx.kind.name,
                    "stderr" to e.message,
                    "stdout_tail" to tail.joinToString("\n"),
                ),
                e,
            )
            throw e
        }
    }

    private fun jobFields(job: DownloadJob, attempt: Int = 0, maxAttempts: Int = 0): Map<String, String?> {
        val p = AppSettings.current
        return mapOf(
            "url" to job.url,
            "label" to job.label,
            "options" to job.options.joinToString(" ") { it.joinToString(" ") },
            "attempt" to "$attempt/$maxAttempts",
            "container" to p.container,
            "timeout_s" to p.timeoutSec.toString(),
            "force_ipv4" to p.forceIpv4.toString(),
            "cookies" to Storage.hasCookies.value.toString(),
            "extra_args" to p.extraArgs.ifBlank { null },
            "yt_dlp" to NativeEngine.info.value.ytdlp,
            "network" to networkType(),
        )
    }

    fun networkType(): String = try {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        when {
            caps == null -> "sin conexión"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "datos móviles"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "otra"
        }
    } catch (ignored: Throwable) {
        "desconocida"
    }

    private fun fail(id: Long, dx: Diagnosis) {
        patch(id) {
            it.copy(status = Status.ERROR, detail = dx.line, errorTitle = dx.kind.title, hint = dx.kind.hint)
        }
        find(id)?.let {
            AppLog.e("download", "Descarga fallida: ${dx.kind.title}", jobFields(it) + mapOf("kind" to dx.kind.name, "detail" to dx.line))
            Notifier.finished(app, it)
        }
    }

    // ---------- Utilidades ----------

    private fun find(id: Long): DownloadJob? = _jobs.value.firstOrNull { it.id == id }

    private fun patch(id: Long, change: (DownloadJob) -> DownloadJob) {
        _jobs.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    private fun titleFrom(path: String): String =
        File(path.trim()).nameWithoutExtension.replace(fragmentSuffix, "")

    private fun startService() {
        try {
            ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
        } catch (ignored: Throwable) {
            AppLog.w("service", "No se pudo iniciar el servicio en primer plano", error = ignored)
        }
    }
}
