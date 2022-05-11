package com.neeva.app

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.storage.BitmapIO
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Rule

/**
 * Base class used to run screenshot comparison tests.
 *
 * "Golden" or "expected" images are stored as assets in the instrumentation test APK.
 * When [runScreenshotTest] runs, the test runs an Activity that draws the Composable being tested.
 * A screenshot of the Composable hierarchy is taken and compared against the golden images.
 *
 * + If the screenshot matches the golden image, the test will pass.
 *
 * + If the golden image doesn't exist, the test will prompt you with instructions for properly
 *   generating the image and adding it to the instrumentation test APK.
 *
 * + If the golden image exists and a difference is detected, the test will fail.
 *     + If this is intentional, then you should follow the instructions to update the golden image
 *       for future tests.  When done correctly, the test should pass the next time you try to run
 *       it.
 *
 *     + If this is not intentional, figure out what changed in the tested Composable and fix it.
 */
abstract class BaseScreenshotTest {
    companion object {
        const val GOLDEN_SCREENSHOT_DIRECTORY = "golden"
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    inline fun runScreenshotTest(
        filenameSuffix: String? = null,
        crossinline content: @Composable () -> Unit
    ) {
        composeTestRule.setContent { content() }

        // Take a screenshot of the hierarchy.
        val imageBitmap = composeTestRule.onRoot().captureToImage()
        val bitmap = imageBitmap.asAndroidBitmap()

        val filename = StringBuilder()
            .apply {
                // It's important that this function is inline in order to allow the screenshot
                // filename to pull the correct test function name.
                val stackTrace = Throwable().stackTrace[0]
                append("${stackTrace.className}_${stackTrace.methodName}")
                filenameSuffix?.let { append("_$it") }
                append(".png")
            }
            .toString()

        // Save the image out to disk.  This file can be retrieved from the device using
        // Android Studio's "Device File Explorer".
        runBlocking {
            val directory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
            BitmapIO.saveBitmap(
                directory = directory,
                bitmapFile = File(directory, filename),
                getOutputStream = ::FileOutputStream,
                bitmap = bitmap
            )
        }

        // Check if the "golden" image exists.
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val goldenFiles = instrumentationContext.assets.list(GOLDEN_SCREENSHOT_DIRECTORY)!!.toList()
        if (!goldenFiles.contains(filename)) {
            failWithInstructions(
                "Couldn't find $filename.  If this is a new test, follow these instructions."
            )
        }

        // Compare the golden image against the screenshot taken by the test.
        val goldenScreenshotFilename = "$GOLDEN_SCREENSHOT_DIRECTORY/$filename"
        val inputStream = instrumentationContext.assets.open(goldenScreenshotFilename)
        val bufferedStream = BufferedInputStream(inputStream)
        val goldenBitmap = BitmapFactory.decodeStream(bufferedStream)

        if (bitmap.width != goldenBitmap.width || bitmap.height != goldenBitmap.height) {
            failWithInstructions("Images are different sizes.")
        }

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                if (bitmap.getPixel(x, y) != goldenBitmap.getPixel(x, y)) {
                    failWithInstructions("Difference at pixel ($x, $y).")
                }
            }
        }
    }

    inline fun failWithInstructions(failureMessage: String) = fail(
        """
        $failureMessage

        If you need to add or update an existing golden image:
        1. Start an emulator using the exact same commands CircleCI uses. 
         
         Only run these commands once to setup:
           sdkmanager "system-images;android-28;default;x86_64"
           echo "no" | avdmanager --verbose create avd -n "test" -k "system-images;android-28;default;x86_64" -d "pixel_2"
        
         This command will actually start the emulator:
           emulator -avd test -no-audio -no-boot-anim -verbose -no-snapshot -gpu swiftshader_indirect -partition-size 2048

        2. Run the test by itself by right clicking on the test function in Android Studio.
           If you don't, the bitmap will be deleted by the test runner before the next test.

        3. Run this script to copy the file off of the emulator and into the golden directory:
           cd neeva-android
           ./pull_new_golden_screenshots.sh

        4. Re-run the test again and make sure it passes.

        Troubleshooting:
        * If you see "Backend Internal error: Exception during IR lowering", rebuild the
          project.  Android Studio _seems_ to be doing some caching somewhere that messes
          with Composable compilation and I haven't figured out why it's happening, yet.
        """
    )
}
