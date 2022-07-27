package com.neeva.app.firstrun

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.neeva.app.LocalClientLogger
import com.neeva.app.MainActivity
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.appnav.Transitions
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.settings.setDefaultAndroidBrowser.SetDefaultAndroidBrowserPane
import com.neeva.app.ui.theme.NeevaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FirstRunActivity : AppCompatActivity() {
    companion object {
        const val TAG = "FirstRunActivity"

        internal enum class Destinations {
            LANDING,
            SET_DEFAULT_BROWSER
        }
    }

    @Inject lateinit var clientLogger: ClientLogger
    @Inject lateinit var firstRunModel: FirstRunModel
    @Inject lateinit var neevaConstants: NeevaConstants

    private lateinit var setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager

    private var sendUserToBrowserOnResume: Boolean = false

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clientLogger.logCounter(LogConfig.Interaction.FIRST_RUN_IMPRESSION, null)
        clientLogger.logCounter(LogConfig.Interaction.GET_STARTED_IN_WELCOME, null)

        setDefaultAndroidBrowserManager = SetDefaultAndroidBrowserManager.create(
            activity = this,
            neevaConstants = neevaConstants,
            clientLogger = clientLogger
        )

        setContent {
            NeevaTheme {
                val navHost = rememberAnimatedNavController()
                CompositionLocalProvider(LocalClientLogger provides clientLogger) {
                    AnimatedNavHost(
                        navController = navHost,
                        modifier = Modifier.fillMaxSize(),
                        startDestination = Destinations.LANDING.name,
                        enterTransition = {
                            Transitions.slideIn(this, AnimatedContentScope.SlideDirection.Start)
                        },
                        exitTransition = {
                            Transitions.slideOut(this, AnimatedContentScope.SlideDirection.Start)
                        },
                        popEnterTransition = {
                            Transitions.slideIn(this, AnimatedContentScope.SlideDirection.End)
                        },
                        popExitTransition = {
                            Transitions.slideOut(this, AnimatedContentScope.SlideDirection.End)
                        }
                    ) {
                        composable(Destinations.LANDING.name) {
                            WelcomeScreen(
                                onShowDefaultBrowserSettings = {
                                    navHost.navigate(Destinations.SET_DEFAULT_BROWSER.name)
                                }
                            )
                        }

                        composable(Destinations.SET_DEFAULT_BROWSER.name) {
                            clientLogger.logCounter(
                                LogConfig.Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_IMP,
                                null
                            )

                            SetDefaultAndroidBrowserPane(
                                clientLogger = clientLogger,
                                onBackPressed = { navHost.popBackStack() },
                                openAndroidDefaultBrowserSettings = {
                                    sendUserToBrowserOnResume = true

                                    try {
                                        ContextCompat.startActivity(
                                            this@FirstRunActivity,
                                            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                                            null
                                        )
                                    } catch (e: ActivityNotFoundException) {
                                        Log.e(TAG, "Could not launch settings", e)
                                        sendUserToBrowser()
                                    }
                                },
                                setDefaultAndroidBrowserManager = setDefaultAndroidBrowserManager,
                                showAsDialog = true,
                                onActivityResultCallback = ::sendUserToBrowser
                            ) {
                                sendUserToBrowser()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (sendUserToBrowserOnResume) {
            sendUserToBrowser()
        }
    }

    private fun sendUserToBrowser() {
        // Prevent First Run from showing again.
        firstRunModel.setFirstRunDone()

        if (setDefaultAndroidBrowserManager.isNeevaTheDefaultBrowser()) {
            clientLogger.logCounter(LogConfig.Interaction.SET_DEFAULT_BROWSER, null)
        } else {
            clientLogger.logCounter(LogConfig.Interaction.SKIP_DEFAULT_BROWSER, null)
        }

        val newIntent = intent.apply {
            setClass(this@FirstRunActivity, MainActivity::class.java)

            when (intent.action) {
                null, Intent.ACTION_MAIN -> {
                    // If the user isn't trying to open the app for a specific reason, override the
                    // action so the user sees Zero Query after finishing the First Run flow.
                    action = NeevaActivity.ACTION_ZERO_QUERY
                }
            }
        }

        // Nullify the transition animation to hide the fact that we're switching Activities.
        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0).toBundle()
        ContextCompat.startActivity(this@FirstRunActivity, newIntent, options)
        finish()
    }
}
