package com.neeva.app.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.storage.NeevaUser
import com.neeva.app.ui.theme.NeevaTheme

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileRow(
    primaryLabel: String? = NeevaUser.shared.displayName,
    secondaryLabel: String? = NeevaUser.shared.email,
    pictureUrl: Uri? = NeevaUser.shared.pictureUrl,
    onClick: (() -> Unit)? = null,
    modifier: Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        if (pictureUrl != null) {
            Image(
                painter = rememberImagePainter(
                    data = pictureUrl,
                    builder = { crossfade(true) }
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column {
            if (primaryLabel != null) {
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(name = "Profile UI Signed In, 1x font size", locale = "en")
@Preview(name = "Profile UI Signed In, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun ProfileUI_SignedInPreview() {
    NeevaTheme {
        ProfileRow(
            primaryLabel = "Jehan Kobe Chang",
            secondaryLabel = "kobec@neeva.co",
            pictureUrl = Uri.parse("https://c.neevacdn.net/image/fetch/s"),
            onClick = {},
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview(name = "Profile UI Signed In Dark, 1x font size", locale = "en")
@Preview(name = "Profile UI Signed In Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun ProfileUI_SignedIn_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        ProfileRow(
            primaryLabel = "Jehan Kobe Chang",
            secondaryLabel = "kobec@neeva.co",
            pictureUrl = null,
            onClick = {},
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
