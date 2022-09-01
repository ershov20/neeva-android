// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPrefKey
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionStateSharedPref

enum class ZeroQueryPrefs(
    override val sharedPrefKey: SharedPrefKey<CollapsingSectionState>,
    override val allowCompactState: Boolean
) : CollapsingSectionStateSharedPref {
    SuggestedSitesState(SharedPrefFolder.App.ZeroQuerySuggestedSitesState, true),
    CommunitySpacesState(SharedPrefFolder.App.ZeroQueryCommunitySpacesState, false),
    SuggestedQueriesState(SharedPrefFolder.App.ZeroQuerySuggestedQueriesState, false),
    SpacesState(SharedPrefFolder.App.ZeroQuerySpacesState, false);
}
