package com.neeva.app

import android.graphics.Rect
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText

/**
 * Perform the provided [tapType] on the center of the View via Espresso.
 *
 * WebLayer has no affordance for clicking links on a website, and neither Compose nor Espresso
 * can find links that are displayed by WebLayer.  Chromium works around this by creating a special
 * webpage that displays a big link and clicking on the middle of the website to ensure that it can
 * trigger the correct link:
 * https://source.chromium.org/chromium/chromium/src/+/main:weblayer/browser/android/javatests/src/org/chromium/weblayer/test/EventUtils.java;drc=1946212ac0100668f14eb9e2843bdd846e510a1e;l=14
 */
private fun pressOnCenterOfView(tapType: Tap): ViewAction {
    return GeneralClickAction(
        tapType,
        { view: View ->
            val drawingRect = Rect()
            view.getDrawingRect(drawingRect)

            FloatArray(2).apply {
                set(0, drawingRect.centerX().toFloat())
                set(1, drawingRect.centerY().toFloat())
            }
        },
        Press.FINGER,
        0,
        0
    )
}

/** Long presses on the center of the View containing the WebLayer's Fragment. */
fun longPressOnBrowserView() {
    onView(withId(R.id.weblayer_fragment_view_container)).perform(pressOnCenterOfView(Tap.LONG))
}

/** If the context menu is displayed, perform a click on the menu item with the given id. */
fun selectItemFromContextMenu(itemStringResId: Int) {
    onView(withText(itemStringResId)).perform(ViewActions.click())
}
