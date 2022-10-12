// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.content.Intent
import android.os.Bundle
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
    @Inject lateinit var firstRunModel: FirstRunModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityClass = if (firstRunModel.mustShowFirstRun()) {
            FirstRunActivity::class.java
        } else {
            NeevaActivity::class.java
        }

        val newIntent = intent.apply {
            setClass(this@MainActivity, activityClass)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0).toBundle()
        ContextCompat.startActivity(this, newIntent, options)
        finish()
    }
}
