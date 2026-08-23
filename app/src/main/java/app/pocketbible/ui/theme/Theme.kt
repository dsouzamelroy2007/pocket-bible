package app.pocketbible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ivory = Color(0xFFFBF8F2)
private val Ink = Color(0xFF232019)
private val Clay = Color(0xFFB5652E)
private val ClayLight = Color(0xFFF3E1D3)
private val ClayLightInk = Color(0xFF5C2E10)
private val ClayDark = Color(0xFFE0925A)
private val Muted = Color(0xFF6B6558)

private val LightColors = lightColorScheme(
    primary = Clay,
    onPrimary = Color.White,
    primaryContainer = ClayLight,
    onPrimaryContainer = ClayLightInk,
    background = Ivory,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    secondary = Muted
)

private val DarkColors = darkColorScheme(
    primary = ClayDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A2A15),
    onPrimaryContainer = Color(0xFFF3E1D3),
    background = Color(0xFF1B1912),
    surface = Color(0xFF242018),
    onBackground = Color(0xFFEDE8DC),
    onSurface = Color(0xFFEDE8DC),
    secondary = Color(0xFFB8AF9C)
)

@Composable
fun PocketBibleTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
