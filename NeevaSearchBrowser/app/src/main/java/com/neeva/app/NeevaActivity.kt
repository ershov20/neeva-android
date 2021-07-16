package com.neeva.app

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.neeva.app.ui.theme.NeevaSearchBrowserTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.coroutines.await

class NeevaActivity : AppCompatActivity() {
    private val searchTextModel by viewModels<SearchTextModel>()
    private val suggestionsModel by viewModels<SuggestionsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeevaSearchBrowserTheme {
                Surface(color = MaterialTheme.colors.background) {
                    BrowsingUI(searchTextModel = searchTextModel, suggestionsViewModel = suggestionsModel)
                }
            }
        }

        searchTextModel.text.observe(this) {
            lifecycleScope.launchWhenResumed {
                val response = apolloClient(this@NeevaActivity.applicationContext).query(
                    SuggestionsQuery(query = it)).await()
                if (response.data?.suggest != null) {
                    suggestionsModel.updateWith(response.data?.suggest!!)
                }
            }
        }
    }
}

@Composable
fun BrowsingUI(searchTextModel: SearchTextModel, suggestionsViewModel: SuggestionsViewModel) {
    val showSuggestionList: Boolean? by suggestionsViewModel.shouldShowSuggestions.observeAsState()
    val isEditing: Boolean? by searchTextModel.isEditing.observeAsState()
    Column() {
        URLBar(searchTextModel = searchTextModel)
        Box {
            LoginScreen()
            if (isEditing != false && showSuggestionList != false) {
                SuggestionList(suggestionsViewModel = suggestionsViewModel)
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val webViewClient = WebClient()

    AndroidView(
        factory = {
            WebView(context).apply {
                this.webViewClient = webViewClient
                this.settings.javaScriptEnabled = true
                this.loadUrl("https://neeva.com/")
            }
        },
    )
}
