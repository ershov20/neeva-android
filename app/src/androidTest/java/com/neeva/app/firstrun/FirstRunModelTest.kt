package com.neeva.app.firstrun

import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.userdata.NeevaUserToken
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@HiltAndroidTest
class FirstRunModelTest : BaseBrowserTest() {
    @Test
    fun skipFirstRunIfSharedPrefSet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Set the shared preference.  First run shouldn't be shown.
        val sharedPreferencesModel = SharedPreferencesModel(context)
        FirstRunModel.firstRunDone(sharedPreferencesModel)

        val intent = Intent.makeMainActivity(ComponentName(context, NeevaActivity::class.java))
        ActivityScenario.launch<NeevaActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity: NeevaActivity ->
                expectThat(activity.appNavModel?.currentDestination?.value?.route)
                    .isEqualTo(AppNavDestination.BROWSER.route)
            }

            scenario.close()
        }
    }

    @Test
    fun skipFirstRunIfUserTokenIsSet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Set the user token.  First run shouldn't be shown.
        val neevaUserToken = NeevaUserToken(SharedPreferencesModel(context), NeevaConstants())
        neevaUserToken.setToken("not a real token, but it's set so first run should get skipped")

        val intent = Intent.makeMainActivity(ComponentName(context, NeevaActivity::class.java))
        ActivityScenario.launch<NeevaActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity: NeevaActivity ->
                expectThat(activity.appNavModel?.currentDestination?.value?.route)
                    .isEqualTo(AppNavDestination.BROWSER.route)
            }

            scenario.close()
        }
    }

    @Test
    fun showFirstRunIfSharedPrefNotSetAndUserTokenIsNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val intent = Intent.makeMainActivity(ComponentName(context, NeevaActivity::class.java))
        ActivityScenario.launch<NeevaActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity: NeevaActivity ->
                expectThat(activity.appNavModel?.currentDestination?.value?.route)
                    .isEqualTo(AppNavDestination.WELCOME.route)
            }

            scenario.close()
        }
    }
}
