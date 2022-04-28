package com.neeva.app.feedback

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.api.Optional
import com.neeva.app.AuthenticatedApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.SendFeedbackMutation
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.storage.scaleDownMaintainingAspectRatio
import com.neeva.app.storage.toBase64String
import com.neeva.app.type.FeedbackSource
import com.neeva.app.type.SendFeedbackV2Input
import com.neeva.app.userdata.NeevaUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val user: NeevaUser,
    private val apolloWrapper: AuthenticatedApolloWrapper,
    private val dispatchers: Dispatchers
) : ViewModel() {
    companion object {
        /** Used to cap screenshot sizes and prevent sending unreasonably large images. */
        private const val MAX_SCREENSHOT_SIZE = 1500
    }

    var screenshot: Bitmap? = null

    fun submitFeedback(
        userFeedback: String,
        url: String?,
        screenshot: Bitmap?
    ) {
        val source = if (user.isSignedOut()) {
            FeedbackSource.AndroidAppLoggedOut
        } else {
            FeedbackSource.AndroidApp
        }

        val feedback = StringBuilder(userFeedback)
            .apply { if (url != null) append("\n\nCurrent URL: $url") }
            .toString()

        val input = SendFeedbackV2Input(
            feedback = Optional.presentIfNotNull(feedback),
            source = Optional.presentIfNotNull(source),
            shareResults = Optional.presentIfNotNull(true),
            userProvidedEmail = Optional.presentIfNotNull(user.data.email),
            screenshot = Optional.presentIfNotNull(
                screenshot?.toBase64String()
            )
        )

        viewModelScope.launch {
            withContext(dispatchers.io) {
                val sendFeedbackMutation = SendFeedbackMutation(input = input)
                apolloWrapper.performMutation(
                    mutation = sendFeedbackMutation,
                    userMustBeLoggedIn = false
                )
            }

            removeScreenshot()
        }
    }

    fun takeScreenshot(
        window: Window,
        currentBrowser: BrowserWrapper,
        callback: () -> Unit
    ) = viewModelScope.launch(dispatchers.main) {
        val content = window.decorView
        val bitmap = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)

        currentBrowser.allowScreenshots(true)

        val copyResult = suspendCoroutine<Boolean> { continuation ->
            PixelCopy.request(
                window,
                bitmap,
                { copyResult ->
                    when (copyResult) {
                        PixelCopy.SUCCESS -> continuation.resume(true)
                        else -> continuation.resume(false)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }

        currentBrowser.allowScreenshots(false)

        val scaledBitmap = bitmap.scaleDownMaintainingAspectRatio(MAX_SCREENSHOT_SIZE)
        screenshot = scaledBitmap.takeIf { copyResult }

        callback()
    }

    fun removeScreenshot() {
        screenshot = null
    }
}
