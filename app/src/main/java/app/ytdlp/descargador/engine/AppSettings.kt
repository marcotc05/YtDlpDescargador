package app.ytdlp.descargador.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class UpdateChannel { STABLE, NIGHTLY }

/** Preferencias del usuario. Los valores por defecto reproducen el comportamiento del .bat. */
data class AppPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Int = 0,
    val fontScale: Float = 1f,
    /** Contenedor para --merge-output-format. El .bat usa mp4. */
    val container: String = "mp4",
    /** Reintentos automáticos (además del primer intento) ante errores de red. */
    val retries: Int = 3,
    /** --socket-timeout, en segundos. */
    val timeoutSec: Int = 30,
    val forceIpv4: Boolean = false,
    val extraArgs: String = "",
    val channel: UpdateChannel = UpdateChannel.STABLE,
    val autoUpdate: Boolean = true,
) {
    fun sanitized(): AppPrefs = copy(
        accent = accent.coerceIn(0, ACCENT_COUNT - 1),
        fontScale = fontScale.coerceIn(0.85f, 1.4f),
        container = if (container in CONTAINERS) container else "mp4",
        retries = retries.coerceIn(0, 8),
        timeoutSec = timeoutSec.coerceIn(10, 120),
    )

    companion object {
        const val ACCENT_COUNT = 5
        val CONTAINERS = listOf("mp4", "mkv", "webm")
    }
}

object AppSettings {
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(AppPrefs())
    val state: StateFlow<AppPrefs> = _state.asStateFlow()
    val current: AppPrefs get() = _state.value

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        _state.value = load()
    }

    fun update(change: (AppPrefs) -> AppPrefs) {
        val next = change(_state.value).sanitized()
        _state.value = next
        if (::prefs.isInitialized) save(next)
    }

    private fun <E : Enum<E>> enumOf(key: String, default: E, values: Array<E>): E {
        val name = prefs.getString(key, null) ?: return default
        return values.firstOrNull { it.name == name } ?: default
    }

    private fun load(): AppPrefs {
        val d = AppPrefs()
        return AppPrefs(
            themeMode = enumOf("theme", d.themeMode, ThemeMode.values()),
            accent = prefs.getInt("accent", d.accent),
            fontScale = prefs.getFloat("font_scale", d.fontScale),
            container = prefs.getString("container", d.container) ?: d.container,
            retries = prefs.getInt("retries", d.retries),
            timeoutSec = prefs.getInt("timeout", d.timeoutSec),
            forceIpv4 = prefs.getBoolean("ipv4", d.forceIpv4),
            extraArgs = prefs.getString("extra_args", d.extraArgs) ?: d.extraArgs,
            channel = enumOf("channel", d.channel, UpdateChannel.values()),
            autoUpdate = prefs.getBoolean("auto_update", d.autoUpdate),
        ).sanitized()
    }

    private fun save(p: AppPrefs) {
        prefs.edit()
            .putString("theme", p.themeMode.name)
            .putInt("accent", p.accent)
            .putFloat("font_scale", p.fontScale)
            .putString("container", p.container)
            .putInt("retries", p.retries)
            .putInt("timeout", p.timeoutSec)
            .putBoolean("ipv4", p.forceIpv4)
            .putString("extra_args", p.extraArgs)
            .putString("channel", p.channel.name)
            .putBoolean("auto_update", p.autoUpdate)
            .apply()
    }
}

/** Divide una línea de argumentos respetando comillas simples y dobles. */
object ArgsTokenizer {
    fun split(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var quote: Char? = null
        var started = false
        for (c in line) {
            when {
                quote != null -> if (c == quote) quote = null else cur.append(c)
                c == '"' || c == '\'' -> { quote = c; started = true }
                c.isWhitespace() -> if (started || cur.isNotEmpty()) {
                    out.add(cur.toString()); cur.clear(); started = false
                }
                else -> { cur.append(c); started = true }
            }
        }
        if (started || cur.isNotEmpty()) out.add(cur.toString())
        return out
    }
}
