package com.neeva.app

/** Identifiers for the possible destinations a user can be sent to via the Navigation library. */
enum class AppNavDestination {
    BROWSER,
    SETTINGS,
    ADD_TO_SPACE,
    NEEVA_MENU,
    HISTORY,
    CARD_GRID,
    FIRST_RUN;

    val route: String = this.name
}
