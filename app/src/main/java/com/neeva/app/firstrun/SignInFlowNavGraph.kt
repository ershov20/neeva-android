package com.neeva.app.firstrun

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.LocalNavHostController
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.appnav.Transitions
import com.neeva.app.firstrun.signin.SignInScreenContainer
import com.neeva.app.firstrun.signup.SignUpLandingContainer
import com.neeva.app.firstrun.signup.SignUpWithOtherContainer
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig

enum class SignInFlowNavDestination {
    SIGN_UP_LANDING_PAGE, SIGN_UP_OTHER, SIGN_IN;

    val route: String get() { return name }
}

/** Manages navigation between the different screens of the sign-in flow. */
class SignInFlowNavModel(
    private val appNavModel: AppNavModel,
    private val clientLogger: ClientLogger,
    private val navController: NavController
) {
    fun navigateBackToSignUpLandingPage() {
        clientLogger.logCounter(LogConfig.Interaction.AUTH_IMPRESSION_LANDING, null)
        navController.navigate(SignInFlowNavDestination.SIGN_UP_LANDING_PAGE.route) {
            launchSingleTop = true

            // Keep the back stack shallow by popping everything off back to the root when returning
            // to the landing page.
            popUpTo(AppNavDestination.SIGN_IN_FLOW.route)
        }
    }

    fun navigateToSignIn() {
        clientLogger.logCounter(LogConfig.Interaction.AUTH_IMPRESSION_SIGN_IN, null)
        navController.navigate(SignInFlowNavDestination.SIGN_IN.route) {
            launchSingleTop = true
        }
    }

    fun navigateToSignUpWithOther() {
        clientLogger.logCounter(LogConfig.Interaction.AUTH_IMPRESSION_OTHER, null)
        navController.navigate(SignInFlowNavDestination.SIGN_UP_OTHER.route) {
            launchSingleTop = true
        }
    }

    fun exitSignInFlow() {
        appNavModel.showBrowser()
    }
}

@Composable
fun rememberSignInFlowNavModel(): SignInFlowNavModel {
    val appNavModel = LocalAppNavModel.current
    val localEnvironment = LocalEnvironment.current
    val navController = LocalNavHostController.current

    return remember(appNavModel, localEnvironment, navController) {
        SignInFlowNavModel(appNavModel, localEnvironment.clientLogger, navController)
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.signInFlowNavGraph() {
    navigation(
        startDestination = SignInFlowNavDestination.SIGN_UP_LANDING_PAGE.route,
        route = AppNavDestination.SIGN_IN_FLOW.route
    ) {
        composable(
            route = SignInFlowNavDestination.SIGN_UP_LANDING_PAGE.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { Transitions.fadeOutLambda() }
        ) {
            val firstRunModel = LocalFirstRunModel.current
            val signInFlowNavModel = rememberSignInFlowNavModel()
            SignUpLandingContainer(
                launchLoginIntent = firstRunModel.getLaunchLoginIntent(LocalContext.current),
                openInCustomTabs = firstRunModel.openInCustomTabs(LocalContext.current),
                onClose = firstRunModel.getOnCloseOnboarding(signInFlowNavModel::exitSignInFlow),
                navigateToSignIn = signInFlowNavModel::navigateToSignIn,
                showSignUpWithOther = signInFlowNavModel::navigateToSignUpWithOther
            )
        }

        composable(
            route = SignInFlowNavDestination.SIGN_UP_OTHER.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            val firstRunModel = LocalFirstRunModel.current
            val signInFlowNavModel = rememberSignInFlowNavModel()
            SignUpWithOtherContainer(
                launchLoginIntent = firstRunModel.getLaunchLoginIntent(LocalContext.current),
                onClose = firstRunModel.getOnCloseOnboarding(signInFlowNavModel::exitSignInFlow),
                navigateToSignIn = signInFlowNavModel::navigateToSignIn
            )
        }

        composable(
            route = SignInFlowNavDestination.SIGN_IN.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            val firstRunModel = LocalFirstRunModel.current
            val signInFlowNavModel = rememberSignInFlowNavModel()
            SignInScreenContainer(
                launchLoginIntent = firstRunModel.getLaunchLoginIntent(LocalContext.current),
                onClose = firstRunModel.getOnCloseOnboarding(signInFlowNavModel::exitSignInFlow),
                navigateToSignUp = signInFlowNavModel::navigateBackToSignUpLandingPage
            )
        }
    }
}
