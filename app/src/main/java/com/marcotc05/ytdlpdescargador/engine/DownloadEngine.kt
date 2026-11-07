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
            val cacheDir = File(context.filesDir, "yt_tmp").apply { if (!exists()) mkdirs() }
            val targetDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
                "DownAll"
            ).apply { if (!exists()) mkdirs() }

            val request = YoutubeDLRequest(url.trim())
            request.addOption("--cache-dir", cacheDir.absolutePath)
            request.addOption("--socket-timeout", config.socketTimeoutSeconds.toString())
            request.addOption("--retries", config.maxRetries.toString())
            request.addOption("--no-mtime")
            if (config.forceIpv4) {
                request.addOption("--force-ipv4")
            }

            if (config.isAudioOnly) {
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
                request.addOption("-o", "${targetDir.absolutePath}/%(title)s.%(ext)s")
            } else {
                request.addOption("-f", "bestvideo+bestaudio/best")
                request.addOption("--merge-output-format", "mp4")
                request.addOption("-o", "${targetDir.absolutePath}/%(title)s.%(ext)s")
            }

            val processTag = "DOWNALL_" + System.currentTimeMillis()
            val response = YoutubeDL.getInstance().execute(
                request = request,
                processId = processTag
            ) { progress, eta, line ->
                val lineMsg = line ?: "Progreso: ${progress.toInt()}% (ETA: ${eta}s)"
                onProgress(progress, lineMsg)
            }

            Result.success("Guardado en: ${targetDir.absolutePath}\n\nLog:\n${response.out ?: "OK"}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(
        context: Context,
        url: String,
        config: DownloadConfig,
        onProgress: (Float, String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        val result = executeDownload(context, url, config, onProgress)
        result.onSuccess { onComplete(true, it) }.onFailure { onComplete(false, it.message ?: it.toString()) }
    }
}