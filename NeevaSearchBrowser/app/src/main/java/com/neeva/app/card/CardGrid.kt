package com.neeva.app.card

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.neeva.app.AppNavModel
import com.neeva.app.AppNavState
import com.neeva.app.R
import com.neeva.app.browsing.BrowserPrimitive
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.urlbar.URLBarModel
import com.neeva.app.widgets.Button
import com.neeva.app.widgets.FaviconView

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CardsContainer(
    appNavModel: AppNavModel,
    webLayerModel: WebLayerModel,
    domainViewModel: DomainViewModel,
    urlBarModel: URLBarModel,
    cardViewModel: CardViewModel
) {
    val state: AppNavState by appNavModel.state.observeAsState(AppNavState.HIDDEN)
    AnimatedVisibility(
        visible = state == AppNavState.CARD_GRID,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        CardGrid(webLayerModel, domainViewModel, appNavModel, urlBarModel, cardViewModel)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardGrid(
    webLayerModel: WebLayerModel,
    domainViewModel: DomainViewModel,
    appNavModel: AppNavModel,
    urlBarModel: URLBarModel,
    cardViewModel: CardViewModel
) {
    val tabs: List<BrowserPrimitive> by webLayerModel.tabList.observeAsState(ArrayList())
    val listState: LazyListState by cardViewModel.listState.observeAsState(LazyListState())

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.background)) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            state = listState,
            contentPadding = PaddingValues(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(tabs) { tab ->
                val favicon: Bitmap? by domainViewModel.getFaviconFor(tab.url).observeAsState()

                TabCard(
                    tab = tab,
                    faviconData = favicon,
                    onSelect = {
                        webLayerModel.select(tab)

                        appNavModel.setContentState(AppNavState.HIDDEN)
                    },
                    onClose = { webLayerModel.close(tab) })
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.LightGray))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(enabled = true, resID = R.drawable.ic_baseline_add_24, contentDescription = "New Tab") {
                urlBarModel.openLazyTab()
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
fun TabCard(
    tab: BrowserPrimitive,
    faviconData: Bitmap?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Box(modifier = Modifier
            .then(
                if (tab.isSelected)
                    Modifier.border(
                        3.dp,
                        Color.Companion.Blue,
                        RoundedCornerShape(12.dp)
                    ) else Modifier
            )
            ) {
            Image(
                painter = rememberImagePainter(
                    data = tab.thumbnailUri,
                    builder = {
                        crossfade(true)
                    }),
                contentDescription = "query image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .width(156.dp)
                    .height(200.dp)
                    .shadow(2.dp, shape = RoundedCornerShape(12.dp))
                    .clickable { onSelect() }
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
                    .background(Color.LightGray, shape = CircleShape)
                    .clickable { onClose() }
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_close_24),
                    contentDescription = "Close tab",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier
                        .size(16.dp, 16.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(156.dp)
                .clip(RoundedCornerShape(12.dp))

        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                FaviconView(faviconData)
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1.0f),
                text = tab.title ?: tab.url.toString(),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
            )
        }
    }

}