package com.marcotc05.ytdlpdescargador.model

data class DownloadConfig(
    val isAudioOnly: Boolean = false,
    val forceIpv4: Boolean = true,
    val socketTimeoutSeconds: Int = 30,
    val maxRetries: Int = 3
)