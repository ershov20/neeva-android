package com.neeva.app.browsing.toolbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingBar(
    progress: Int,
    modifier: Modifier = Modifier
) {
    if (progress != 100) {
        LinearProgressIndicator(
            progress = progress / 100.0f,
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .then(modifier),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
