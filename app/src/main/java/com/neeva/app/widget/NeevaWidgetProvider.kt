// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.neeva.app.MainActivity
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.R

class NeevaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = appWidgetIds.forEach { appWidgetId ->
        updateWidget(context, appWidgetManager, appWidgetId, NeevaConstants().appURL)
    }

    companion object {
        private fun createPendingIntent(context: Context, intent: Intent): PendingIntent {
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            appURL: String
        ) {
            val urlBarIntent = createPendingIntent(
                context,
                Intent(context, MainActivity::class.java).setAction(NeevaActivity.ACTION_NEW_TAB)
            )

            val spacesIntent = createPendingIntent(
                context,
                Intent(
                    context,
                    MainActivity::class.java
                ).setAction(NeevaActivity.ACTION_SHOW_SPACES)
            )

            val homeIntent = createPendingIntent(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse(appURL))
                    .setPackage(context.packageName)
            )

            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setOnClickPendingIntent(R.id.home, homeIntent)
                setOnClickPendingIntent(R.id.spaces, spacesIntent)
                setOnClickPendingIntent(R.id.search_box, urlBarIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
