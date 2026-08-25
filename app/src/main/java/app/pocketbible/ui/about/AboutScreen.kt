package app.pocketbible.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketbible.R
import app.pocketbible.data.Translation

@Composable
fun AboutScreen(translations: List<Translation>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.about_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.about_translations_heading),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
        }
        items(translations, key = { it.id }) { translation ->
            TranslationCard(translation)
            Spacer(Modifier.height(10.dp))
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TranslationCard(translation: Translation) {
    val uriHandler = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                translation.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${translation.abbreviation} · ${translation.language} · ${translation.license}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            translation.sourceName?.let { source ->
                Spacer(Modifier.height(6.dp))
                Text(
                    source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (translation.sourceUrl != null || translation.licenseUrl != null) {
                Row(Modifier.padding(top = 4.dp)) {
                    translation.sourceUrl?.let { url ->
                        TextButton(onClick = { uriHandler.openUri(url) }) {
                            Text(stringResource(R.string.about_view_source))
                        }
                    }
                    translation.licenseUrl?.let { url ->
                        TextButton(onClick = { uriHandler.openUri(url) }) {
                            Text(stringResource(R.string.about_view_license))
                        }
                    }
                }
            }
        }
    }
}
