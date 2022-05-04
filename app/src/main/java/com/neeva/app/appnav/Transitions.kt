package com.neeva.app.appnav

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

private const val ANIMATION_LENGTH_MS = 250

/** Creates animations for the NavDestination that the user is navigating to. */
@OptIn(ExperimentalAnimationApi::class)
fun enterTransitionFactory(scope: AnimatedContentScope<NavBackStackEntry>): EnterTransition {
    val current = AppNavDestination.fromRouteName(scope.initialState.destination.route)
    val target = AppNavDestination.fromRouteName(scope.targetState.destination.route)

    return when {
        // If the user isn't actually going anywhere, skip the animation.
        current == target -> EnterTransition.None

        target?.fadesOut == true -> {
            fadeIn(animationSpec = tween(ANIMATION_LENGTH_MS))
        }

        target?.slidesInToward != null -> {
            scope.slideIntoContainer(
                towards = target.slidesInToward!!,
                animationSpec = tween(ANIMATION_LENGTH_MS)
            )
        }

        else -> {
            fadeIn(animationSpec = tween(ANIMATION_LENGTH_MS))
        }
    }
}

/** Creates animations for the NavDestination that the user is leaving. */
@OptIn(ExperimentalAnimationApi::class)
fun exitTransitionFactory(scope: AnimatedContentScope<NavBackStackEntry>): ExitTransition {
    val current = AppNavDestination.fromRouteName(scope.initialState.destination.route)
    val target = AppNavDestination.fromRouteName(scope.targetState.destination.route)

    return when {
        // If the user is going to a child of the current destination, just fade it out.  This
        // avoids issues where (e.g.) Settings is being slid to the right while Clear Browsing Data
        // is being slid in from the same direction.
        target?.parent == current -> {
            fadeOut(animationSpec = tween(ANIMATION_LENGTH_MS))
        }

        current?.fadesOut == true -> {
            fadeOut(animationSpec = tween(ANIMATION_LENGTH_MS))
        }

        current?.slidesOutToward != null -> {
            scope.slideOutOfContainer(
                towards = current.slidesOutToward,
                animationSpec = tween(ANIMATION_LENGTH_MS)
            )
        }

        else -> {
            // Default to fading out just to avoid a visual pop.
            fadeOut(animationSpec = tween(ANIMATION_LENGTH_MS))
        }
    }
}

/** Creates animations for the NavDestination that is being returned to after popping the stack. */
@OptIn(ExperimentalAnimationApi::class)
fun popEnterTransitionFactory(scope: AnimatedContentScope<NavBackStackEntry>): EnterTransition {
    return fadeIn(animationSpec = tween(ANIMATION_LENGTH_MS))
}

/** Creates animations for the NavDestination that is being popped off the stack. */
@OptIn(ExperimentalAnimationApi::class)
fun popExitTransitionFactory(scope: AnimatedContentScope<NavBackStackEntry>): ExitTransition {
    return exitTransitionFactory(scope)
}
