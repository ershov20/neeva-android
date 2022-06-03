package com.neeva.app.zeroQuery

import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPrefKey
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateModel
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

internal class ZeroQueryModel(
    sharedPreferencesModel: SharedPreferencesModel
) : CollapsingSectionStateModel(sharedPreferencesModel, SharedPrefFolder.App)
