package app.pocketbible.ui

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * An [IconButton] that shows [text] in a small tooltip on long-press --
 * touch devices have no hover, so this is the equivalent affordance for
 * icon-only controls (language switcher, bookmark, info) whose meaning
 * isn't otherwise labeled on screen. [text] is already localized by the
 * caller, same string used for the icon's contentDescription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(text: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        modifier = Modifier.wrapContentSize()
    ) {
        IconButton(onClick = onClick) {
            icon()
        }
    }
}
