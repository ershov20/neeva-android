// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.spaces

import com.neeva.app.storage.entities.Space

/** Adds or removes the tab's current URL within the given [Space]. */
fun interface SpaceModifier {
    fun addOrRemoveCurrentTabToSpace(space: Space)
}
