package app.ytdlp.descargador

import android.app.Application
import app.ytdlp.descargador.engine.DownloadManager
import app.ytdlp.descargador.engine.Notifier

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
        DownloadManager.init(this)
    }
}
