package com.neeva.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Patterns
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.webkit.ValueCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.apollographql.apollo.coroutines.await
import com.neeva.app.storage.DomainRepository
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.storage.DomainViewModelFactory
import com.neeva.app.storage.History
import com.neeva.app.suggestions.SuggestionsViewModel
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.urlbar.URLBar
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.urlbar.UrlBarModelFactory
import com.neeva.app.web.WebViewModel
import com.neeva.app.web.WebViewModelFactory
import org.chromium.weblayer.*
import kotlin.math.roundToInt

class NeevaActivity : AppCompatActivity() {
    private val domainsViewModel by viewModels<DomainViewModel> {
        DomainViewModelFactory(DomainRepository(History.db.fromDomains()))
    }
    private val suggestionsModel by viewModels<SuggestionsViewModel>()
    private val webModel by viewModels<WebViewModel> {
        WebViewModelFactory(this, domainsViewModel)
    }
    private val urlBarModel by viewModels<URLBarModel> { UrlBarModelFactory(webModel) }

    private val NON_INCOGNITO_PROFILE_NAME = "DefaultProfile"
    private val EXTRA_START_IN_INCOGNITO = "EXTRA_START_IN_INCOGNITO"

    private lateinit var bottomControls: View

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        findViewById<ComposeView>(R.id.browser_ui).setContent {
            NeevaTheme {
                Surface(color = MaterialTheme.colors.background) {
                    BrowsingUI(urlBarModel, suggestionsModel, webModel, domainsViewModel)
                }
            }
        }
        findViewById<ComposeView>(R.id.browser_ui).background = null

        findViewById<ComposeView>(R.id.omnibox).setContent {
            NeevaTheme {
                Surface(color = MaterialTheme.colors.background) {
                    URLBar(urlBarModel, webModel, domainsViewModel)
                }
            }
        }
        bottomControls = this.layoutInflater.inflate(R.layout.bottom_controls, null)
        bottomControls.findViewById<ComposeView>(R.id.tab_toolbar).setContent {
            NeevaTheme {
                Surface(color = MaterialTheme.colors.background) {
                    TabToolbar(TabToolbarModel({}, {}, {}), webModel)
                }
            }
        }
        try {
            // This ensures asynchronous initialization of WebLayer on first start of activity.
            // If activity is re-created during process restart, FragmentManager attaches
            // BrowserFragment immediately, resulting in synchronous init. By the time this line
            // executes, the synchronous init has already happened.
            WebLayer.loadAsync(
                application
            ) { webLayer: WebLayer ->
                onWebLayerReady(
                    webLayer,
                    savedInstanceState
                )
            }
        } catch (e: UnsupportedVersionException) {
            throw RuntimeException("Failed to initialize WebLayer", e)
        }

        // DB warmup
        History.db

        urlBarModel.text.observe(this) {
            lifecycleScope.launchWhenResumed {
                val response = apolloClient(this@NeevaActivity.applicationContext).query(
                    SuggestionsQuery(query = it.text)).await()
                if (response.data?.suggest != null) {
                    suggestionsModel.updateWith(response.data?.suggest!!)
                }
            }

            lifecycleScope.launchWhenResumed {
                domainsViewModel.textFlow.emit(it.text)
            }
        }

        webModel.currentUrl.observe(this) {
            if (it.isEmpty()) return@observe

            urlBarModel.onCurrentUrlChanged(it)
        }
    }

    private fun getOrCreateBrowserFragment(savedInstanceState: Bundle?): Fragment {
        val fragmentManager = supportFragmentManager
        if (savedInstanceState != null) {
            // FragmentManager could have re-created the fragment.
            val fragments = fragmentManager.fragments
            check(fragments.size <= 1) { "More than one fragment added, shouldn't happen" }
            if (fragments.size == 1) {
                return fragments[0]
            }
        }
        val profileName = NON_INCOGNITO_PROFILE_NAME
        val fragment = WebLayer.createBrowserFragment(profileName)
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.weblayer, fragment)

        // Note the commitNow() instead of commit(). We want the fragment to get attached to
        // activity synchronously, so we can use all the functionality immediately. Otherwise we'd
        // have to wait until the commit is executed.
        transaction.commitNow()
        return fragment
    }

    private fun onWebLayerReady(webLayer: WebLayer, savedInstanceState: Bundle?) {
        if (isFinishing || isDestroyed) return
        webLayer.isRemoteDebuggingEnabled = true
        val fragment: Fragment = getOrCreateBrowserFragment(savedInstanceState)

        webModel.onWebLayerReady(fragment, bottomControls)
    }
}
