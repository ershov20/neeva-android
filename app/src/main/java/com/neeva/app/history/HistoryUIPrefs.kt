package com.neeva.app.history

import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateKey
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateModel

enum class HistoryUIPrefs(
    override val defaultValue: CollapsingSectionState
) : CollapsingSectionStateKey {
    TodayState(CollapsingSectionState.EXPANDED),
    YesterdayState(CollapsingSectionState.EXPANDED),
    ThisWeekState(CollapsingSectionState.COLLAPSED),
    BeforeThisWeekState(CollapsingSectionState.COLLAPSED);

    override val allowCompactState: Boolean = false
}

internal class HistoryUIModel(sharedPreferencesModel: SharedPreferencesModel) :
    CollapsingSectionStateModel<HistoryUIPrefs>(sharedPreferencesModel, SharedPrefFolder.HISTORY_UI)
