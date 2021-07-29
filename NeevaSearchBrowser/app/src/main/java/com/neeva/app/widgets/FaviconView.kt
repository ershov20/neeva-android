package com.neeva.app.widgets

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.neeva.app.storage.DomainViewModel


@Composable
fun FaviconView(domainViewModel: DomainViewModel,
                url: Uri, bordered: Boolean = true,) {
    val bitmap: Bitmap? by domainViewModel.getFaviconFor(url).observeAsState(
        domainViewModel.defaultFavicon.value)

    Box(
        modifier = Modifier
            .size(20.dp)
            .then(
                if (bordered) {
                    Modifier.border(
                        1.dp, MaterialTheme.colors.onSecondary,
                        RoundedCornerShape(4.dp)
                    )
                } else Modifier
            ),
        Alignment.Center
    ) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "favicon",
            modifier = Modifier
                .size(16.dp)
                .padding(2.dp),
            contentScale = ContentScale.FillBounds,
        )
    }
}