package app.pocketbible.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.pocketbible.R

@Composable
fun LoadingScreen(
    showFirstSeedMessage: Boolean,
    showLanguageSwitchMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val crossColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    strokeWidth = 3.dp
                )
                Canvas(Modifier.size(24.dp)) {
                    val cross = Path().apply {
                        val centerX = size.width / 2f
                        val stemWidth = size.width * 0.24f
                        val armWidth = size.width * 0.72f
                        val armTop = size.height * 0.28f
                        val armHeight = size.height * 0.18f
                        moveTo(centerX - stemWidth / 2f, 0f)
                        lineTo(centerX + stemWidth / 2f, 0f)
                        lineTo(centerX + stemWidth / 2f, armTop)
                        lineTo(centerX + armWidth / 2f, armTop)
                        lineTo(centerX + armWidth / 2f, armTop + armHeight)
                        lineTo(centerX + stemWidth / 2f, armTop + armHeight)
                        lineTo(centerX + stemWidth / 2f, size.height)
                        lineTo(centerX - stemWidth / 2f, size.height)
                        lineTo(centerX - stemWidth / 2f, armTop + armHeight)
                        lineTo(centerX - armWidth / 2f, armTop + armHeight)
                        lineTo(centerX - armWidth / 2f, armTop)
                        lineTo(centerX - stemWidth / 2f, armTop)
                        close()
                    }
                    drawPath(cross, color = crossColor)
                }
            }
            val message = when {
                showLanguageSwitchMessage -> stringResource(R.string.loading_switching_language)
                showFirstSeedMessage -> stringResource(R.string.loading_first_seed)
                else -> null
            }
            if (message != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}