package com.neeva.app.browsing.toolbar

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R

/** Icon used to represent the tab switcher or the regular tab grid. */
@Composable
fun TabSwitcherIcon(contentDescription: String) {
    Icon(
        painter = painterResource(R.drawable.ic_baseline_filter_none_24),
        contentDescription = contentDescription,
        modifier = Modifier.size(20.dp)
    )
}
