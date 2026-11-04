package app.ytdlp.descargador

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ytdlp.descargador.ui.HomeBottomArea
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba que el aviso "Agregada a la cola" queda SIEMPRE por encima del botón Descargar
 * (sin solaparse) y que el botón queda dentro de la pantalla, en distintos tamaños, orientaciones
 * y tamaños de fuente.
 *
 * Ejecutar con un emulador o dispositivo conectado:  ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class HomeBottomAreaTest {

    @get:Rule
    val rule = createComposeRule()

    private fun check(fontScale: Float, widthDp: Int, heightDp: Int) {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f))
                            HomeBottomArea(
                                showQueued = true,
                                canSubmit = true,
                                buttonLabel = "Descargar",
                                onSubmit = {},
                                onViewQueue = {},
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        val banner = rule.onNodeWithTag("queued_banner").getBoundsInRoot()
        val button = rule.onNodeWithTag("download_button").getBoundsInRoot()
        assertTrue("El aviso tapa el botón ($widthDp x $heightDp, fuente $fontScale)", banner.bottom <= button.top)
        assertTrue("El botón se sale de la pantalla", button.bottom <= heightDp.dp)
    }

    @Test fun phonePortraitSmall() = check(1f, 320, 568)
    @Test fun phonePortraitNormal() = check(1f, 393, 852)
    @Test fun phoneLandscape() = check(1f, 852, 393)
    @Test fun phoneLandscapeShort() = check(1f, 640, 300)
    @Test fun tablet() = check(1f, 800, 1280)
    @Test fun largeFont() = check(1.3f, 360, 640)
}
