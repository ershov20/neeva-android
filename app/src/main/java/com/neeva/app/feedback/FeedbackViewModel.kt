package com.neeva.app.feedback

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.graphics.applyCanvas
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
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface FeedbackViewModel {
    fun onBackPressed()
    fun onOpenHelpCenterPressed()
    fun onSubmitFeedbackPressed(
        feedback: String,
        shareScreenshot: Boolean,
        url: String?,
        apolloWrapper: ApolloWrapper,
        dispatchers: Dispatchers
    )

    fun createScreenshot(view: View, context: Context)
}

class FeedbackViewModelImpl(
    val appNavModel: AppNavModel,
    val user: NeevaUser,
    val coroutineScope: CoroutineScope,
    val openHelpCenter: () -> Unit
) : FeedbackViewModel {
    var screenshot: String? = null

    override fun onBackPressed() {
        appNavModel.popBackStack()
    }

    override fun onOpenHelpCenterPressed() {
        openHelpCenter()
    }

    override fun onSubmitFeedbackPressed(
        feedback: String,
        shareScreenshot: Boolean,
        url: String?,
        apolloWrapper: ApolloWrapper,
        dispatchers: Dispatchers
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

        val input = SendFeedbackV2Input(
            feedback = Optional.presentIfNotNull(feedback),
            source = Optional.presentIfNotNull(source),
            shareResults = Optional.presentIfNotNull(true),
            userProvidedEmail = Optional.presentIfNotNull(user.data.email)
        )
        val sendFeedbackMutation = SendFeedbackMutation(input = input)

        coroutineScope.launch(dispatchers.io) {
            apolloWrapper.performMutation(sendFeedbackMutation)
        }

        appNavModel.popBackStack()
    }

    override fun createScreenshot(view: View, context: Context) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(
            Runnable {
                val bitmap = Bitmap.createBitmap(
                    view.width, view.height,
                    Bitmap.Config.ARGB_8888
                ).applyCanvas {
                    view.draw(this)
                }

                bitmap.let {
                    val byteOutputStream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.PNG, 100, byteOutputStream)

                    val byteArray = byteOutputStream.toByteArray()
                    screenshot = Base64.getEncoder().encodeToString(byteArray)
                }
            }
        )
    }
}

internal fun getFakeFeedbackViewModel(): FeedbackViewModel {
    return object : FeedbackViewModel {
        override fun onBackPressed() {}
        override fun onOpenHelpCenterPressed() {}
        override fun onSubmitFeedbackPressed(
            feedback: String,
            shareScreenshot: Boolean,
            url: String?,
            apolloWrapper: ApolloWrapper,
            dispatchers: Dispatchers
        ) {}

        override fun createScreenshot(view: View, context: Context) {}
    }
}

private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}
