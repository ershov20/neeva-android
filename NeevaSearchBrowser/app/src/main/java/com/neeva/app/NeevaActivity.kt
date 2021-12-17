package com.neeva.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.apollographql.apollo.coroutines.await
import com.neeva.app.browsing.SelectedTabModel
import com.neeva.app.browsing.SelectedTabModelFactory
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.WebViewModelFactory
import com.neeva.app.card.CardViewModel
import com.neeva.app.card.CardViewModelFactory
import com.neeva.app.history.HistoryViewModel
import com.neeva.app.history.HistoryViewModelFactory
import com.neeva.app.neeva_menu.NeevaMenuData
import com.neeva.app.storage.*
import com.neeva.app.suggestions.SuggestionsViewModel
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.urlbar.URLBar
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.urlbar.UrlBarModelFactory
import org.chromium.weblayer.UnsupportedVersionException
import org.chromium.weblayer.WebLayer

class NeevaActivity : AppCompatActivity() {
    companion object {
        private const val NON_INCOGNITO_PROFILE_NAME = "DefaultProfile"
        private const val PERSISTENCE_ID = "Neeva_Browser"
        private const val EXTRA_START_IN_INCOGNITO = "EXTRA_START_IN_INCOGNITO"
    }

    private val domainsViewModel by viewModels<DomainViewModel> {
        DomainViewModelFactory(DomainRepository(History.db.fromDomains()))
    }
    private val historyViewModel by viewModels<HistoryViewModel> {
        HistoryViewModelFactory(SitesRepository(History.db.fromSites()))
    }
    private val suggestionsModel by viewModels<SuggestionsViewModel>()
    private val webModel by viewModels<WebLayerModel> {
        WebViewModelFactory(this, domainsViewModel, historyViewModel)
    }

    private val selectedTabModel by viewModels<SelectedTabModel> {
        SelectedTabModelFactory(
            webModel.selectedTabFlow, webModel::registerNewTab, webModel::createTabFor)
    }

    private val urlBarModel by viewModels<URLBarModel> { UrlBarModelFactory(selectedTabModel) }
    private val cardViewModel by viewModels<CardViewModel> {
        CardViewModelFactory(webModel.tabList)
    }

    private val appNavModel by viewModels<AppNavModel>()

    private lateinit var bottomControls: View

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        findViewById<ComposeView>(R.id.browser_ui).setContent {
            NeevaTheme {
                Surface(color = MaterialTheme.colors.background) {
                    BrowserUI(urlBarModel, suggestionsModel,
                        selectedTabModel, domainsViewModel, historyViewModel)
                }
            }
        }
        findViewById<ComposeView>(R.id.browser_ui).background = null

        findViewById<ComposeView>(R.id.omnibox).setContent {
            NeevaTheme {
                Surface(color = MaterialTheme.colors.background) {
                    URLBar(urlBarModel, domainsViewModel)
                }
            }
        }
        findViewById<ComposeView>(R.id.app_nav).setContent {
            NeevaTheme {
                Surface(color = Color.Transparent) {
                    AppNav(
                        appNavModel, selectedTabModel, historyViewModel,
                        domainsViewModel, webModel, urlBarModel, cardViewModel
                    )
                }
            }
        }
        bottomControls = this.layoutInflater.inflate(R.layout.bottom_controls, null)
        bottomControls.findViewById<ComposeView>(R.id.tab_toolbar).setContent {
            NeevaTheme {
                Surface(color = MaterialTheme.colors.background) {
                    TabToolbar(TabToolbarModel(
                        { appNavModel.setContentState(AppNavState.NEEVA_MENU) },
                        { appNavModel.setContentState(AppNavState.ADD_TO_SPACE) },
                        {
                            webModel.onGridShown()
                            appNavModel.setContentState(AppNavState.CARD_GRID)
                        }
                    ), selectedTabModel)
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

        lifecycleScope.launchWhenResumed {
            NeevaUserInfo.fetch()
        }

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

        selectedTabModel.currentUrl.observe(this) {
            if (it.toString().isEmpty()) return@observe

            urlBarModel.onCurrentUrlChanged(it.toString())
        }

        urlBarModel.autocompletedSuggestion = domainsViewModel.domainsSuggestions.map {
            if (it.isEmpty()) return@map null

            it.first()
        }

        // TODO: Move these to CompositionLocal
        appNavModel.onOpenUrl = { selectedTabModel.loadUrl(it) }
            NeevaMenuData.updateState = appNavModel::setContentState
            NeevaMenuData.loadURL = { selectedTabModel.loadUrl(it) }
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
        val fragment = WebLayer.createBrowserFragment(NON_INCOGNITO_PROFILE_NAME, PERSISTENCE_ID)
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

        webModel.onWebLayerReady(fragment, bottomControls, savedInstanceState)
    }

    override fun onBackPressed() {
        when {
            appNavModel.state.value != null && appNavModel.state.value != AppNavState.HIDDEN -> {
                appNavModel.setContentState(AppNavState.HIDDEN)
            }

            selectedTabModel.canGoBack.value == true -> {
                selectedTabModel.goBack()
            }

            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        webModel.onSaveInstanceState(outState)
    }
}
