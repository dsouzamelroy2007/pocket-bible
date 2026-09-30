package app.pocketbible.ui.stories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
            LazyColumn(Modifier.padding(horizontal = 20.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(stories, key = { it.id }) { story ->
                    StoryRow(story, onClick = { onStorySelected(story) })
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}
