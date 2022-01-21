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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.storage.NeevaUser
import com.neeva.app.ui.theme.NeevaTheme

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileUI(
    id: String? = NeevaUser.shared.id,
    displayName: String? = NeevaUser.shared.displayName,
    email: String? = NeevaUser.shared.email,
    pictureUrl: Uri? = NeevaUser.shared.pictureUrl
) {
    val appNavModel = LocalEnvironment.current.appNavModel
    ProfileUI(id, displayName, email, pictureUrl, appNavModel::showFirstRun)
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileUI(
    id: String? = NeevaUser.shared.id,
    displayName: String? = NeevaUser.shared.displayName,
    email: String? = NeevaUser.shared.email,
    pictureUrl: Uri? = NeevaUser.shared.pictureUrl,
    showFirstRun: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (id == null) {
                    Modifier.clickable { showFirstRun() }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        // TODO(kobec): make this a mutable state boolean after sign-out is implemented
        if (id == null) {
            Text(
                text = stringResource(R.string.settings_sign_in_to_join_neeva),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1.0f)
            )
        } else {
            if (pictureUrl != null) {
                Image(
                    painter = rememberImagePainter(
                        data = pictureUrl,
                        builder = { crossfade(true) }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column {
                if (displayName != null) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (email != null) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// TODO(kobec): previews not working because of LocalEnvironment
@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun ProfileUI_SignedInPreview() {
    NeevaTheme {
        ProfileUI(
            id = "123",
            displayName = "Jehan Kobe Chang",
            email = "kobec@neeva.co",
            pictureUrl = Uri.parse("https://c.neevacdn.net/image/fetch/s"),
            showFirstRun = {}
        )
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun ProfileUI_SignedIn_DarkPreview() {
    NeevaTheme(useDarkTheme = true) {
        ProfileUI(
            id = "123",
            displayName = "Jehan Kobe Chang",
            email = "kobec@neeva.co",
            pictureUrl = null,
            showFirstRun = {}
        )
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun ProfileUI_NoSignInPreview() {
    NeevaTheme {
        ProfileUI(
            id = null,
            displayName = "Jehan Kobe Chang",
            email = "kobec@neeva.co",
            showFirstRun = {}
        )
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun ProfileUI_NoSignIn_DarkPreview() {
    NeevaTheme(useDarkTheme = true) {
        ProfileUI(
            id = null,
            displayName = "Jehan Kobe Chang",
            email = "kobec@neeva.co",
            showFirstRun = {}
        )
    }
}
