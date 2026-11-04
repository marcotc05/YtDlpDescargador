package app.ytdlp.descargador.engine

enum class ErrorKind(val title: String, val hint: String?, val retryable: Boolean) {
    JS_RUNTIME(
        "yt-dlp no pudo resolver el desafío de YouTube",
        "Actualiza yt-dlp en Ajustes → Motor. Si ya está al día, exporta el log y compártelo.",
        false,
    ),
    BOT_CHECK(
        "YouTube pide confirmar que no eres un robot",
        "Importa un cookies.txt en Ajustes → Descarga, o prueba con otra red (Wi-Fi o datos).",
        false,
    ),
    FORBIDDEN(
        "El sitio rechazó la petición (HTTP 403)",
        "Actualiza yt-dlp; si sigue, activa 'Forzar IPv4', importa cookies o cambia de red.",
        false,
    ),
    TOO_MANY(
        "Demasiadas solicitudes (HTTP 429)",
        "Espera unos minutos antes de reintentar.",
        true,
    ),
    FORMAT(
        "Esa calidad o formato no está disponible",
        "Prueba 'Mejor calidad' o consulta los formatos en la pestaña Formato.",
        false,
    ),
    UNSUPPORTED(
        "Enlace no compatible",
        "Revisa que sea la URL completa de un video o playlist.",
        false,
    ),
    UNAVAILABLE(
        "El video no está disponible",
        "Puede ser privado, eliminado, con restricción de edad o de otra región.",
        false,
    ),
    NETWORK(
        "Problema de red",
        "Revisa tu conexión. En Ajustes puedes subir el tiempo de espera o activar 'Forzar IPv4'.",
        true,
    ),
    SERVER(
        "El servidor falló",
        null,
        true,
    ),
    STORAGE(
        "No se pudo guardar el archivo",
        "Libera espacio o elige otra carpeta en Ajustes → Descarga.",
        false,
    ),
    FFMPEG(
        "Falló la conversión con ffmpeg",
        "Prueba otro formato o contenedor en Ajustes → Descarga.",
        false,
    ),
    ENGINE(
        "El motor (Python/yt-dlp) no pudo ejecutarse",
        "Ajustes → Motor → Restablecer a la versión incluida.",
        false,
    ),
    UNKNOWN(
        "Error desconocido",
        "Exporta el log en Ajustes → Diagnóstico y compártelo.",
        false,
    ),
}

data class Diagnosis(val kind: ErrorKind, val line: String)

/** Convierte el stderr de yt-dlp (o una excepción) en un diagnóstico entendible. */
object ErrorClassifier {

    fun classify(raw: String?): Diagnosis {
        val text = raw.orEmpty()
        val low = text.lowercase()
        val line = pickLine(text)
        val noJs = "no supported javascript runtime" in low

        val kind = when {
            "confirm you’re not a bot" in low || "confirm you're not a bot" in low ||
                "sign in to confirm" in low -> ErrorKind.BOT_CHECK

            "n challenge" in low || "signature solving failed" in low ||
                "challenge solving failed" in low -> ErrorKind.JS_RUNTIME

            "http error 429" in low || "too many requests" in low -> ErrorKind.TOO_MANY

            "http error 403" in low -> if (noJs) ErrorKind.JS_RUNTIME else ErrorKind.FORBIDDEN

            "requested format is not available" in low ->
                if (noJs) ErrorKind.JS_RUNTIME else ErrorKind.FORMAT

            "unsupported url" in low || "is not a valid url" in low -> ErrorKind.UNSUPPORTED

            "video unavailable" in low || "private video" in low || "this video is not available" in low ||
                "has been removed" in low || "members-only" in low || "confirm your age" in low ||
                "age-restricted" in low || "not available in your country" in low -> ErrorKind.UNAVAILABLE

            "no space left" in low || "enospc" in low || "permission denied" in low ||
                "read-only file system" in low || "disk quota" in low -> ErrorKind.STORAGE

            "syntaxerror" in low || "importerror" in low || "modulenotfounderror" in low ||
                "unsatisfiedlink" in low || "cannot run program" in low || "error=13" in low ||
                "failed to initialize" in low -> ErrorKind.ENGINE

            "ffmpeg" in low && ("postprocessing" in low || "conversion failed" in low ||
                "not found" in low || "error while" in low) -> ErrorKind.FFMPEG

            Regex("http error 5\\d\\d").containsMatchIn(low) || "bad gateway" in low ||
                "service unavailable" in low -> ErrorKind.SERVER

            "timed out" in low || "timeout" in low || "connection reset" in low ||
                "connection refused" in low || "temporary failure in name resolution" in low ||
                "name or service not known" in low || "network is unreachable" in low ||
                "unable to download" in low || "ssl" in low || "eof occurred" in low ||
                "incomplete read" in low || "remote end closed" in low || "getaddrinfo" in low ||
                "unknownhostexception" in low -> ErrorKind.NETWORK

            else -> ErrorKind.UNKNOWN
        }
        return Diagnosis(kind, line)
    }

    private fun pickLine(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val line = lines.lastOrNull { it.contains("ERROR", ignoreCase = true) }
            ?: lines.lastOrNull()
            ?: "Sin detalles"
        return line.substringAfter("ERROR:", line).trim().take(260)
    }
}
