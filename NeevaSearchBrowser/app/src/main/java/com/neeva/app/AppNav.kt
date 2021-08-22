package com.neeva.app

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neeva.app.settings.SettingsMain

class AppNavModel: ViewModel() {
    private val _showAppNav = MutableLiveData(false)
    val showAppNav: LiveData<Boolean> = _showAppNav

    lateinit var onOpenUrl: (Uri) -> Unit

    fun setVisibility(show: Boolean) {
        _showAppNav.value = show
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNav(model: AppNavModel) {
    val show: Boolean by model.showAppNav.observeAsState(false)
    if (show) {
        SettingsMain(appNavModel = model)
    }
}
