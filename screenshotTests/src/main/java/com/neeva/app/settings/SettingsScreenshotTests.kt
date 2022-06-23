package com.neeva.app.settings

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.settings.clearBrowsing.ClearBrowsingSettings_Dark_Preview
import com.neeva.app.settings.clearBrowsing.ClearBrowsingSettings_Preview
import com.neeva.app.settings.main.SettingsMain_Dark_Preview
import com.neeva.app.settings.main.SettingsMain_Preview
import com.neeva.app.settings.profile.SettingsProfile_Dark_Preview
import com.neeva.app.settings.profile.SettingsProfile_Preview
import com.neeva.app.settings.setDefaultAndroidBrowser.SettingsDefaultAndroidBrowser_Dark_Preview
import com.neeva.app.settings.setDefaultAndroidBrowser.SettingsDefaultAndroidBrowser_Preview
import org.junit.Test

class SettingsScreenshotTests : BaseScreenshotTest() {
    @Test
    fun clearBrowsingPanePreview_Light() = runScreenshotTest {
        ClearBrowsingSettings_Preview()
    }

    @Test
    fun clearBrowsingPanePreview_Dark() = runScreenshotTest {
        ClearBrowsingSettings_Dark_Preview()
    }

    @Test
    fun mainPanePreview_Light() = runScreenshotTest {
        SettingsMain_Preview()
    }

    @Test
    fun mainPanePreview_Dark() = runScreenshotTest {
        SettingsMain_Dark_Preview()
    }

    @Test
    fun profilePreview_Light() = runScreenshotTest {
        SettingsProfile_Preview()
    }

    @Test
    fun profilePreview_Dark() = runScreenshotTest {
        SettingsProfile_Dark_Preview()
    }

    @Test
    fun setDefaultAndroidBrowserPreview_Light() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_Preview()
    }

    @Test
    fun setDefaultAndroidBrowserPreview_Dark() = runScreenshotTest {
        SettingsDefaultAndroidBrowser_Dark_Preview()
    }
}
