package com.neeva.app.settings.sharedComposables.subcomponents

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.storage.toBitmap

@Composable
fun ProfileImage(
    displayName: String?,
    painter: Painter?,
    circlePicture: Boolean,
    showSingleLetterPictureIfAvailable: Boolean
) {
    val regularModifier = Modifier.size(SettingsUIConstants.profilePictureSize)
    val circleClippedModifier = regularModifier.clip(CircleShape)
    when {
        painter != null -> {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = if (circlePicture) circleClippedModifier else regularModifier
            )
        }

        displayName != null && displayName.isNotEmpty() && showSingleLetterPictureIfAvailable -> {
            SingleLetterPicture(displayName, circleClippedModifier)
        }

        else -> {
            DefaultAccountImage(circleClippedModifier)
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
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
private fun DefaultAccountImage(modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(id = R.drawable.ic_default_avatar),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(SettingsUIConstants.profilePictureSize / 2)
            )
        }
    }
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
