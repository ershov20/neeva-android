package com.neeva.app.feedback

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import com.apollographql.apollo3.api.Optional
import com.neeva.app.ApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.SendFeedbackMutation
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.type.FeedbackSource
import com.neeva.app.type.SendFeedbackV2Input
import com.neeva.app.userdata.NeevaUser
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface FeedbackViewModel {
    fun onBackPressed()
    fun onOpenHelpCenterPressed()
    fun onSubmitFeedbackPressed(
        feedback: String,
        imageUri: Uri?,
        url: String?,
        apolloWrapper: ApolloWrapper,
        dispatchers: Dispatchers,
        contentResolver: ContentResolver
    )

    fun showImagePreview(imageUri: Uri)
}

class FeedbackViewModelImpl(
    val appNavModel: AppNavModel,
    val user: NeevaUser,
    val coroutineScope: CoroutineScope,
    val openHelpCenter: () -> Unit
) : FeedbackViewModel {
    var imageUri: Uri = Uri.EMPTY

    override fun onBackPressed() {
        appNavModel.popBackStack()
    }

    override fun onOpenHelpCenterPressed() {
        openHelpCenter()
    }

    override fun onSubmitFeedbackPressed(
        feedback: String,
        imageUri: Uri?,
        url: String?,
        apolloWrapper: ApolloWrapper,
        dispatchers: Dispatchers,
        contentResolver: ContentResolver
    ) {
        val source = if (user.isSignedOut()) {
            FeedbackSource.AndroidAppLoggedOut
        } else {
            FeedbackSource.AndroidApp
        }

        val feedback = if (url != null) {
            feedback + "\n\nCurrent URL: $url"
        } else {
            feedback
        }

        if (imageUri != null) {
            this.imageUri = imageUri
        }

        val input = SendFeedbackV2Input(
            feedback = Optional.presentIfNotNull(feedback),
            source = Optional.presentIfNotNull(source),
            shareResults = Optional.presentIfNotNull(true),
            userProvidedEmail = Optional.presentIfNotNull(user.data.email),
            screenshot = Optional.presentIfNotNull(
                if (imageUri != null) {
                    imgUriToString(imageUri, contentResolver)
                } else {
                    null
                }
            )
        )
        val sendFeedbackMutation = SendFeedbackMutation(input = input)

        coroutineScope.launch(dispatchers.io) {
            apolloWrapper.performMutation(
                mutation = sendFeedbackMutation,
                userMustBeLoggedIn = false
            )
        }

        appNavModel.popBackStack()
    }

    fun imgUriToString(uri: Uri, contentResolver: ContentResolver): String {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        val byteArray = byteArrayOutputStream.toByteArray()
        val baseString = Base64.encodeToString(byteArray, Base64.DEFAULT)

        return baseString
    }

    override fun showImagePreview(imageUri: Uri) {
        this.imageUri = imageUri
        appNavModel.showFeedbackPreviewImage()
    }
}

internal fun getFakeFeedbackViewModel(): FeedbackViewModel {
    return object : FeedbackViewModel {
        override fun onBackPressed() {}
        override fun onOpenHelpCenterPressed() {}
        override fun onSubmitFeedbackPressed(
            feedback: String,
            imageUri: Uri?,
            url: String?,
            apolloWrapper: ApolloWrapper,
            dispatchers: Dispatchers,
            contentResolver: ContentResolver
        ) {}

        override fun showImagePreview(imageUri: Uri) {}
    }
}

private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}
