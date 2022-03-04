package com.neeva.app.spaces

import com.neeva.app.storage.entities.Space

/** Adds or removes the tab's current URL within the given [Space]. */
fun interface SpaceModifier {
    fun addOrRemoveCurrentTabToSpace(space: Space)
}
