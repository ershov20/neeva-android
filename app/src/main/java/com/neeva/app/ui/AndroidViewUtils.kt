package com.neeva.app.ui

import android.view.View
import android.view.ViewGroup

/**
 * Moves the [childView] from its current parent to the [desiredParentView] if the childView is
 * currently in the Android View hierarchy.
 */
fun reparentView(childView: View, desiredParentView: ViewGroup) {
    if (childView.parent == null || childView.parent == desiredParentView) return

    removeViewFromParent(childView)
    desiredParentView.addView(
        childView,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
}

/** Removes the given [childView] from its parent. */
fun removeViewFromParent(childView: View?) {
    childView?.let { view ->
        (view.parent as? ViewGroup)?.removeView(view)
    }
}
