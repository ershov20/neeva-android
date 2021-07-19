package com.neeva.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import com.neeva.app.ui.theme.NeevaTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.*
import com.apollographql.apollo.coroutines.await
import com.neeva.app.storage.DomainRepository
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.storage.DomainViewModelFactory
import com.neeva.app.storage.History
import com.neeva.app.suggestions.SuggestionList
import com.neeva.app.suggestions.SuggestionsViewModel
import com.neeva.app.urlbar.URLBar
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.urlbar.UrlBarModelFactory
import com.neeva.app.web.WebPanel
import com.neeva.app.web.WebViewModel
import com.neeva.app.web.WebViewModelFactory

class NeevaActivity : AppCompatActivity() {
    private val domainsViewModel by viewModels<DomainViewModel> {
        DomainViewModelFactory(DomainRepository(History.db.fromDomains()))
    }
    private val suggestionsModel by viewModels<SuggestionsViewModel>()
    private val webModel by viewModels<WebViewModel> {
        WebViewModelFactory(this, domainsViewModel)
    }
    private val searchTextModel by viewModels<URLBarModel> { UrlBarModelFactory(webModel) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeevaTheme {
                Surface(color = MaterialTheme.colors.background) {
                    BrowsingUI(searchTextModel, suggestionsModel, webModel, domainsViewModel)
                }
            }
        }

        // DB warmup
        History.db

        searchTextModel.text.observe(this) {

            lifecycleScope.launchWhenResumed {
                val response = apolloClient(this@NeevaActivity.applicationContext).query(
                    SuggestionsQuery(query = it)).await()
                if (response.data?.suggest != null) {
                    suggestionsModel.updateWith(response.data?.suggest!!)
                }
            }
        }

        webModel.currentUrl.observe(this) {
            if (it.isEmpty()) return@observe

            searchTextModel.onCurrentUrlChanged(it)
        }
    }
}

@Composable
fun BrowsingUI(urlBarModel: URLBarModel,
               suggestionsViewModel: SuggestionsViewModel,
               webViewModel: WebViewModel,
               domainViewModel: DomainViewModel,
) {
    val isEditing: Boolean? by urlBarModel.isEditing.observeAsState()
    Column {
        URLBar(urlBarModel = urlBarModel, webViewModel, domainViewModel)
        Box(modifier = Modifier.weight(1.0f)) {
            WebPanel(webViewModel)
            if (isEditing != false) {
                SuggestionList(suggestionsViewModel, urlBarModel, webViewModel, domainViewModel)
            }
        }
        TabToolbar(
            TabToolbarModel(
                {},
                {},
                {}
            ),
            webViewModel
        )
    }
}
