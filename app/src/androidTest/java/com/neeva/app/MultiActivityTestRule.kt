// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.firstrun.FirstRunActivity
import dagger.hilt.android.testing.HiltTestApplication
import java.lang.ref.WeakReference
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Forces the Neeva app to skip First Run when it starts. */
class MultiActivityTestRule : TestRule {
    val activities = mutableListOf<WeakReference<Activity>>()
    var lastCreatedActivity = WeakReference<Activity>(null)
    var lastForegroundActivity = WeakReference<Activity>(null)

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val application = ApplicationProvider.getApplicationContext() as HiltTestApplication
                application.registerActivityLifecycleCallbacks(
                    object : Application.ActivityLifecycleCallbacks {
                        override fun onActivityCreated(
                            activity: Activity,
                            savedInstanceState: Bundle?
                        ) {
                            WeakReference(activity).apply {
                                activities.add(this)
                                lastCreatedActivity = this
                            }
                        }

                        override fun onActivityStarted(activity: Activity) {
                            lastForegroundActivity = WeakReference(activity)
                        }

                        override fun onActivitySaveInstanceState(
                            activity: Activity,
                            outState: Bundle
                        ) {}

                        override fun onActivityResumed(activity: Activity) {}
                        override fun onActivityPaused(activity: Activity) {}
                        override fun onActivityStopped(activity: Activity) {}
                        override fun onActivityDestroyed(activity: Activity) {}
                    }
                )

                base?.evaluate()
            }
        }
    }

    /**
     * Returns the last Activity that passed the STARTED state.  If that doesn't exist, we return
     * the last Activity that was created.
     */
    fun getLastForegroundActivity(): ComponentActivity? {
        return lastForegroundActivity.get() as? ComponentActivity
            ?: lastCreatedActivity.get() as? ComponentActivity
    }

    /** Returns any NeevaActivity instance that was launched. */
    fun getNeevaActivity(): NeevaActivity? {
        return activities
            .firstOrNull { it.get() is NeevaActivity }
            ?.get() as? NeevaActivity
    }

    /** Returns the FirstRunActivity that was launched, assuming that it exists. */
    fun getFirstRunActivity(): FirstRunActivity? {
        return activities
            .firstOrNull { it.get() is FirstRunActivity }
            ?.get() as? FirstRunActivity
    }
}
