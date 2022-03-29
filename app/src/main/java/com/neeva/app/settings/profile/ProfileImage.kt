package com.neeva.app.settings.sharedComposables.subcomponents

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.storage.toBitmap
import com.neeva.app.userdata.NeevaUser

@Composable
fun ProfileImage(displayName: String?, painter: Painter?, circlePicture: Boolean) {
    val modifier = Modifier.size(32.dp).then(
        if (circlePicture) {
            Modifier.clip(CircleShape)
        } else {
            Modifier
        }
    )
    when {
        painter != null -> {
            Image(painter = painter, contentDescription = null, modifier = modifier)
        }

        displayName != null && displayName.isNotEmpty() -> {
            SingleLetterPicture(displayName, modifier)
        }

        else -> {
            DefaultAccountPicture(modifier)
        }
    }
}

@Composable
private fun SingleLetterPicture(displayName: String, modifier: Modifier) {
    val bitmap = displayName.toBitmap(0.50f, MaterialTheme.colorScheme.primary.toArgb())
        .asImageBitmap()
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier.fillMaxSize().clip(CircleShape),
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
private fun DefaultAccountPicture(modifier: Modifier) {
    Icon(
        Icons.Rounded.AccountCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun SSOImagePainter(ssoProvider: NeevaUser.SSOProvider): Painter? {
    val painter = when (ssoProvider) {
        NeevaUser.SSOProvider.GOOGLE -> painterResource(id = R.drawable.ic_google)
        NeevaUser.SSOProvider.MICROSOFT -> painterResource(id = R.drawable.ic_microsoft)
        NeevaUser.SSOProvider.OKTA -> painterResource(id = R.drawable.ic_neeva_logo)
        else -> null
    }
    return painter
}

@Composable
fun PictureUrlPainter(pictureURI: Uri?): Painter? {
    if (pictureURI == null) {
        return null
    }
    return rememberImagePainter(
        data = pictureURI,
        builder = { crossfade(true) }
    )
}
