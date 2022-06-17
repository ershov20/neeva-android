package com.neeva.app.ui.widgets.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

enum class MenuItemType {
    HEADER,
    SEPARATOR,
    ACTION
}

data class MenuRowData(
    val type: MenuItemType,
    val id: Int? = null,

    internal val label: String? = null,
    @StringRes internal val labelId: Int? = null,

    internal val secondaryLabel: String? = null,
    @StringRes internal val secondaryLabelId: Int? = null,

    @DrawableRes internal val imageResourceID: Int? = null,
    internal val icon: ImageVector? = null
) {
    companion object {
        fun forHeader(
            primaryLabel: String? = null,
            secondaryLabel: String? = null
        ) = MenuRowData(
            type = MenuItemType.HEADER,
            label = primaryLabel,
            secondaryLabel = secondaryLabel
        )

        fun forSeparator() = MenuRowData(type = MenuItemType.SEPARATOR)

        fun forAction(labelId: Int) = MenuRowData(
            type = MenuItemType.ACTION,
            id = labelId,
            labelId = labelId
        )
    }

    @Composable
    internal fun primaryLabel(): String? {
        return label ?: labelId?.let { labelId -> stringResource(id = labelId) }
    }

    @Composable
    internal fun secondaryLabel(): String? {
        return secondaryLabel ?: secondaryLabelId?.let { labelId -> stringResource(id = labelId) }
    }

    @Composable
    internal fun icon(): Painter? {
        return icon?.let { rememberVectorPainter(image = icon) }
            ?: imageResourceID?.let { painterResource(id = imageResourceID) }
    }
}
