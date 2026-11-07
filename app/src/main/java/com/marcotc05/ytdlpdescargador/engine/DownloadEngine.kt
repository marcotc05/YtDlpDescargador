package com.marcotc05.ytdlpdescargador.engine

import android.content.Context
import android.os.Environment
import com.marcotc05.ytdlpdescargador.model.DownloadConfig
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DownloadEngine {
    suspend fun executeDownload(
        context: Context,
        url: String,
        config: DownloadConfig,
        onProgress: (Float, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val downloadDir = File(publicDir, "DownAll").apply { if (!exists()) mkdirs() }
            val cacheDir = File(context.filesDir, "yt_tmp").apply { if (!exists()) mkdirs() }

            val request = YoutubeDLRequest(url)
            request.addOption("--cache-dir", cacheDir.absolutePath)
            request.addOption("--socket-timeout", config.socketTimeoutSeconds.toString())
            request.addOption("--retries", config.maxRetries.toString())
            if (config.forceIpv4) request.addOption("--force-ipv4")

            if (config.isAudioOnly) {
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
                request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
            } else {
                request.addOption("-f", "bestvideo+bestaudio/best")
                request.addOption("--merge-output-format", "mp4")
                request.addOption("-o", "${downloadDir.absolutePath}/%(playlist)s/%(playlist_index)s - %(title)s.%(ext)s")
            }

            val response = YoutubeDL.getInstance().execute(
                request = request,
                processId = "DOWNALL_TASK"
            ) { progress, eta, line ->
                onProgress(progress, line ?: "ETA: ${eta}s")
            }

            Result.success(response.out ?: "Descarga completada")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}