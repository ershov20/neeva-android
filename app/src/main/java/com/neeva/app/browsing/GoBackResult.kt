package com.neeva.app.browsing

/** Data needed to process what happens when a user hits back while actively viewing a webpage. */
data class GoBackResult(
    /** If non-null, ID of a tab that should be closed. */
    val tabIdToClose: String? = null,

    /** Query used to produce a bunch of SAYT suggestions that resulted in this navigation. */
    val originalSearchQuery: String? = null,

    /** ID of a Space that the user was viewing when they originally opened this tab. */
    val spaceIdToOpen: String? = null
)
