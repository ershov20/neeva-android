package com.neeva.app

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

@OptIn(ExperimentalAnimationApi::class)
fun exitSlideTransition(
    targetRoute: String,
    slideDirection: AnimatedContentScope.SlideDirection
): (AnimatedContentScope<NavBackStackEntry>) -> ExitTransition? {
    return { scope: AnimatedContentScope<NavBackStackEntry> ->
        when (scope.targetState.destination.route) {
            targetRoute ->
                scope.slideOutOfContainer(
                    slideDirection,
                    animationSpec = tween(500)
                )
            else -> null
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun enterSlideTransition(
    initialRoute: String,
    slideDirection: AnimatedContentScope.SlideDirection
): (AnimatedContentScope<NavBackStackEntry>) -> EnterTransition? {
    return { scope: AnimatedContentScope<NavBackStackEntry> ->
        when (scope.initialState.destination.route) {
            initialRoute ->
                scope.slideIntoContainer(
                    slideDirection,
                    animationSpec = tween(500)
                )
            else -> null
        }
    }
}
