package app.pocketbible.ui.verse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketbible.data.EntrySummary
import app.pocketbible.data.PassageWithRole

@Composable
fun VerseScreen(
    feelingLabel: String,
    entry: EntrySummary?,
    extraPassages: List<PassageWithRole>,
    onAnother: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (entry == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No entries yet for $feelingLabel.")
        }
        return
    }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(feelingLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(14.dp))

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    entry.passageText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    entry.referenceAlt?.let { "${entry.referenceDisplay}  ·  $it" } ?: entry.referenceDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        extraPassages.forEach { extra ->
            Spacer(Modifier.height(10.dp))
            Text(
                extra.passage.pullQuote ?: extra.passage.text,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("What it means for you", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(entry.entry.reflection, style = MaterialTheme.typography.bodyMedium)

        entry.entry.saintQuote?.let { quote ->
            Spacer(Modifier.height(14.dp))
            Text(
                quote,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
            Text(
                "— ${entry.entry.saintAttribution ?: "A saint"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "A short prayer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.entry.prayer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onToggleSave, modifier = Modifier.weight(1f)) {
                Icon(
                    if (entry.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(if (entry.isSaved) "Saved" else "Save")
            }
            OutlinedButton(onClick = onAnother, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Another")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
