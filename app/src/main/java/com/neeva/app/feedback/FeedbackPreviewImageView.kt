package com.neeva.app.feedback

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.ui.FullScreenDialogTopBar

@Composable
fun FeedbackPreviewImageView(
    appNavModel: AppNavModel,
    imageUri: Uri
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        FullScreenDialogTopBar(
            title = stringResource(R.string.submit_feedback_share_screenshot_preview),
            onBackPressed = { appNavModel.popBackStack() }
        )

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberImagePainter(imageUri),
            contentDescription = stringResource(
                R.string.submit_feedback_share_screenshot_preview
            )
        )
    }
}
