// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry

fun createAndroidHomeIntent() = Intent()
    .setAction(Intent.ACTION_MAIN)
    .addCategory(Intent.CATEGORY_HOME)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun createMainIntent() =
    InstrumentationRegistry.getInstrumentation().context.packageManager.getLaunchIntentForPackage(
        InstrumentationRegistry.getInstrumentation().targetContext.packageName
    )

fun createLazyTabIntent() = Intent()
    .setAction(NeevaActivity.ACTION_NEW_TAB)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    .setComponent(
        ComponentName(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MainActivity::class.java
        )
    )

fun createSpacesIntent() = Intent()
    .setAction(NeevaActivity.ACTION_SHOW_SPACES)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    .setComponent(
        ComponentName(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MainActivity::class.java
        )
    )

fun createViewIntent(url: String) = Intent()
    .setAction(Intent.ACTION_VIEW)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    .setData(Uri.parse(url))
    .setComponent(
        ComponentName(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MainActivity::class.java
        )
    )
