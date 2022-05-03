package com.neeva.app.browsing.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    "Blank Landscape, 1x scale",
    heightDp = 400,
    locale = "en",
    device = Devices.PIXEL_C
)
@Preview(
    "Blank Landscape, RTL, 1x scale",
    heightDp = 400,
    locale = "he",
    device = Devices.PIXEL_C
)
@Composable
fun ToolbarPreview_Blank_Landscape() {
    ToolbarPreview_Blank(true)
}

@Preview(
    "Focus Landscape, 1x scale",
    heightDp = 400,
    locale = "en",
    device = Devices.PIXEL_C
)
@Preview(
    "Focus Landscape, RTL, 1x scale",
    heightDp = 400,
    locale = "he",
    device = Devices.PIXEL_C
)
@Composable
fun ToolbarPreview_Focus_Landscape() {
    ToolbarPreview_Focus(true)
}

@Preview(
    "Typing Landscape, 1x scale",
    heightDp = 400,
    locale = "en",
    device = Devices.PIXEL_C
)
@Preview(
    "Typing Landscape, RTL, 1x scale",
    heightDp = 400,
    locale = "he",
    device = Devices.PIXEL_C
)
@Composable
fun ToolbarPreview_Typing_Landscape() {
    ToolbarPreview_Typing(true)
}

@Preview(
    "Search Landscape, 1x scale",
    heightDp = 400,
    locale = "en",
    device = Devices.PIXEL_C
)
@Preview(
    "Search Landscape, RTL, 1x scale",
    heightDp = 400,
    locale = "he",
    device = Devices.PIXEL_C
)
@Composable
fun ToolbarPreview_Search_Landscape() {
    ToolbarPreview_Search(true)
}

@Preview(
    "Loading Landscape, 1x scale",
    heightDp = 400,
    locale = "en",
    device = Devices.PIXEL_C
)
@Preview(
    "Loading Landscape, RTL, 1x scale",
    heightDp = 400,
    locale = "he",
    device = Devices.PIXEL_C
)
@Composable
fun ToolbarPreview_Loading_Landscape() {
    ToolbarPreview_Loading(useSingleBrowserToolbar = true)
}
