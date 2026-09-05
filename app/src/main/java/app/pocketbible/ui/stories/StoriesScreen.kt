package app.pocketbible.ui.stories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pocketbible.R
import app.pocketbible.data.StorySummary

@Composable
fun StoriesScreen(
    stories: List<StorySummary>,
    onStorySelected: (StorySummary) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var testamentFilter by remember { mutableStateOf<String?>(null) }
    var typeFilter by remember { mutableStateOf<String?>(null) }

    val filtered = remember(stories, query, testamentFilter, typeFilter) {
        stories
            .filter { testamentFilter == null || it.testament == testamentFilter }
            .filter { typeFilter == null || it.storyType == typeFilter }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.moral.contains(query, ignoreCase = true)
            }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.bookGroup } }

    LazyColumn(modifier.padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.story_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.story_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.story_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = testamentFilter == null,
                        onClick = { testamentFilter = null },
                        label = { Text(stringResource(R.string.story_testament_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = testamentFilter == "ot",
                        onClick = { testamentFilter = if (testamentFilter == "ot") null else "ot" },
                        label = { Text(stringResource(R.string.story_testament_ot)) }
                    )
                }
                item {
                    FilterChip(
                        selected = testamentFilter == "nt",
                        onClick = { testamentFilter = if (testamentFilter == "nt") null else "nt" },
                        label = { Text(stringResource(R.string.story_testament_nt)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = typeFilter == null,
                        onClick = { typeFilter = null },
                        label = { Text(stringResource(R.string.story_type_all)) }
                    )
                }
                items(STORY_TYPE_ORDER) { (code, labelRes) ->
                    FilterChip(
                        selected = typeFilter == code,
                        onClick = { typeFilter = if (typeFilter == code) null else code },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (stories.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.story_none_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else if (filtered.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.story_no_matches),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else if (query.isNotBlank()) {
            items(filtered, key = { it.id }) { story ->
                StoryRow(story, onClick = { onStorySelected(story) })
            }
        } else {
            BOOK_GROUP_ORDER.forEach { (groupCode, labelRes) ->
                val inGroup = grouped[groupCode]
                if (!inGroup.isNullOrEmpty()) {
                    item {
                        Text(
                            stringResource(labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(inGroup, key = { it.id }) { story ->
                        StoryRow(story, onClick = { onStorySelected(story) })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun StoryRow(story: StorySummary, onClick: () -> Unit) {
    Column {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp)
        ) {
            Text(story.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                stringResource(storyTypeLabelRes(story.storyType)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                story.moral,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider()
    }
}
