package com.neeva.app.settings.defaultbrowser

import com.neeva.app.BaseScreenshotTest
import org.junit.Test

class SetDefaultAndroidBrowserPaneScreenshotTest : BaseScreenshotTest() {
    @Test
    fun settingsDefaultAndroidBrowser_PreviewAsDialog() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_PreviewAsDialog()
    }

    @Test
    fun settingsDefaultAndroidBrowser_PreviewAsDialog_Dark() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_PreviewAsDialog_Dark()
    }

    @Test
    fun settingsDefaultAndroidBrowser_PreviewAsDialog_MustOpenSettings() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_PreviewAsDialog_MustOpenSettings()
    }

    @Test
    fun settingsDefaultAndroidBrowser_Dark_PreviewAsDialog_MustOpenSettings() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_Dark_PreviewAsDialog_MustOpenSettings()
    }

    @Test
    fun settingsDefaultAndroidBrowser_Preview() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_Preview()
    }

    @Test
    fun settingsDefaultAndroidBrowser_Dark_Preview() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_Dark_Preview()
    }

    @Test
    fun settingsDefaultAndroidBrowser_Preview_MustOpenSettings() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_Preview_MustOpenSettings()
    }

    @Test
    fun settingsDefaultAndroidBrowser_Dark_Preview_MustOpenSettings() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_Dark_Preview_MustOpenSettings()
    }
}
