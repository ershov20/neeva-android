package com.neeva.app.ui

import android.view.View
import android.view.ViewGroup

/** Moves the [childView] from its current parent to the [desiredParentView]. */
fun reparentView(
    childView: View?,
    desiredParentView: ViewGroup,
    layoutParams: ViewGroup.LayoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
) {
    when {
        childView == null -> return
        childView.parent == desiredParentView -> return
    }

    removeViewFromParent(childView)
    desiredParentView.addView(childView, layoutParams)
}

/** Removes the given [childView] from its parent. */
fun removeViewFromParent(childView: View?) {
    childView?.let { view ->
        (view.parent as? ViewGroup)?.removeView(view)
    }
}
