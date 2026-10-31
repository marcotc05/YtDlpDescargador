package app.ytdlp.descargador.engine

data class FormatRow(
    val id: String,
    val ext: String,
    val kind: String,
    val resolution: String?,
    val size: String?,
)

/** Convierte la tabla de `yt-dlp -F` en filas que se pueden tocar. */
object FormatParser {
    private val sizeRegex = Regex("~?\\d+(?:\\.\\d+)?[KMG]iB")
    private val spaces = Regex("\\s+")

    fun parse(output: String): List<FormatRow> {
        val lines = output.lines()
        val start = lines.indexOfFirst { line ->
            val t = line.trim()
            t.length > 10 && t.all { it == '-' || it == '─' || it == '━' || it == ' ' }
        }
        if (start < 0) return emptyList()

        return lines.drop(start + 1).mapNotNull { line ->
            val t = line.trim()
            if (t.isEmpty()) return@mapNotNull null
            val tok = t.split(spaces)
            if (tok.size < 2) return@mapNotNull null
            val id = tok[0]
            val ext = tok[1]
            // Las miniaturas (storyboards) no sirven para descargar.
            if (ext == "mhtml" || id.startsWith("sb")) return@mapNotNull null

            val audioOnly = t.contains("audio only")
            val videoOnly = t.contains("video only")
            val resolution = if (audioOnly) null
            else tok.getOrNull(2)?.takeIf { it.contains('x') || it.endsWith("p") }
            val kind = when {
                audioOnly -> "Solo audio"
                videoOnly -> "Solo video"
                else -> "Video con audio"
            }
            FormatRow(
                id = id,
                ext = ext,
                kind = kind,
                resolution = resolution,
                size = sizeRegex.find(t)?.value,
            )
        }
    }
}
