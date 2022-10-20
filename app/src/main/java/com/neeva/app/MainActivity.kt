// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.neeva.app.firstrun.FirstRunActivity
import com.neeva.app.firstrun.FirstRunModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity that takes incoming Intents and sends them to the right places.
 * If the user has not yet finished First Run, this will send the user there to make sure that they
 * see any required dialogs.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject lateinit var firstRunModel: FirstRunModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityClass = if (firstRunModel.mustShowFirstRun()) {
            FirstRunActivity::class.java
        } else {
            NeevaActivity::class.java
        }

        // Sanitize any Intents we receive from external sources before it gets passed along to the
        // rest of our app.
        val newIntent: Intent = intent.sanitized()
            ?: Intent(this@MainActivity, activityClass).setAction(Intent.ACTION_MAIN)
        newIntent.setClass(this@MainActivity, activityClass)

        // Replace the flags because they apply to MainActivity and not child Activities, (e.g.) we
        // don't want the "exclude from recents" flag being applied to children.
        newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0).toBundle()
        ContextCompat.startActivity(this, newIntent, options)
        finishAndRemoveTask()
    }

    /**
     * Checks to see if trying to read the [Intent]'s extras will cause a crash.
     *
     * Returns the original [Intent] if it is safe to use and null if it isn't.
     */
    private fun Intent?.sanitized(): Intent? {
        return try {
            // Check for parceling/unmarshalling errors, which can happen if an external app sends
            // an Intent that contains a Parcelable we can't handle.  The Bundle will get
            // unmarshalled whenever we try to check for any extras from the Bundle, so it doesn't
            // really matter which we pick.
            also { this?.hasExtra(SearchManager.QUERY) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Intent; discarding", e)
            null
        }
    }
}
