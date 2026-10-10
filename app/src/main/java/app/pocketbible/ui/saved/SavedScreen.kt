package app.pocketbible.ui.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pocketbible.R
import app.pocketbible.data.EntrySummary
import app.pocketbible.ui.bible.localizedReference

@Composable
fun SavedScreen(
    saved: List<EntrySummary>,
    onUnsave: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (saved.isEmpty()) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.saved_empty),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp)) {
            Text(
                stringResource(R.string.saved_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
        }
        ReorderableLazyColumn(
            values = saved,
            key = { it.entry.id },
            onReorder = { onReorder(it.map { summary -> summary.entry.id }) },
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp)
        ) { entry, dragHandleModifier ->
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.passageText, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            localizedReference(entry.bookId, entry.chapter, entry.verseStart, entry.verseEnd),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = { onUnsave(entry.entry.id) }) {
                        Icon(
                            Icons.Filled.BookmarkRemove,
                            contentDescription = stringResource(R.string.saved_remove)
                        )
                    }
                    IconButton(onClick = {}, modifier = dragHandleModifier) {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.saved_reorder)
                        )
                    }
                }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
