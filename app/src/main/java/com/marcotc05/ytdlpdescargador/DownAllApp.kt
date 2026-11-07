package com.marcotc05.ytdlpdescargador

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownAllApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeEngine()
    }

    private fun initializeEngine() {
        applicationScope.launch {
            try {
                YoutubeDL.getInstance().init(this@DownAllApp)
                _engineReady.value = true
                initLog = "Motor yt-dlp nativo inicializado correctamente en almacenamiento interno."
                Log.i("DownAllApp", initLog)
            } catch (e: YoutubeDLException) {
                _engineReady.value = false
                initLog = "ERROR CRÍTICO inicializando YoutubeDL: ${e.message}\n${Log.getStackTraceString(e)}"
                Log.e("DownAllApp", initLog, e)
            } catch (t: Throwable) {
                _engineReady.value = false
                initLog = "ERROR NO CONTROLADO en el motor: ${t.message}\n${Log.getStackTraceString(t)}"
                Log.e("DownAllApp", initLog, t)
            }
        }
    }

    companion object {
        lateinit var instance: DownAllApp
            private set

        private val _engineReady = MutableStateFlow(false)
        val engineReady: StateFlow<Boolean> = _engineReady.asStateFlow()
        val isEngineReady: StateFlow<Boolean> get() = engineReady

        var initLog: String = "Iniciando..."
            private set
    }
}