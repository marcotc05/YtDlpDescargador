package com.marcotc05.ytdlpdescargador.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownAllPreferences(
    val downloadFolderDisplay: String = "Descargas/YT-DLP",
    val downloadFolderTreeUri: String = "",
    val defaultFormat: String = "MP4",
    val defaultQuality: String = "best",
    val retries: Int = 3,
    val socketTimeoutSeconds: Int = 30,
    val forceIpv4: Boolean = true,
    val themeMode: String = "SYSTEM",
    val glassBlurEnabled: Boolean = true
)

class SettingsManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("downall_user_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadPreferences())
    val state: StateFlow<DownAllPreferences> = _state.asStateFlow()

    private fun loadPreferences(): DownAllPreferences {
        return DownAllPreferences(
            downloadFolderDisplay = prefs.getString("folder_display", "Descargas/YT-DLP") ?: "Descargas/YT-DLP",
            downloadFolderTreeUri = prefs.getString("folder_tree_uri", "") ?: "",
            defaultFormat = prefs.getString("default_format", "MP4") ?: "MP4",
            defaultQuality = prefs.getString("default_quality", "best") ?: "best",
            retries = prefs.getInt("retries", 3).coerceIn(0, 5),
            socketTimeoutSeconds = prefs.getInt("socket_timeout", 30).coerceIn(10, 120),
            forceIpv4 = prefs.getBoolean("force_ipv4", true),
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            glassBlurEnabled = prefs.getBoolean("glass_blur", true)
        )
    }

    fun updateFolder(treeUri: String, display: String) {
        prefs.edit()
            .putString("folder_tree_uri", treeUri)
            .putString("folder_display", display)
            .apply()
        _state.value = loadPreferences()
    }

    fun updateDefaultFormat(format: String) {
        prefs.edit().putString("default_format", format).apply()
        _state.value = loadPreferences()
    }

    fun updateRetries(retries: Int) {
        prefs.edit().putInt("retries", retries.coerceIn(0, 5)).apply()
        _state.value = loadPreferences()
    }

    fun updateTimeout(timeout: Int) {
        prefs.edit().putInt("socket_timeout", timeout.coerceIn(10, 120)).apply()
        _state.value = loadPreferences()
    }

    fun updateForceIpv4(force: Boolean) {
        prefs.edit().putBoolean("force_ipv4", force).apply()
        _state.value = loadPreferences()
    }

    fun updateThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _state.value = loadPreferences()
    }

    fun updateGlassBlur(enabled: Boolean) {
        prefs.edit().putBoolean("glass_blur", enabled).apply()
        _state.value = loadPreferences()
    }

    companion object {
        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context).also { instance = it }
            }
        }
    }
}