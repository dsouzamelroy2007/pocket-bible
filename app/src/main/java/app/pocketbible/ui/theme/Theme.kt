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
private val ClayContainerDark = Color(0xFF4A2A15)
private val Muted = Color(0xFF6B6558)
private val Teal = Color(0xFF3F6E64)
private val TealLight = Color(0xFFD9EBE5)
private val TealLightInk = Color(0xFF163A32)
private val TealDark = Color(0xFF7FC2B4)
private val TealContainerDark = Color(0xFF1E4A40)
private val Gold = Color(0xFFF7EBC8)
private val GoldInk = Color(0xFF4A3B14)
private val GoldContainerDark = Color(0xFF4A3B14)
private val GoldInkDark = Color(0xFFF3E7C9)
private val Purple = Color(0xFFE7D9F5)
private val PurpleInk = Color(0xFF33234A)
private val PurpleContainerDark = Color(0xFF33234A)
private val PurpleInkDark = Color(0xFFE7D9F5)
private val Green = Color(0xFFDCF0D4)
private val GreenInk = Color(0xFF244A1E)
private val GreenContainerDark = Color(0xFF244A1E)
private val GreenInkDark = Color(0xFFDCF0D4)
private val SkyBlue = Color(0xFFD4E7F0)
private val SkyBlueInk = Color(0xFF1E3A4A)
private val SkyBlueContainerDark = Color(0xFF1E3A4A)
private val SkyBlueInkDark = Color(0xFFD4E7F0)
private val NeutralContainerLight = Color(0xFFE7E1D2)
private val NeutralInkLight = Color(0xFF383426)
private val NeutralContainerDark = Color(0xFF3A362A)
private val NeutralInkDark = Color(0xFFE7E1D2)

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
        "distress" -> if (dark) CategoryAccent(ClayContainerDark, ClayLight) else CategoryAccent(ClayLight, ClayLightInk)
        "moral" -> if (dark) CategoryAccent(GoldContainerDark, GoldInkDark) else CategoryAccent(Gold, GoldInk)
        "relational" -> if (dark) CategoryAccent(TealContainerDark, TealLight) else CategoryAccent(TealLight, TealLightInk)
        "spiritual" -> if (dark) CategoryAccent(PurpleContainerDark, PurpleInkDark) else CategoryAccent(Purple, PurpleInk)
        "thanksgiving" -> if (dark) CategoryAccent(GreenContainerDark, GreenInkDark) else CategoryAccent(Green, GreenInk)
        "desire" -> if (dark) CategoryAccent(SkyBlueContainerDark, SkyBlueInkDark) else CategoryAccent(SkyBlue, SkyBlueInk)
        else -> if (dark) CategoryAccent(NeutralContainerDark, NeutralInkDark) else CategoryAccent(NeutralContainerLight, NeutralInkLight)
    }
}

/**
 * Old/New Testament accent colors for the Stories filter chips -- reuses
 * this app's existing Clay/Teal brand colors (the same ones "distress"/
 * "relational" feeling categories use) rather than a new palette.
 */
@Composable
fun testamentAccent(testament: String?): CategoryAccent {
    val dark = isSystemInDarkTheme()
    return when (testament) {
        "ot" -> if (dark) CategoryAccent(ClayContainerDark, ClayLight) else CategoryAccent(ClayLight, ClayLightInk)
        "nt" -> if (dark) CategoryAccent(TealContainerDark, TealLight) else CategoryAccent(TealLight, TealLightInk)
        else -> if (dark) CategoryAccent(NeutralContainerDark, NeutralInkDark) else CategoryAccent(NeutralContainerLight, NeutralInkLight)
    }
}

/**
 * Narrative/Parable/Miracle accent colors for the Stories filter chips and
 * the per-story type tag -- three more hues already used for feeling
 * categories elsewhere, chosen to stay visually distinct from the
 * testament accents above.
 */
@Composable
fun storyTypeAccent(storyType: String?): CategoryAccent {
    val dark = isSystemInDarkTheme()
    return when (storyType) {
        "narrative" -> if (dark) CategoryAccent(GoldContainerDark, GoldInkDark) else CategoryAccent(Gold, GoldInk)
        "parable" -> if (dark) CategoryAccent(PurpleContainerDark, PurpleInkDark) else CategoryAccent(Purple, PurpleInk)
        "miracle" -> if (dark) CategoryAccent(SkyBlueContainerDark, SkyBlueInkDark) else CategoryAccent(SkyBlue, SkyBlueInk)
        else -> if (dark) CategoryAccent(NeutralContainerDark, NeutralInkDark) else CategoryAccent(NeutralContainerLight, NeutralInkLight)
    }
}

@Composable
fun PocketBibleTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
