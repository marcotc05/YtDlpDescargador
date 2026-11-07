package com.marcotc05.ytdlpdescargador.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun AeroLiquidCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    accentColor: Color = Color(0xFF00F0FF),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            // Capa 1: Tinte acrilico liquido translúcido
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x3D11192C),
                        Color(0x240A0F1D)
                    )
                )
            )
            // Capa 2: Borde con refraccion y brillo especular Aero (Liquid Rim Light)
            .border(
                BorderStroke(
                    width = 1.3.dp,
                    brush = Brush.verticalGradient(
                        0.0f to Color(0xB3FFFFFF),
                        0.15f to accentColor.copy(alpha = 0.75f),
                        0.75f to accentColor.copy(alpha = 0.20f),
                        1.0f to Color(0x0A000000)
                    )
                ),
                shape = shape
            )
    ) {
        // Capa 3: Brillo curvado superior (Aero Specular Highlight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x22FFFFFF),
                            Color(0x05FFFFFF),
                            Color.Transparent
                        )
                    )
                )
        )

        // Capa 4: Contenido nítido e interactivo en primer plano (sin blur)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            content()
        }
    }
}

// Compatibilidad con invocaciones previas
@Composable
fun GlassmorphicContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AeroLiquidCard(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}