package app.pocketbible.ui.reading

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import app.pocketbible.R
import app.pocketbible.data.DailyReading
import app.pocketbible.ui.ResolvedReading

private fun roleHeading(role: String): Int = when (role) {
    "first_reading" -> R.string.reading_role_first_reading
    "psalm" -> R.string.reading_role_psalm
    "second_reading" -> R.string.reading_role_second_reading
    "gospel" -> R.string.reading_role_gospel
    else -> R.string.reading_role_first_reading
}

@Composable
fun DailyReadingScreen(
    dailyReading: DailyReading?,
    readings: List<ResolvedReading>,
    onBack: () -> Unit,
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
            Text(stringResource(R.string.reading_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }

        if (dailyReading == null) {
            Text(
                stringResource(R.string.reading_none_today),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp)
            )
            return@Column
        }

        LazyColumn(Modifier.padding(horizontal = 20.dp)) {
            item { Spacer(Modifier.height(4.dp)) }
            items(readings) { reading ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            stringResource(roleHeading(reading.role)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        if (reading.text.isEmpty()) {
                            Text(
                                stringResource(R.string.reading_text_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        } else {
                            Text(
                                reading.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            reading.citationDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            val reflection = dailyReading.reflection
            if (reflection != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                stringResource(R.string.reading_reflection_heading),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                reflection,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
