package com.neeva.app.zeroQuery

import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateKey
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateModel

enum class ZeroQueryPrefs(
    override val allowCompactState: Boolean,
    override val defaultValue: CollapsingSectionState
) : CollapsingSectionStateKey {
    SuggestedSitesState(true, CollapsingSectionState.COMPACT),
    CommunitySpacesState(false, CollapsingSectionState.EXPANDED),
    SuggestedQueriesState(false, CollapsingSectionState.EXPANDED),
    SpacesState(false, CollapsingSectionState.EXPANDED);
}

internal class ZeroQueryModel(sharedPreferencesModel: SharedPreferencesModel) :
    CollapsingSectionStateModel<ZeroQueryPrefs>(sharedPreferencesModel, SharedPrefFolder.ZERO_QUERY)
