package com.neeva.app.widgets.overlay

enum class OverlaySheetHeightConfig {
    HALF_SCREEN, WRAP_CONTENT
}

class OverlaySheetConfig(sheetHeight: OverlaySheetHeightConfig) {
    val height: OverlaySheetHeightConfig = sheetHeight

    companion object {
        val neevaMenu: OverlaySheetConfig =
            OverlaySheetConfig(
                sheetHeight = OverlaySheetHeightConfig.WRAP_CONTENT
            )

        val spaces: OverlaySheetConfig =
            OverlaySheetConfig(
                sheetHeight = OverlaySheetHeightConfig.HALF_SCREEN
            )

        val default: OverlaySheetConfig =
            OverlaySheetConfig(
                sheetHeight = OverlaySheetHeightConfig.HALF_SCREEN
            )
    }
}
