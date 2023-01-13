package com.neeva.app.settings

import com.neeva.app.BaseScreenshotTest
import com.neeva.app.settings.clearbrowsing.ClearBrowsingSettings_Dark_Preview
import com.neeva.app.settings.clearbrowsing.ClearBrowsingSettings_Preview
import com.neeva.app.settings.defaultbrowser.SettingsDefaultAndroidBrowser_Dark_Preview
import com.neeva.app.settings.defaultbrowser.SettingsDefaultAndroidBrowser_Preview
import com.neeva.app.settings.main.SettingsMain_Dark_Preview
import com.neeva.app.settings.main.SettingsMain_Preview
import com.neeva.app.settings.profile.SettingsProfile_BasicSubscription_Preview
import com.neeva.app.settings.profile.SettingsProfile_BasicSubscription_Preview_Dark
import com.neeva.app.settings.profile.SettingsProfile_PremiumSubscription_Preview
import com.neeva.app.settings.profile.SettingsProfile_PremiumSubscription_Preview_Dark
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
    fun profilePreview_BasicSubscription_Light() = runScreenshotTest {
        SettingsProfile_BasicSubscription_Preview()
    }

    @Test
    fun profilePreview_BasicSubscription_Dark() = runScreenshotTest {
        SettingsProfile_BasicSubscription_Preview_Dark()
    }

    @Test
    fun profilePreview_PremiumSubscription_Light() = runScreenshotTest {
        SettingsProfile_PremiumSubscription_Preview()
    }

    @Test
    fun profilePreview_PremiumSubscription_Dark() = runScreenshotTest {
        SettingsProfile_PremiumSubscription_Preview_Dark()
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
