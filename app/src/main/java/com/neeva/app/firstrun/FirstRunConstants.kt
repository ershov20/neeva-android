package com.neeva.app.firstrun

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import com.neeva.app.R

object FirstRunConstants {

    @Composable
    fun getSubtextStyle(): TextStyle {
        return MaterialTheme.typography.bodyMedium
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    @Composable
    fun getScreenModifier(): Modifier {
        return Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(id = R.dimen.first_run_padding))
    }
}
