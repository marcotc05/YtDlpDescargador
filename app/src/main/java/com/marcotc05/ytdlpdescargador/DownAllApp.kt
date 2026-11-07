package com.marcotc05.ytdlpdescargador

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownAllApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val _isEngineReady = MutableStateFlow(false)
        val isEngineReady = _isEngineReady.asStateFlow()

        var engineInitLog: String = ""
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                _isEngineReady.value = true
                engineInitLog = "Engine OK: Binarios inicializados en almacenamiento interno."
                Log.i("DOWNALL_ENGINE", engineInitLog)
            } catch (e: Exception) {
                _isEngineReady.value = false
                engineInitLog = "CRITICAL INIT FAILURE:\n" + e.stackTraceToString()
                Log.e("DOWNALL_ENGINE", engineInitLog)
            }
        }
    }
}