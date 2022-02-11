package com.neeva.app.spaces

/** Adds or removes the tab's current URL within the given [Space]. */
fun interface SpaceModifier {
    fun addOrRemoveCurrentTabToSpace(space: Space)
}
