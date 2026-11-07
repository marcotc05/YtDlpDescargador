package app.ytdlp.descargador.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytdlp.descargador.engine.AppSettings
import app.ytdlp.descargador.engine.ThemeMode
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcotc05.ytdlpdescargador.R

/** Colores de marca que no forman parte del esquema de Material. */
object Brand {
    /** Tarjeta de la descarga activa: el elemento protagonista de la app. */
    val Ink = Color(0xFF14163A)
    val OnInk = Color(0xFFF2F3FF)
    val OnInkMuted = Color(0xFFA9ACD6)
    /** Ámbar: solo para el progreso. */
    val Signal = Color(0xFFFFC23D)
    val Good = Color(0xFF1FB57A)
}

private val Light = lightColorScheme(
    primary = Color(0xFF3A35F0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEDFFF),
    onPrimaryContainer = Color(0xFF14153F),
    secondary = Color(0xFF5B5E85),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E4F8),
    onSecondaryContainer = Color(0xFF1B1D45),
    background = Color(0xFFEEF0F8),
    onBackground = Color(0xFF11132B),
    surface = Color(0xFFF6F7FC),
    onSurface = Color(0xFF11132B),
    surfaceVariant = Color(0xFFE2E5F3),
    onSurfaceVariant = Color(0xFF4A4D6B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FD),
    surfaceContainer = Color(0xFFF1F3FA),
    surfaceContainerHigh = Color(0xFFE9ECF7),
    surfaceContainerHighest = Color(0xFFE2E5F3),
    outline = Color(0xFF8F93B5),
    outlineVariant = Color(0xFFCDD1E6),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9A95FF),
    onPrimary = Color(0xFF0E0F33),
    primaryContainer = Color(0xFF2A2C6B),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFFBFC2EC),
    onSecondary = Color(0xFF282B55),
    secondaryContainer = Color(0xFF2A2D5A),
    onSecondaryContainer = Color(0xFFDEE0FF),
    background = Color(0xFF0C0D1F),
    onBackground = Color(0xFFECEDFF),
    surface = Color(0xFF12142B),
    onSurface = Color(0xFFECEDFF),
    surfaceVariant = Color(0xFF23264D),
    onSurfaceVariant = Color(0xFFB5B8DA),
    surfaceContainerLowest = Color(0xFF090A18),
    surfaceContainerLow = Color(0xFF14162E),
    surfaceContainer = Color(0xFF181A36),
    surfaceContainerHigh = Color(0xFF1F2242),
    surfaceContainerHighest = Color(0xFF262A4E),
    outline = Color(0xFF6F739B),
    outlineVariant = Color(0xFF34376A),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0004),
    errorContainer = Color(0xFF5C1217),
    onErrorContainer = Color(0xFFFFDAD6),
)

// LÍNEA CORREGIDA: Se añade @OptIn encima de la función
@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int) = Font(
    R.font.bricolage,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Bricolage = FontFamily(
    bricolage(400), bricolage(500), bricolage(600), bricolage(700), bricolage(800),
)

private fun type(): Typography {
    val d = Typography()
    fun TextStyle.f() = copy(fontFamily = Bricolage)
    return Typography(
        displayLarge = d.displayLarge.f(),
        displayMedium = d.displayMedium.f(),
        displaySmall = d.displaySmall.f().copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineLarge = d.headlineLarge.f(),
        headlineMedium = d.headlineMedium.f().copy(fontWeight = FontWeight.Bold),
        headlineSmall = d.headlineSmall.f().copy(fontWeight = FontWeight.Bold),
        titleLarge = d.titleLarge.f().copy(fontWeight = FontWeight.Bold),
        titleMedium = d.titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = d.titleSmall.f().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = d.bodyLarge.f(),
        bodyMedium = d.bodyMedium.f(),
        bodySmall = d.bodySmall.f(),
        labelLarge = d.labelLarge.f().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = d.labelMedium.f().copy(fontWeight = FontWeight.Medium),
        labelSmall = d.labelSmall.f().copy(fontWeight = FontWeight.Medium),
    )
}

/** Cifra grande de la descarga activa. */
val HeroNumber = TextStyle(
    fontFamily = Bricolage,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 76.sp,
    lineHeight = 76.sp,
    letterSpacing = (-3).sp,
)

/** Colores de acento disponibles en Ajustes. El primero es la identidad original de la app. */
data class Accent(
    val name: String,
    val light: Color,
    val lightContainer: Color,
    val dark: Color,
    val darkContainer: Color,
)

val Accents = listOf(
    Accent("Ultramar", Color(0xFF3A35F0), Color(0xFFDEDFFF), Color(0xFF9A95FF), Color(0xFF2A2C6B)),
    Accent("Turquesa", Color(0xFF0B7A75), Color(0xFFCCF2EE), Color(0xFF5FD6CD), Color(0xFF0F3F3C)),
    Accent("Magenta", Color(0xFFB5179E), Color(0xFFFBD9F4), Color(0xFFF08BDD), Color(0xFF5A0F4E)),
    Accent("Naranja", Color(0xFFC2410C), Color(0xFFFFE0CC), Color(0xFFFFA574), Color(0xFF5E2308)),
    Accent("Verde", Color(0xFF15803D), Color(0xFFD3F3DE), Color(0xFF7CE0A0), Color(0xFF0F3F20)),
)

/** Espaciados uniformes de toda la app. */
object Dimens {
    val Screen = 20.dp
    val Gap = 16.dp
    val Card = 24.dp
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val prefs by AppSettings.state.collectAsStateWithLifecycle()
    val dark = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = Accents[prefs.accent.coerceIn(0, Accents.lastIndex)]
    val scheme = if (dark) {
        Dark.copy(primary = accent.dark, primaryContainer = accent.darkContainer)
    } else {
        Light.copy(primary = accent.light, primaryContainer = accent.lightContainer)
    }

    // Iconos de la barra de estado/navegación legibles según el tema elegido (no solo el del sistema).
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        onDispose { }
    }

    // Tamaño de fuente de la app, multiplicado sobre el del sistema.
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * prefs.fontScale)) {
        MaterialTheme(colorScheme = scheme, typography = type(), content = content)
    }
}
