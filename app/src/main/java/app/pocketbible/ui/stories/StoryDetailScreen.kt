package app.pocketbible.ui.stories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketbible.R
import app.pocketbible.data.CharacterSummary
import app.pocketbible.data.StorySummary
import app.pocketbible.ui.StoryVerseDisplay
import app.pocketbible.ui.bible.localizedBookNameById

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoryDetailScreen(
    story: StorySummary?,
    verses: List<StoryVerseDisplay>,
    relatedCharacters: List<CharacterSummary>,
    onBack: () -> Unit,
    onCharacterSelected: (CharacterSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.read_back))
            }
            Text(story?.title ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }

        LazyColumn(Modifier.padding(horizontal = 20.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                FlowRow {
                    if (story != null) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(if (story.testament == "ot") R.string.story_testament_ot else R.string.story_testament_nt)) }
                        )
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(bookGroupLabelRes(story.bookGroup))) }
                        )
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(storyTypeLabelRes(story.storyType))) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            items(verses) { display ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        if (display.verses.isEmpty()) {
                            Text(
                                stringResource(R.string.character_verse_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        } else {
                            Text(
                                display.verses.joinToString(" ") { it.text },
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        val chapterLabel = if (display.chapterStart == display.chapterEnd) {
                            "${display.chapterStart}:${display.verseStart}-${display.verseEnd}"
                        } else {
                            "${display.chapterStart}:${display.verseStart}-${display.chapterEnd}:${display.verseEnd}"
                        }
                        Text(
                            "${localizedBookNameById(display.bookId)} $chapterLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (story != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(story.summary, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(20.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.story_moral_heading),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                story.moral,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                stringResource(R.string.story_reflection_heading),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                story.reflection,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (relatedCharacters.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.story_related_characters_heading),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow {
                        relatedCharacters.forEach { character ->
                            SuggestionChip(
                                onClick = { onCharacterSelected(character) },
                                label = { Text(character.name) },
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
