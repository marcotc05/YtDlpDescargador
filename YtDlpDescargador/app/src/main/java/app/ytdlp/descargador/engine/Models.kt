package app.ytdlp.descargador.engine

import android.net.Uri

enum class Status { QUEUED, RUNNING, SAVING, DONE, ERROR, CANCELED }

/**
 * Una descarga. [options] son los argumentos de yt-dlp: cada elemento es
 * [flag] o [flag, valor]. El destino (-o) se agrega al ejecutar.
 */
data class DownloadJob(
    val id: Long,
    val url: String,
    val label: String,
    val options: List<List<String>>,
    val playlist: Boolean,
    val status: Status = Status.QUEUED,
    val title: String? = null,
    /** 0..100, o -1 si todavía no hay dato. */
    val progress: Float = -1f,
    val etaSeconds: Long = -1,
    val detail: String? = null,
    val savedCount: Int = 0,
    val openUri: Uri? = null,
) {
    val isActive: Boolean
        get() = status == Status.QUEUED || status == Status.RUNNING || status == Status.SAVING
    val isRunning: Boolean
        get() = status == Status.RUNNING || status == Status.SAVING
}

data class Quality(val label: String, val format: String)

data class AudioFormat(
    val label: String,
    val codec: String,
    val bestQuality: Boolean,
    val hint: String? = null,
)

/**
 * Mismos parámetros que usa yt-dlp-menu.bat.
 */
object Presets {

    // Opción [1] del menú VIDEO
    val videoQualities = listOf(
        Quality("Mejor calidad", "bestvideo+bestaudio/best"),
        Quality("1080p", "bestvideo[height<=1080]+bestaudio/best[height<=1080]"),
        Quality("720p", "bestvideo[height<=720]+bestaudio/best[height<=720]"),
        Quality("480p", "bestvideo[height<=480]+bestaudio/best[height<=480]"),
        Quality("360p", "bestvideo[height<=360]+bestaudio/best[height<=360]"),
    )

    // Opción [2] del menú AUDIO
    val audioFormats = listOf(
        AudioFormat("MP3", "mp3", true, "Compatible con todo"),
        AudioFormat("AAC", "aac", true),
        AudioFormat("FLAC", "flac", false, "Máxima calidad, archivo grande"),
        AudioFormat("OGG", "vorbis", false),
        AudioFormat("WAV", "wav", false, "Sin compresión, archivo grande"),
    )

    fun videoOptions(format: String): List<List<String>> = listOf(
        listOf("--no-playlist"),
        listOf("-f", format),
        listOf("--merge-output-format", "mp4"),
    )

    fun audioOptions(a: AudioFormat): List<List<String>> = buildList {
        add(listOf("--no-playlist"))
        add(listOf("-x"))
        add(listOf("--audio-format", a.codec))
        if (a.bestQuality) add(listOf("--audio-quality", "0"))
    }

    // Opción [3]: formato personalizado
    fun customOptions(code: String): List<List<String>> = listOf(
        listOf("--no-playlist"),
        listOf("-f", code),
        listOf("--merge-output-format", "mp4"),
    )

    // Opción [5]: playlist
    fun playlistVideoOptions(): List<List<String>> = listOf(
        listOf("-f", "bestvideo+bestaudio/best"),
        listOf("--merge-output-format", "mp4"),
    )

    fun playlistAudioOptions(): List<List<String>> = listOf(
        listOf("-x"),
        listOf("--audio-format", "mp3"),
        listOf("--audio-quality", "0"),
    )
}
