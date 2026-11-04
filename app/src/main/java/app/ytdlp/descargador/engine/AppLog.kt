package app.ytdlp.descargador.engine

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val time: Long,
    val level: String,
    val tag: String,
    val message: String,
    val fields: Map<String, String>,
    val stack: String?,
)

/**
 * Registro persistente de eventos (JSON por línea) con rotación por tamaño.
 * Pensado para exportarse y compartirse: nunca se guardan cookies ni contenido de archivos.
 */
object AppLog {
    private const val MAX_BYTES = 512 * 1024
    private const val KEEP_LINES = 300
    private const val MAX_FIELD = 6000

    private val lock = Any()
    private var file: File? = null

    fun init(context: Context) {
        val dir = File(context.filesDir, "logs")
        dir.mkdirs()
        file = File(dir, "app.log")
    }

    fun e(tag: String, message: String, fields: Map<String, String?> = emptyMap(), error: Throwable? = null) =
        write("ERROR", tag, message, fields, error)

    fun w(tag: String, message: String, fields: Map<String, String?> = emptyMap(), error: Throwable? = null) =
        write("WARN", tag, message, fields, error)

    fun i(tag: String, message: String, fields: Map<String, String?> = emptyMap()) =
        write("INFO", tag, message, fields, null)

    private fun write(level: String, tag: String, message: String, fields: Map<String, String?>, error: Throwable?) {
        val f = file ?: return
        try {
            val json = JSONObject()
            json.put("t", System.currentTimeMillis())
            json.put("l", level)
            json.put("g", tag)
            json.put("m", message.take(MAX_FIELD))
            val fj = JSONObject()
            fields.forEach { (k, v) -> if (v != null) fj.put(k, v.take(MAX_FIELD)) }
            json.put("f", fj)
            if (error != null) json.put("s", android.util.Log.getStackTraceString(error).take(MAX_FIELD))
            synchronized(lock) {
                f.appendText(json.toString() + "\n")
                if (f.length() > MAX_BYTES) rotate(f)
            }
            android.util.Log.println(
                when (level) { "ERROR" -> android.util.Log.ERROR; "WARN" -> android.util.Log.WARN; else -> android.util.Log.INFO },
                "YtDlpDescargador/$tag", message,
            )
        } catch (ignored: Throwable) {
            // El registro nunca debe romper la app.
        }
    }

    private fun rotate(f: File) {
        val lines = f.readLines()
        f.writeText(lines.takeLast(KEEP_LINES).joinToString("\n", postfix = "\n"))
    }

    fun entries(limit: Int = 100, onlyProblems: Boolean = false): List<LogEntry> {
        val f = file ?: return emptyList()
        return synchronized(lock) {
            if (!f.exists()) return@synchronized emptyList()
            f.readLines().mapNotNull { parse(it) }
        }.filter { !onlyProblems || it.level != "INFO" }.takeLast(limit)
    }

    fun problemCount(): Int = entries(10_000, onlyProblems = true).size

    fun clear() {
        synchronized(lock) { file?.delete() }
    }

    private fun parse(line: String): LogEntry? = try {
        val j = JSONObject(line)
        val fj = j.optJSONObject("f")
        val fields = mutableMapOf<String, String>()
        fj?.keys()?.forEach { k -> fields[k] = fj.optString(k) }
        LogEntry(j.optLong("t"), j.optString("l"), j.optString("g"), j.optString("m"), fields, j.optString("s").ifEmpty { null })
    } catch (ignored: Throwable) {
        null
    }

    fun format(e: LogEntry): String = buildString {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(e.time))
        append("[").append(ts).append("] ").append(e.level).append(" ").append(e.tag).append(": ").append(e.message).append('\n')
        e.fields.forEach { (k, v) ->
            val lines = v.lines()
            if (lines.size <= 1) append("    ").append(k).append(": ").append(v).append('\n')
            else {
                append("    ").append(k).append(":\n")
                lines.forEach { append("        ").append(it).append('\n') }
            }
        }
        e.stack?.let { st -> append("    stack:\n"); st.lines().forEach { append("        ").append(it).append('\n') } }
    }
}
