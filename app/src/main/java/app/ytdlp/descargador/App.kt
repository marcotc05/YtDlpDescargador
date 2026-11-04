package app.ytdlp.descargador

import android.app.Application
import app.ytdlp.descargador.engine.AppLog
import app.ytdlp.descargador.engine.AppSettings
import app.ytdlp.descargador.engine.DownloadManager
import app.ytdlp.descargador.engine.Notifier

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        AppLog.init(this)
        installCrashLogger()
        Notifier.ensureChannels(this)
        DownloadManager.init(this)
    }

    /** Deja constancia de los cierres inesperados en el log exportable. */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            AppLog.e("crash", "Excepción no controlada en ${thread.name}", error = error)
            previous?.uncaughtException(thread, error)
        }
    }
}
