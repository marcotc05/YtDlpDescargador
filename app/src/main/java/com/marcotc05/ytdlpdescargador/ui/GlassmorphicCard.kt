package com.marcotc05.ytdlpdescargador.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphicContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP).asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(blurModifier)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x33282C3E),
                        Color(0x1A141622)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x8000F0FF),
                            Color(0x20FF0055)
                        )
                    )
                ),
                shape
            )
    ) {
        content()
    }
}