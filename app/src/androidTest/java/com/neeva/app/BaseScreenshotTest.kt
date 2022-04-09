package com.neeva.app

import android.graphics.BitmapFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.storage.BitmapIO
import com.neeva.app.ui.theme.NeevaTheme
import java.io.BufferedInputStream
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Rule
import strikt.api.expectThat
import strikt.assertions.isEqualTo

abstract class BaseScreenshotTest {
    companion object {
        const val GOLDEN_SCREENSHOT_DIRECTORY = "golden"
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    fun <T : Any> runScreenshotTest(
        useDarkTheme: Boolean,
        testClass: KClass<T>,
        filenameSuffix: String? = null,
        content: @Composable () -> Unit
    ) {
        composeTestRule.setContent {
            NeevaTheme(useDarkTheme = useDarkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    content()
                }
            }
        }

        val imageBitmap = composeTestRule.onRoot().captureToImage()
        val bitmap = imageBitmap.asAndroidBitmap()

        val filename = StringBuilder()
            .apply {
                append(testClass.qualifiedName)

                filenameSuffix?.let { append("_$it") }

                if (useDarkTheme) {
                    append("_dark")
                } else {
                    append("_light")
                }
                append(".png")
            }
            .toString()

        // Save the image out to disk.  This file can be retrieved from the device using
        // Android Studio's "Device File Explorer".
        runBlocking {
            BitmapIO.saveBitmap(
                directory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                dispatchers = Dispatchers(
                    Dispatchers.Main,
                    Dispatchers.Main
                ),
                id = filename,
                bitmap = bitmap
            )
        }

        // Check if the "golden" image exists.
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val goldenFiles = instrumentationContext.assets.list(GOLDEN_SCREENSHOT_DIRECTORY)!!.toList()
        if (!goldenFiles.contains(filename)) {
            fail(
                """
                Could not find golden image.  If this is a new test, then:
                1. Start an emulator using the exact same commands CircleCI uses.  The first command
                   only has to be run once, while the second command starts the emulator.

                   echo "no" | avdmanager --verbose create avd -n "test" -k "system-images;android-28;default;x86_64" -d "pixel_2"
                   emulator -avd test -no-audio -no-boot-anim -verbose -no-snapshot -gpu swiftshader_indirect -partition-size 1536

                2. Run the test by itself.  If you don't, the bitmaps will be deleted by the test
                   runner.
                   
                3. Run this script to copy the file off of the emulator:
                   pull_new_golden_screenshots.sh
            """
            )
        }

        // Compare the golden image against what is being checked in.
        val goldenScreenshotFilename = "$GOLDEN_SCREENSHOT_DIRECTORY/$filename"
        val inputStream = instrumentationContext.assets.open(goldenScreenshotFilename)
        val bufferedStream = BufferedInputStream(inputStream)
        val goldenBitmap = BitmapFactory.decodeStream(bufferedStream)

        expectThat(bitmap.width).isEqualTo(goldenBitmap.width)
        expectThat(bitmap.height).isEqualTo(goldenBitmap.height)

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                expectThat(bitmap.getPixel(x, y)).isEqualTo(goldenBitmap.getPixel(x, y))
            }
        }
    }
}
