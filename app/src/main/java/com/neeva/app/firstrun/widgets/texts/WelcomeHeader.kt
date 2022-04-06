package com.neeva.app.firstrun.widgets.texts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neeva.app.firstrun.widgets.NeevaLogo

@Composable
fun WelcomeHeader(
    primaryLabel: String,
    secondaryLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        NeevaLogo()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier,
            text = primaryLabel,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (secondaryLabel != null) {
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
