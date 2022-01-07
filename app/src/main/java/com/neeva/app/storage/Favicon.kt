package com.neeva.app.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import coil.compose.rememberImagePainter
import com.neeva.app.R
import java.io.ByteArrayOutputStream

/**
 * Stores information about a website's favicon.
 *
 * TODO(dan.alcantara): Delete [encodedImage] when we move to the next version of the database.
 */
data class Favicon(
    val faviconURL: String?,
    val encodedImage: String?,
    val width: Int,
    val height: Int,
) {
    companion object {
        private const val DATA_URI_PREFIX = "data:image/png;base64,"

        /** Returns a [Painter] that can be used to render the favicon. */
        @Composable
        fun Favicon?.toPainter(): Painter {
            return when {
                this?.faviconURL?.startsWith(DATA_URI_PREFIX) == true -> {
                    val byteArray =
                        Base64.decode(faviconURL.drop(DATA_URI_PREFIX.length), Base64.DEFAULT)
                    BitmapPainter(
                        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            .asImageBitmap()
                    )
                }

                this?.faviconURL != null -> {
                    rememberImagePainter(faviconURL) {
                        placeholder(R.drawable.globe)
                        error(R.drawable.globe)
                    }
                }

                else -> {
                    painterResource(R.drawable.globe)
                }
            }
        }

        /** Generates a single-colored Bitmap from the given Uri for debugging. */
        fun Uri?.toFavicon(): Favicon {
            val color = hashCode().or(0xff000000.toInt())
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(color)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray: ByteArray = stream.toByteArray()
            val encoded = DATA_URI_PREFIX + Base64.encodeToString(byteArray, Base64.DEFAULT)

            return Favicon(
                faviconURL = encoded,
                encodedImage = null,
                width = bitmap.width,
                height = bitmap.height
            )
        }
    }
}
