package com.neeva.app.ui.widgets.overlay

enum class OverlaySheetHeightConfig {
    HALF_SCREEN, WRAP_CONTENT
}

class OverlaySheetConfig(sheetHeight: OverlaySheetHeightConfig) {
    val height: OverlaySheetHeightConfig = sheetHeight

    companion object {
        val default: OverlaySheetConfig =
            OverlaySheetConfig(
                sheetHeight = OverlaySheetHeightConfig.WRAP_CONTENT
            )
    }
}
