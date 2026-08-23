package app.pocketbible.ui.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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

@Composable
fun SavedScreen(saved: List<EntrySummary>, modifier: Modifier = Modifier) {
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
    LazyColumn(modifier.padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.saved_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
        }
        items(saved, key = { it.entry.id }) { entry ->
            Column(Modifier.padding(vertical = 10.dp)) {
                Text(entry.passageText, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.referenceDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
