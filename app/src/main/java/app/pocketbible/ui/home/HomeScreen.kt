package app.pocketbible.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketbible.R
import app.pocketbible.data.DailyReading
import app.pocketbible.data.Feeling
import app.pocketbible.data.Passage
import app.pocketbible.ui.theme.categoryAccent

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

@Composable
fun HomeScreen(
    feelings: List<Feeling>,
    searchQuery: String,
    searchResults: List<Feeling>,
    verseOfDay: Passage?,
    dailyReading: DailyReading?,
    onSearchQueryChange: (String) -> Unit,
    onFeelingSelected: (Feeling) -> Unit,
    onLanguageSelected: (String?) -> Unit,
    onSavedClicked: () -> Unit,
    onAboutClicked: () -> Unit,
    onDailyReadingClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.home_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onSavedClicked) {
                Icon(Icons.Filled.Bookmark, contentDescription = stringResource(R.string.nav_saved))
            }
            IconButton(onClick = onAboutClicked) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.about_title))
            }
            LanguageMenuButton(onLanguageSelected = onLanguageSelected)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (searchQuery.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            if (searchResults.isEmpty()) {
                Text(
                    stringResource(R.string.home_no_matches),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    stringResource(R.string.home_matches),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                searchResults.forEach { feeling ->
                    val accent = categoryAccent(feeling.category)
                    Card(
                        onClick = { onFeelingSelected(feeling) },
                        colors = CardDefaults.cardColors(containerColor = accent.container),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                feeling.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = accent.onContainer
                            )
                            Text(
                                feeling.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = accent.onContainer.copy(alpha = 0.75f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.home_i_am_feeling), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(feelings, key = { it.id }) { feeling ->
                FeelingCard(feeling, onClick = { onFeelingSelected(feeling) })
            }
        }

        verseOfDay?.let { verse ->
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.home_verse_of_day),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        verse.pullQuote ?: verse.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        verse.referenceDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        dailyReading?.let {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDailyReadingClicked)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.home_daily_reading),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LanguageMenuButton(onLanguageSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val systemDefaultLabel = stringResource(R.string.language_system_default)
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Language, contentDescription = stringResource(R.string.language_button))
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

@Composable
private fun FeelingCard(feeling: Feeling, onClick: () -> Unit) {
    val accent = categoryAccent(feeling.category)
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = accent.container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                feeling.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = accent.onContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                feeling.description,
                style = MaterialTheme.typography.bodySmall,
                color = accent.onContainer.copy(alpha = 0.75f),
                maxLines = 2
            )
        }
    }
}
