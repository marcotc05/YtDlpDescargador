package app.ytdlp.descargador.engine

import android.app.Application
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * Cola de descargas. Se procesa de una en una, igual que en el .bat, pero la persona
 * puede agregar varias y seguir usando el teléfono mientras tanto.
 */
object DownloadManager {

    sealed interface Engine {
        data object Loading : Engine
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

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val _engine = MutableStateFlow<Engine>(Engine.Loading)
    val engine: StateFlow<Engine> = _engine.asStateFlow()

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    private val _version = MutableStateFlow("…")
    val ytdlpVersion: StateFlow<String> = _version.asStateFlow()

    private val destRegex = Regex("""\[(?:download|ExtractAudio)] Destination: (.+)""")
    private val itemRegex = Regex("""Downloading (?:item|video) (\d+) of (\d+)""")
    private val fragmentSuffix = Regex("""\.f[0-9]+$""")

    fun init(application: Application) {
        app = application
        Storage.init(application)
        scope.launch { initEngine() }
        scope.launch {
            for (id in queue) runJob(id)
        }
    }

    // ---------- Motor ----------

    private fun initEngine() {
        _engine.value = Engine.Loading
        try {
            YoutubeDL.init(app)
            FFmpeg.init(app)
            Storage.cleanTemp()
            refreshVersion()
            _engine.value = Engine.Ready
        } catch (e: Throwable) {
            _engine.value = Engine.Failed(e.message ?: e.toString())
        }
    }

    private fun refreshVersion() {
        _version.value = try {
            YoutubeDL.versionName(app) ?: YoutubeDL.version(app) ?: "incluida"
        } catch (ignored: Throwable) {
            "incluida"
        }
    }

    /** Equivale a la opción [7] del menú: yt-dlp -U */
    fun updateYtDlp() {
        if (_update.value is UpdateState.Working) return
        scope.launch {
            _update.value = UpdateState.Working
            _update.value = try {
                val status = YoutubeDL.updateYoutubeDL(app)
                refreshVersion()
                when (status) {
                    YoutubeDL.UpdateStatus.DONE -> UpdateState.Done("yt-dlp se actualizó")
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> UpdateState.Done("Ya tienes la última versión")
                    else -> UpdateState.Done("Sin cambios")
                }
            } catch (e: Throwable) {
                UpdateState.Failed(friendly(e.message))
            }
        }
    }

    /** Equivale a la opción [4] del menú: yt-dlp -F */
    suspend fun listFormats(url: String): String = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url)
        request.addOption("--no-playlist")
        request.addOption("-F")
        YoutubeDL.execute(request).out
    }

    // ---------- Cola ----------

    fun enqueue(
        url: String,
        label: String,
        options: List<List<String>>,
        playlist: Boolean,
    ) {
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
            Status.RUNNING -> YoutubeDL.destroyProcessById("job$id")
            else -> Unit
        }
    }

    fun retry(id: Long) {
        val job = find(id) ?: return
        if (job.isActive) return
        patch(id) {
            it.copy(
                status = Status.QUEUED, progress = -1f, etaSeconds = -1,
                detail = null, savedCount = 0, openUri = null,
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

    // ---------- Ejecución ----------

    private suspend fun runJob(id: Long) {
        val job = find(id) ?: return
        if (job.status != Status.QUEUED) return

        _engine.first { it !is Engine.Loading }
        val engineState = _engine.value
        if (engineState is Engine.Failed) {
            fail(id, "El motor de descarga no pudo iniciar: ${engineState.message}")
            return
        }

        patch(id) { it.copy(status = Status.RUNNING, progress = -1f, detail = "Preparando…") }
        val tmp = Storage.newTempDir(id)
        try {
            val request = YoutubeDLRequest(job.url)
            job.options.forEach { o ->
                if (o.size == 1) request.addOption(o[0]) else request.addOption(o[0], o[1])
            }
            val template = if (job.playlist) {
                "%(playlist)s/%(playlist_index)s - %(title)s.%(ext)s"
            } else {
                "%(title)s.%(ext)s"
            }
            request.addOption("-o", File(tmp, template).absolutePath)

            var lastTick = 0L
            YoutubeDL.execute(request, "job$id") { progress, eta, line ->
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

            patch(id) {
                it.copy(status = Status.SAVING, progress = 100f, detail = "Guardando en ${Storage.label.value}…")
            }
            val saved = Storage.saveAll(tmp)
            if (saved.isEmpty()) {
                fail(id, "yt-dlp terminó, pero no generó ningún archivo")
                return
            }
            patch(id) {
                it.copy(
                    status = Status.DONE, progress = 100f, detail = null,
                    savedCount = saved.size, openUri = saved.first(),
                )
            }
            find(id)?.let { Notifier.finished(app, it) }
        } catch (ignored: YoutubeDL.CanceledException) {
            patch(id) { it.copy(status = Status.CANCELED, detail = "Cancelada") }
        } catch (e: Throwable) {
            fail(id, friendly(e.message))
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun fail(id: Long, message: String) {
        patch(id) { it.copy(status = Status.ERROR, detail = message) }
        find(id)?.let { Notifier.finished(app, it) }
    }

    // ---------- Utilidades ----------

    private fun find(id: Long): DownloadJob? = _jobs.value.firstOrNull { it.id == id }

    private fun patch(id: Long, change: (DownloadJob) -> DownloadJob) {
        _jobs.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    private fun titleFrom(path: String): String =
        File(path.trim()).nameWithoutExtension.replace(fragmentSuffix, "")

    private fun friendly(raw: String?): String {
        val lines = raw.orEmpty().lines().filter { it.isNotBlank() }
        val line = lines.lastOrNull { it.contains("ERROR") } ?: lines.lastOrNull() ?: "Error desconocido"
        return line.substringAfter("ERROR:", line).trim().take(240)
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
        } catch (ignored: Throwable) {
            // Si el sistema no permite el servicio, la descarga sigue mientras la app esté abierta.
        }
    }
}
