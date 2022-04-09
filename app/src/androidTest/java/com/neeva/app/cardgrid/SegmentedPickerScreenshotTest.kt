package com.neeva.app.cardgrid

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SegmentedPickerScreenshotTest : BaseScreenshotTest() {
    @Test fun lightMode() = runTest(false)
    @Test fun darkMode() = runTest(true)

    private fun runTest(useDarkTheme: Boolean) {
        runScreenshotTest(
            useDarkTheme = useDarkTheme,
            testClass = SegmentedPickerScreenshotTest::class
        ) {
            Column {
                val incognitoScreen = remember { mutableStateOf(SelectedScreen.INCOGNITO_TABS) }
                SegmentedPicker(
                    selectedScreen = incognitoScreen,
                    onSwitchScreen = { incognitoScreen.value = it }
                )

                val regularScreen = remember { mutableStateOf(SelectedScreen.REGULAR_TABS) }
                SegmentedPicker(
                    selectedScreen = regularScreen,
                    onSwitchScreen = { regularScreen.value = it }
                )

                val spacesScreen = remember { mutableStateOf(SelectedScreen.SPACES) }
                SegmentedPicker(
                    selectedScreen = spacesScreen,
                    onSwitchScreen = { spacesScreen.value = it }
                )
            }
        }
    }
}
