package app.pocketbible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ivory = Color(0xFFFBF3E8)
private val Ink = Color(0xFF232019)
private val Clay = Color(0xFFB5652E)
private val ClayLight = Color(0xFFF3E1D3)
private val ClayLightInk = Color(0xFF5C2E10)
private val ClayDark = Color(0xFFE0925A)
private val Muted = Color(0xFF6B6558)
private val Teal = Color(0xFF3F6E64)
private val TealLight = Color(0xFFD9EBE5)
private val TealLightInk = Color(0xFF163A32)
private val TealDark = Color(0xFF7FC2B4)

private val LightColors = lightColorScheme(
    primary = Clay,
    onPrimary = Color.White,
    primaryContainer = ClayLight,
    onPrimaryContainer = ClayLightInk,
    secondary = Muted,
    secondaryContainer = Color(0xFFE7E1D2),
    onSecondaryContainer = Color(0xFF383426),
    tertiary = Teal,
    tertiaryContainer = TealLight,
    onTertiaryContainer = TealLightInk,
    background = Ivory,
    surface = Color(0xFFFFFDF9),
    surfaceVariant = Color(0xFFEFE3D2),
    onSurfaceVariant = Color(0xFF4E4739),
    onBackground = Ink,
    onSurface = Ink
)

private val DarkColors = darkColorScheme(
    primary = ClayDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A2A15),
    onPrimaryContainer = Color(0xFFF3E1D3),
    secondary = Color(0xFFB8AF9C),
    secondaryContainer = Color(0xFF3A362A),
    onSecondaryContainer = Color(0xFFE7E1D2),
    tertiary = TealDark,
    tertiaryContainer = Color(0xFF1E4A40),
    onTertiaryContainer = TealLight,
    background = Color(0xFF1B1912),
    surface = Color(0xFF242018),
    surfaceVariant = Color(0xFF3A3427),
    onSurfaceVariant = Color(0xFFD8D0BE),
    onBackground = Color(0xFFEDE8DC),
    onSurface = Color(0xFFEDE8DC)
)

/**
 * One accent per feeling category, used to tint cards on the topic grid so
 * the list reads as more than one undifferentiated white stack.
 */
data class CategoryAccent(val container: Color, val onContainer: Color)

@Composable
fun categoryAccent(category: String): CategoryAccent {
    val dark = isSystemInDarkTheme()
    return when (category) {
        "distress" -> if (dark) CategoryAccent(Color(0xFF4A2A15), Color(0xFFF3E1D3)) else CategoryAccent(ClayLight, ClayLightInk)
        "moral" -> if (dark) CategoryAccent(Color(0xFF4A3B14), Color(0xFFF3E7C9)) else CategoryAccent(Color(0xFFF7EBC8), Color(0xFF4A3B14))
        "relational" -> if (dark) CategoryAccent(Color(0xFF1E4A40), TealLight) else CategoryAccent(TealLight, TealLightInk)
        "spiritual" -> if (dark) CategoryAccent(Color(0xFF33234A), Color(0xFFE7D9F5)) else CategoryAccent(Color(0xFFE7D9F5), Color(0xFF33234A))
        "thanksgiving" -> if (dark) CategoryAccent(Color(0xFF244A1E), Color(0xFFDCF0D4)) else CategoryAccent(Color(0xFFDCF0D4), Color(0xFF244A1E))
        "desire" -> if (dark) CategoryAccent(Color(0xFF1E3A4A), Color(0xFFD4E7F0)) else CategoryAccent(Color(0xFFD4E7F0), Color(0xFF1E3A4A))
        else -> if (dark) CategoryAccent(Color(0xFF3A362A), Color(0xFFE7E1D2)) else CategoryAccent(Color(0xFFE7E1D2), Color(0xFF383426))
    }
}

@Composable
fun PocketBibleTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
