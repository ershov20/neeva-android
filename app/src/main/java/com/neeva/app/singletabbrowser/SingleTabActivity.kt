// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.singletabbrowser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.appbar.MaterialToolbar
import com.neeva.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity that displays one tab at a time without any ability to switch between them, akin to how
 * a Custom Tab would look.
 *
 * This Activity uses a WebLayer profile that is separate from the ones used by the browser, letting
 * us avoid issues with tab and profile management.  The tradeoff is that we don't get to share the
 * cookie jar with the main app.
 *
 * The browsing experience provided by this Activity is limited because we intend for it to be used
 * only to complete a workflow, like logging in or visiting the help pages.  This means that it only
 * displays one tab without a switche, doesn't allow the user to go into fullscreen mode, doesn't
 * do content filtering, etc.
 */
@AndroidEntryPoint
class SingleTabActivity : AppCompatActivity() {
    internal val viewModel: SingleTabActivityViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var webLayerContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If there is no URL to load, send the user back to the original activity.
        if (intent.data == null) {
            cancelAndFinishActivity()
            return
        }

        setContentView(R.layout.activity_single_tab)
        progressBar = findViewById(R.id.progress_bar)
        toolbar = findViewById(R.id.toolbar)
        webLayerContainer = findViewById(R.id.weblayer_fragment_view_container)

        toolbar.setNavigationOnClickListener {
            cancelAndFinishActivity()
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.initializeWebLayer(
                supportFragmentManager,
                windowMetrics = WindowMetricsCalculator.getOrCreate()
                    .computeCurrentWindowMetrics(this@SingleTabActivity)
            ) { success ->
                if (success) {
                    intent.data?.let { loadUrl(it) }
                } else {
                    cancelAndFinishActivity()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.titleFlow.collectLatest { toolbar.title = it }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.urlFlow.collectLatest { toolbar.subtitle = it }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.progressFlow.collectLatest { progress ->
                progressBar.visibility = if (progress == 100) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        intent = newIntent
        newIntent?.data?.let { loadUrl(it) }
    }

    @Suppress("DEPRECATED")
    override fun onBackPressed() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!viewModel.onBackPressed()) {
                cancelAndFinishActivity()
            }
        }
    }

    private fun cancelAndFinishActivity() {
        setResult(RESULT_CANCELED)
        finishAndRemoveTask()
    }

    private fun loadUrl(url: Uri) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!viewModel.loadUrl(url)) {
                cancelAndFinishActivity()
            }
        }
    }
}
