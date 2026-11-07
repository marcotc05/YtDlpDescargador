package com.marcotc05.ytdlpdescargador.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marcotc05.ytdlpdescargador.ui.theme.AeroCyanAccent
import com.marcotc05.ytdlpdescargador.ui.theme.AeroGlassBorderAccent
import com.marcotc05.ytdlpdescargador.ui.theme.AeroGlassBorderBottom
import com.marcotc05.ytdlpdescargador.ui.theme.AeroGlassBorderTop
import com.marcotc05.ytdlpdescargador.ui.theme.AeroGlassHighlight
import com.marcotc05.ytdlpdescargador.ui.theme.AeroGlassSurface

@Composable
fun AeroLiquidCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    borderAccentColor: Color = AeroCyanAccent,
    enableBlur: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = Brush.verticalGradient(
        0.0f to AeroGlassBorderTop,
        0.35f to borderAccentColor.copy(alpha = 0.45f),
        0.80f to AeroGlassBorderAccent,
        1.0f to AeroGlassBorderBottom
    )

    val acrylicBrush = Brush.verticalGradient(
        listOf(
            AeroGlassSurface.copy(alpha = 0.40f),
            AeroGlassSurface.copy(alpha = 0.18f)
        )
    )

    val blurModifier = if (enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect.createBlurEffect(
                22f, 22f, Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(BorderStroke(1.2.dp, borderBrush), shape)
            .background(acrylicBrush)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(blurModifier)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AeroGlassHighlight,
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
fun GlassmorphicContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AeroLiquidCard(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth(), content = content)
    }
}