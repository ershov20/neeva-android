package com.neeva.app.card

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.R
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.currentDisplayTitle
import com.neeva.app.browsing.currentDisplayUrl
import com.neeva.app.browsing.isSelected
import com.neeva.app.widgets.Button
import com.neeva.app.widgets.FaviconView
import com.neeva.app.zeroQuery.ZeroQueryViewModel
import org.chromium.weblayer.Tab

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CardsContainer(
    appNavModel: AppNavModel,
    webLayerModel: WebLayerModel,
    domainViewModel: DomainViewModel,
    zeroQueryViewModel: ZeroQueryViewModel
) {
    val state: AppNavState by appNavModel.state.observeAsState(AppNavState.HIDDEN)
    AnimatedVisibility(
        visible = state == AppNavState.CARD_GRID,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        CardList(webLayerModel, domainViewModel, appNavModel, zeroQueryViewModel)
    }
}


// TODO Replace this with the proper card grid
@Composable
fun CardList(
    webLayerModel: WebLayerModel,
    domainViewModel: DomainViewModel,
    appNavModel: AppNavModel,
    zeroQueryViewModel: ZeroQueryViewModel
) {
    val tabs: ArrayList<Tab> by webLayerModel.tabList.observeAsState(ArrayList())
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(
                items = tabs,
                key = { tab -> tab.guid }
            ) {
                TabListRow(tabs,
                    tab = it,
                    faviconData = domainViewModel.getFaviconFor(it.currentDisplayUrl)
                ) {
                    webLayerModel.selectTab(it)
                    appNavModel.setContentState(AppNavState.HIDDEN)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(enabled = true, resID = R.drawable.ic_baseline_add_24, contentDescription = "New Tab") {
                zeroQueryViewModel.openLazyTab()
                appNavModel.setContentState(AppNavState.HIDDEN)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(enabled = true, resID = R.drawable.ic_baseline_close_24, contentDescription = "Done") {
                appNavModel.setContentState(AppNavState.HIDDEN)
            }
        }
    }
}

@Composable
fun TabListRow(tabs: ArrayList<Tab>,
    tab: Tab, faviconData: LiveData<Bitmap>, onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                if (tab.isSelected) 3.dp else 1.dp,
                if (tab.isSelected) Color.Companion.Blue else MaterialTheme.colors.onSecondary,
                RoundedCornerShape(12.dp)
            )
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            FaviconView(faviconData)
        }
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1.0f),
            text = tab.currentDisplayTitle ?: tab.currentDisplayUrl.toString(),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onPrimary,
            maxLines = 1,
        )
        Button(enabled = true, resID = R.drawable.ic_baseline_close_24, contentDescription = "Close tab") {
            tabs.remove(tab)
            tab.dispatchBeforeUnloadAndClose()
        }
    }
}