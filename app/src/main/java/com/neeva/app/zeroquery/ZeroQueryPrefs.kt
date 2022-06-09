package com.neeva.app.zeroquery

import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPrefKey
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateSharedPref

enum class ZeroQueryPrefs(
    override val allowCompactState: Boolean,
    override val defaultValue: CollapsingSectionState,
    override val sharedPrefKey: SharedPrefKey<String>
) : CollapsingSectionStateSharedPref {
    SuggestedSitesState(
        true,
        CollapsingSectionState.COMPACT,
        SharedPrefFolder.App.ZeroQuerySuggestedSitesState
    ),
    CommunitySpacesState(
        false,
        CollapsingSectionState.EXPANDED,
        SharedPrefFolder.App.ZeroQueryCommunitySpacesState
    ),
    SuggestedQueriesState(
        false,
        CollapsingSectionState.EXPANDED,
        SharedPrefFolder.App.ZeroQuerySuggestedQueriesState
    ),
    SpacesState(
        false,
        CollapsingSectionState.EXPANDED,
        SharedPrefFolder.App.ZeroQuerySpacesState
    );
}
