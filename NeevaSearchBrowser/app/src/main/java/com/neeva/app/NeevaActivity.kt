package com.neeva.app

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.neeva.app.ui.theme.NeevaSearchBrowserTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.apollographql.apollo.coroutines.await

class NeevaActivity : AppCompatActivity() {
    private val searchTextModel by viewModels<SearchTextModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeevaSearchBrowserTheme {
                Surface(color = MaterialTheme.colors.background) {
                    BrowsingUI(searchTextModel = searchTextModel)
                }
            }
        }

        searchTextModel.text.observe(this) {
            lifecycleScope.launchWhenResumed {
                val response = apolloClient(this@NeevaActivity.applicationContext).query(
                    SuggestionsQuery(query = it)).await()
                android.util.Log.e("Suggestions", "Success ${response?.data}")
            }
        }
    }
}

@Composable
fun BrowsingUI(searchTextModel: SearchTextModel) {
    Column() {
        URLBar(searchTextModel = searchTextModel)
        LoginScreen()
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

@Composable
fun URLBar(searchTextModel: SearchTextModel) {
    val text: String by searchTextModel.text.observeAsState("")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(vertical = 10.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    ) {
        BasicTextField(
            value = text,
            onValueChange = { searchTextModel.onSearchTextChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray)
                .height(42.dp)
                .padding(horizontal = 8.dp)
                .wrapContentSize(Alignment.CenterStart),
            singleLine = true,
            textStyle = MaterialTheme.typography.body1,
        )
    }

}

class SearchTextModel: ViewModel() {
    private val _text = MutableLiveData("")
    val text: LiveData<String> = _text

    fun onSearchTextChanged(newText: String) {
        _text.value = newText
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NeevaSearchBrowserTheme {
        URLBar(searchTextModel = SearchTextModel())
    }
}