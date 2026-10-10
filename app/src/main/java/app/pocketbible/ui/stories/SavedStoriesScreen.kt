package app.pocketbible.ui.stories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pocketbible.R
import app.pocketbible.data.StorySummary
import app.pocketbible.ui.saved.ReorderableLazyColumn

/**
 * Stories the user bookmarked, reached from the Stories tab -- reuses
 * [StoryRow], the same row rendering as the main Stories list, just fed a
 * pre-filtered (isSaved) list instead of the full one.
 */
@Composable
fun SavedStoriesScreen(
    stories: List<StorySummary>,
    onBack: () -> Unit,
    onStorySelected: (StorySummary) -> Unit,
    onUnsave: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.read_back))
            }
            Text(
                stringResource(R.string.saved_stories_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        }

        if (stories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.saved_stories_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            ReorderableLazyColumn(
                values = stories.sortedByDescending { it.savedOrder ?: Long.MIN_VALUE },
                key = { it.id },
                onReorder = { onReorder(it.map { story -> story.id }) },
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp)
            ) { story, dragHandleModifier ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        StoryRow(story, onClick = { onStorySelected(story) })
                    }
                    Column {
                        IconButton(onClick = { onUnsave(story.id) }) {
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
                }
            }
        }
    }
}
