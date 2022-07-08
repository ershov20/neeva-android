package com.neeva.app.feedback

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.api.Optional
import com.neeva.app.Dispatchers
import com.neeva.app.SendFeedbackMutation
import com.neeva.app.apollo.AuthenticatedApolloWrapper
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

        internal fun createMutation(
            user: NeevaUser,
            userFeedback: String,
            url: String?,
            screenshot: Bitmap?
        ): SendFeedbackMutation {
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

            return SendFeedbackMutation(input = input)
        }
    }

    internal var screenshot: Bitmap? = null

    fun submitFeedback(userFeedback: String, url: String?, screenshot: Bitmap?) {
        val sendFeedbackMutation = createMutation(user, userFeedback, url, screenshot)

        viewModelScope.launch {
            withContext(dispatchers.io) {
                apolloWrapper.performMutation(
                    mutation = sendFeedbackMutation,
                    userMustBeLoggedIn = false
                )
            }

            removeScreenshot()
        }
    }

    /**
     * Take a screenshot of the screen.
     *
     * We check if the browser is visible before configuring it to be screenshotable because
     * WebLayer will block while it waits for the Browser to be in the Android View hierarchy.
     * Because the Browser is only attached when the user is in [AppNavDestination.BROWSER], this
     * will prevent this coroutine from completing until the user returns to the browser view.
     */
    fun takeScreenshot(
        isBrowserVisible: Boolean,
        window: Window,
        currentBrowser: BrowserWrapper,
        callback: () -> Unit
    ) = viewModelScope.launch(dispatchers.main) {
        if (isBrowserVisible) {
            currentBrowser.allowScreenshots(true)
        }

        val content = window.decorView
        val bitmap = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)

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

        val scaledBitmap = bitmap.scaleDownMaintainingAspectRatio(MAX_SCREENSHOT_SIZE)
        screenshot = scaledBitmap.takeIf { copyResult }
        callback()

        // https://github.com/neevaco/neeva-android/issues/600
        // We reset the Browser's screenshot mode AFTER the callback() because it can get stuck
        // until the user taps on the screen.  Currently, the only use case is to take a screenshot
        // for the Support screen.  After closing that screen and returning to the browser screen,
        // everything proceeds as it should, but it's unclear why.
        if (isBrowserVisible) {
            currentBrowser.allowScreenshots(false)
        }
    }

    fun removeScreenshot() {
        screenshot = null
    }
}
