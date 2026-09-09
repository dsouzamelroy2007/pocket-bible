package app.pocketbible.ui.stories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pocketbible.R
import app.pocketbible.data.StorySummary
import app.pocketbible.ui.LanguageMenuButton
import app.pocketbible.ui.TooltipIconButton
import app.pocketbible.ui.theme.CategoryAccent
import app.pocketbible.ui.theme.storyTypeAccent
import app.pocketbible.ui.theme.testamentAccent

@Composable
fun StoriesScreen(
    stories: List<StorySummary>,
    onStorySelected: (StorySummary) -> Unit,
    onLanguageSelected: (String?) -> Unit,
    onSavedStoriesClicked: () -> Unit,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.story_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                val savedStoriesLabel = stringResource(R.string.nav_saved)
                TooltipIconButton(text = savedStoriesLabel, onClick = onSavedStoriesClicked) {
                    Icon(Icons.Filled.Bookmark, contentDescription = savedStoriesLabel)
                }
                LanguageMenuButton(onLanguageSelected = onLanguageSelected)
            }
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
                    AccentFilterChip(
                        selected = testamentFilter == null,
                        onClick = { testamentFilter = null },
                        label = stringResource(R.string.story_testament_all),
                        accent = testamentAccent(null)
                    )
                }
                item {
                    AccentFilterChip(
                        selected = testamentFilter == "ot",
                        onClick = { testamentFilter = if (testamentFilter == "ot") null else "ot" },
                        label = stringResource(R.string.story_testament_ot),
                        accent = testamentAccent("ot")
                    )
                }
                item {
                    AccentFilterChip(
                        selected = testamentFilter == "nt",
                        onClick = { testamentFilter = if (testamentFilter == "nt") null else "nt" },
                        label = stringResource(R.string.story_testament_nt),
                        accent = testamentAccent("nt")
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AccentFilterChip(
                        selected = typeFilter == null,
                        onClick = { typeFilter = null },
                        label = stringResource(R.string.story_type_all),
                        accent = storyTypeAccent(null)
                    )
                }
                items(STORY_TYPE_ORDER) { (code, labelRes) ->
                    AccentFilterChip(
                        selected = typeFilter == code,
                        onClick = { typeFilter = if (typeFilter == code) null else code },
                        label = stringResource(labelRes),
                        accent = storyTypeAccent(code)
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
            var isFirstVisibleGroup = true
            BOOK_GROUP_ORDER.forEach { (groupCode, labelRes) ->
                val inGroup = grouped[groupCode]
                if (!inGroup.isNullOrEmpty()) {
                    val topPadding = if (isFirstVisibleGroup) 0.dp else 28.dp
                    isFirstVisibleGroup = false
                    item {
                        BookGroupHeader(
                            stringResource(labelRes),
                            modifier = Modifier.padding(top = topPadding, bottom = 12.dp)
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

/**
 * A testament/story-type filter chip tinted with its own [accent] even when
 * unselected (a soft tonal fill rather than Material3's plain outline), and
 * inverted to a bold fill when selected -- so the row reads as colorful and
 * inviting rather than the black-and-white default look.
 */
@Composable
private fun AccentFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    accent: CategoryAccent
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = accent.container,
            labelColor = accent.onContainer,
            selectedContainerColor = accent.onContainer,
            selectedLabelColor = accent.container
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderWidth = 0.dp,
            selectedBorderWidth = 0.dp
        )
    )
}

/**
 * A book-group divider ("Pentateuch", "Historical", ...) styled as a filled
 * banner rather than plain text, so it reads unmistakably as a section break
 * rather than blending in with the per-story type label (also small and
 * primary-colored) directly below it.
 */
@Composable
private fun BookGroupHeader(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun StoryRow(story: StorySummary, onClick: () -> Unit) {
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
