package app.ytdlp.descargador.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ytdlp.descargador.engine.DownloadManager
import app.ytdlp.descargador.engine.FormatParser
import app.ytdlp.descargador.engine.FormatRow
import app.ytdlp.descargador.engine.Presets
import kotlinx.coroutines.launch

enum class Mode(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    PLAYLIST("Playlist"),
    CUSTOM("Formato"),
}

sealed interface FormatsState {
    data object Idle : FormatsState
    data object Loading : FormatsState
    data class Loaded(val rows: List<FormatRow>, val raw: String) : FormatsState
    data class Error(val message: String) : FormatsState
}

class HomeViewModel : ViewModel() {
    var url by mutableStateOf("")
    var mode by mutableStateOf(Mode.VIDEO)
    var videoQuality by mutableIntStateOf(0)
    var audioFormat by mutableIntStateOf(0)
    var playlistAudio by mutableStateOf(false)
    var formatCode by mutableStateOf("")
    var formats by mutableStateOf<FormatsState>(FormatsState.Idle)
        private set

    val canSubmit: Boolean
        get() = url.isNotBlank() && (mode != Mode.CUSTOM || formatCode.isNotBlank())

    /** Opción [4] del menú: ver formatos disponibles. */
    fun loadFormats() {
        val target = url.trim()
        if (target.isEmpty()) return
        formats = FormatsState.Loading
        viewModelScope.launch {
            formats = try {
                val raw = DownloadManager.listFormats(target)
                FormatsState.Loaded(FormatParser.parse(raw), raw)
            } catch (e: Throwable) {
                FormatsState.Error(
                    e.message?.lines()?.lastOrNull { it.isNotBlank() }?.take(240)
                        ?: "No se pudieron obtener los formatos"
                )
            }
        }
    }

    /** Toca un formato para armar códigos como 137+140. */
    fun toggleFormat(id: String) {
        val parts = formatCode.split('+').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (!parts.remove(id)) parts.add(id)
        formatCode = parts.joinToString("+")
    }

    fun submit() {
        val target = url.trim()
        if (target.isEmpty()) return
        when (mode) {
            Mode.VIDEO -> {
                val q = Presets.videoQualities[videoQuality]
                DownloadManager.enqueue(target, "Video, ${q.label}", Presets.videoOptions(q.format), false)
            }
            Mode.AUDIO -> {
                val a = Presets.audioFormats[audioFormat]
                DownloadManager.enqueue(target, "Audio ${a.label}", Presets.audioOptions(a), false)
            }
            Mode.PLAYLIST -> {
                if (playlistAudio) {
                    DownloadManager.enqueue(target, "Playlist en audio MP3", Presets.playlistAudioOptions(), true)
                } else {
                    DownloadManager.enqueue(target, "Playlist en video MP4", Presets.playlistVideoOptions(), true)
                }
            }
            Mode.CUSTOM -> {
                val code = formatCode.trim()
                DownloadManager.enqueue(target, "Formato $code", Presets.customOptions(code), false)
            }
        }
        url = ""
    }
}
