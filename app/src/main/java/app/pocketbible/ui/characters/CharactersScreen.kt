package app.pocketbible.ui.characters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import app.pocketbible.data.CharacterSummary

/** Category codes in display order, mapped to their header string resource. */
private val CATEGORY_ORDER: List<Pair<String, Int>> = listOf(
    "central" to R.string.character_category_central,
    "holy_family" to R.string.character_category_holy_family,
    "apostles" to R.string.character_category_apostles,
    "early_church" to R.string.character_category_early_church,
    "women_and_others" to R.string.character_category_women_and_others,
    "opposed_jesus" to R.string.character_category_opposed_jesus,
    "patriarchs" to R.string.character_category_patriarchs,
    "exodus_judges" to R.string.character_category_exodus_judges,
    "kingdom" to R.string.character_category_kingdom,
    "prophets" to R.string.character_category_prophets,
    "post_exile" to R.string.character_category_post_exile
)

@Composable
fun CharactersScreen(
    characters: List<CharacterSummary>,
    onCharacterSelected: (CharacterSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(characters, query) {
        if (query.isBlank()) characters
        else characters.filter { it.name.contains(query, ignoreCase = true) }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    LazyColumn(modifier.padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.character_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.character_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.character_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.character_no_matches),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else if (query.isNotBlank()) {
            items(filtered, key = { it.id }) { character ->
                CharacterRow(character, onClick = { onCharacterSelected(character) })
            }
        } else {
            CATEGORY_ORDER.forEach { (categoryCode, labelRes) ->
                val inCategory = grouped[categoryCode]
                if (!inCategory.isNullOrEmpty()) {
                    item {
                        Text(
                            stringResource(labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(inCategory, key = { it.id }) { character ->
                        CharacterRow(character, onClick = { onCharacterSelected(character) })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun CharacterRow(character: CharacterSummary, onClick: () -> Unit) {
    Column {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp)
        ) {
            Text(character.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                character.intro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider()
    }
}
