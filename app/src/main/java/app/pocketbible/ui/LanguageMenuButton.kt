package app.pocketbible.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.pocketbible.R

/**
 * Language tag to native name, so each option stays findable regardless of
 * the app's current locale. `null` means "follow the system language" and
 * is rendered separately using [R.string.language_system_default].
 */
private val APP_LANGUAGES: List<Pair<String?, String>> = listOf(
    null to "",
    "en" to "English",
    "de" to "Deutsch",
    "fr" to "Français",
    "pt" to "Português",
    "es" to "Español",
    "hi" to "हिन्दी",
    "it" to "Italiano",
    "mr" to "मराठी"
)

/** Shared language switcher, shown top-right on every main tab screen. */
@Composable
fun LanguageMenuButton(onLanguageSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val systemDefaultLabel = stringResource(R.string.language_system_default)
    val languageButtonLabel = stringResource(R.string.language_button)
    Box {
        TooltipIconButton(text = languageButtonLabel, onClick = { expanded = true }) {
            Icon(Icons.Filled.Language, contentDescription = languageButtonLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            APP_LANGUAGES.forEach { (tag, nativeName) ->
                DropdownMenuItem(
                    text = { Text(if (tag == null) systemDefaultLabel else nativeName) },
                    onClick = {
                        expanded = false
                        onLanguageSelected(tag)
                    }
                )
            }
        }
    }
}
