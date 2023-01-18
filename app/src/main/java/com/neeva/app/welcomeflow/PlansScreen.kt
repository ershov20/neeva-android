// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.LocalSubscriptionManager
import com.neeva.app.R
import com.neeva.app.billing.BillingSubscriptionPlanTags.ANNUAL_PREMIUM_PLAN
import com.neeva.app.billing.BillingSubscriptionPlanTags.FREE_PLAN
import com.neeva.app.billing.BillingSubscriptionPlanTags.MONTHLY_PREMIUM_PLAN
import com.neeva.app.firstrun.FirstRunConstants
import com.neeva.app.firstrun.widgets.texts.ToggleSignUpText
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.AnnotatedSpannable
import timber.log.Timber

@Composable
fun PlansScreen(
    navigateToSignIn: () -> Unit,
    onSelectSubscriptionPlan: (String) -> Unit,
    onBack: (() -> Unit)?,
    showSignInText: Boolean,
    showFreePlan: Boolean = true
) {
    val subscriptionManager = LocalSubscriptionManager.current
    val subscriptionOfferDetails = subscriptionManager.productDetailsWrapperFlow
        .collectAsState().value
        .productDetails?.subscriptionOfferDetails
    val selectedSubscriptionTag = subscriptionManager.selectedSubscriptionTagFlow
        .collectAsState().value

    // This screen should only be opened if there are available subscription
    // plans for this device.
    if (subscriptionOfferDetails.isNullOrEmpty()) {
        Timber.e(
            "PLANS Screen should not be opened when there are " +
                "no available subscriptions to purchase!"
        )
    } else {
        val subscriptionPlans = PlansScreenData.getSubscriptionPlans(
            subscriptionOfferDetails = subscriptionOfferDetails,
            showFreePlan = showFreePlan
        )
        val indexOfSelectedPlan = subscriptionPlans
            .indexOfFirst { it.tag == selectedSubscriptionTag }

        val initialTabIndex = when {
            indexOfSelectedPlan != -1 -> indexOfSelectedPlan
            showFreePlan -> 1
            else -> 0
        }

        PlansScreen(
            subscriptionPlans = subscriptionPlans,
            onSelectSubscriptionPlan = onSelectSubscriptionPlan,
            navigateToSignIn = navigateToSignIn,
            initialTabIndex = initialTabIndex,
            showSignInText = showSignInText,
            onBack = onBack,
        )
    }
}

@Composable
internal fun PlansScreen(
    initialTabIndex: Int = 1,
    subscriptionPlans: List<SubscriptionPlan>,
    onSelectSubscriptionPlan: (String) -> Unit,
    navigateToSignIn: () -> Unit,
    showSignInText: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    WelcomeFlowContainer(
        headerText = stringResource(id = R.string.welcomeflow_plans_header),
        onBack = onBack
    ) {
        PlansScreenContent(
            initialTabIndex = initialTabIndex,
            subscriptionPlans = subscriptionPlans,
            onSelectSubscriptionPlan = onSelectSubscriptionPlan,
            navigateToSignIn = navigateToSignIn,
            showSignInText = showSignInText,
            modifier = it
        )
    }
}

@Composable
fun PlansScreenContent(
    initialTabIndex: Int,
    subscriptionPlans: List<SubscriptionPlan>,
    onSelectSubscriptionPlan: (String) -> Unit,
    navigateToSignIn: () -> Unit,
    showSignInText: Boolean = true,
    modifier: Modifier
) {
    var selectedTabIndex by remember {
        mutableStateOf(initialTabIndex.coerceIn(0, subscriptionPlans.size - 1))
    }

    val subscriptionPlan = subscriptionPlans[selectedTabIndex]

    Column(modifier) {
        Spacer(Modifier.height(24.dp))
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                // Jetpack Compose's default Tab implementation doesn't follow Material3's guideline
                // to make the indicator width the same width as the Tab content. Instead, it
                // decides to make the indicator fill the entire width of the Tab itself.

                // In trying to reach parity with design, we decided to make the indicator width
                // a fixed length of 67.dp. If 67.dp is larger than the Tab width itself,
                // it will default to just filling the entire width of the Tab.
                val currentTabPosition = tabPositions[selectedTabIndex]
                Surface(
                    color = LocalContentColor.current,
                    shape = RoundedCornerShape(
                        topStart = 100.dp,
                        topEnd = 100.dp
                    ),
                    modifier = Modifier.composed {
                        val indicatorOffset by animateDpAsState(
                            targetValue = currentTabPosition.left,
                            animationSpec = tween(
                                durationMillis = 250,
                                easing = FastOutSlowInEasing
                            )
                        )

                        val indicatorSize = 67.dp
                        val withinTabOffset = (currentTabPosition.width - indicatorSize) / 2

                        fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .then(
                                if (indicatorSize > currentTabPosition.width) {
                                    Modifier
                                        .offset(x = indicatorOffset)
                                        .width(currentTabPosition.width)
                                } else {
                                    Modifier
                                        .offset(x = indicatorOffset + withinTabOffset)
                                        .width(indicatorSize)
                                }
                            )
                            .height(3.dp)
                    }
                ) {}
            }
        ) {
            subscriptionPlans.forEachIndexed { index, subscriptionPlan ->
                Tab(
                    selected = selectedTabIndex == index,
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { selectedTabIndex = index },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = subscriptionPlan.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        val monthlyPrice = subscriptionPlan.price?.monthlyPrice
                        if (monthlyPrice != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.welcomeflow_per_month,
                                    monthlyPrice
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        subscriptionPlan.benefits.forEach {
            PlansBenefit(title = it.title, description = it.description)
            Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))
        }

        Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))

        SubscriptionInfo(
            subscriptionPlan = subscriptionPlan,
            onClickContinue = {
                onSelectSubscriptionPlan(subscriptionPlan.tag)
            }
        )

        Spacer(Modifier.size(Dimensions.PADDING_SMALL))

        SubscribeText()

        Spacer(Modifier.size(Dimensions.PADDING_LARGE))

        if (showSignInText) {
            ToggleSignUpText(signup = true, onClick = navigateToSignIn)
        }
    }
}

@Composable
private fun SubscriptionInfo(
    subscriptionPlan: SubscriptionPlan,
    onClickContinue: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        when (subscriptionPlan.tag) {
            FREE_PLAN -> {
                Text(text = subscriptionPlan.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(28.dp))
                WelcomeFlowButton(
                    primaryText = stringResource(id = R.string.welcomeflow_get_free_plan),
                    onClick = onClickContinue
                )
            }

            ANNUAL_PREMIUM_PLAN -> {
                Row {
                    val annualPrice = subscriptionPlan.price?.annualPrice ?: ""
                    val yearlySubscriptionPrice = stringResource(
                        id = R.string.welcomeflow_yearly_subscription_period,
                        annualPrice
                    )
                    PriceText(
                        price = annualPrice,
                        formattedText = yearlySubscriptionPrice
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = stringResource(
                            id = R.string.welcomeflow_annual_subscription_savings
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = stringResource(R.string.dot_separator),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = Dimensions.PADDING_TINY)
                    )
                    Text(
                        text = stringResource(id = R.string.welcomeflow_cancel_anytime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            MONTHLY_PREMIUM_PLAN -> {
                Row {
                    val monthlyPrice = subscriptionPlan.price?.monthlyPrice ?: ""
                    val monthlySubscriptionPrice = stringResource(
                        id = R.string.welcomeflow_monthly_subscription_period,
                        monthlyPrice
                    )
                    PriceText(
                        price = monthlyPrice,
                        formattedText = monthlySubscriptionPrice
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.welcomeflow_cancel_anytime),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (subscriptionPlan.tag != FREE_PLAN) {
            Spacer(Modifier.height(Dimensions.PADDING_LARGE))
            WelcomeFlowButton(
                primaryText = stringResource(id = R.string.welcomeflow_try_it_free),
                secondaryText = stringResource(id = R.string.welcomeflow_free_trial),
                onClick = onClickContinue
            )
        }

        Spacer(Modifier.height(Dimensions.PADDING_LARGE))
    }
}

@Composable
private fun SubscribeText() {
    val context = LocalContext.current
    val firstRunModel = LocalFirstRunModel.current
    val openURL: (Uri) -> Unit = { uri -> firstRunModel.openSingleTabActivity(context, uri) }
    val climatePledgeURL = LocalNeevaConstants.current.climatePledgeURL

    AnnotatedSpannable(
        rawHtml = stringResource(
            R.string.welcomeflow_subscribe_and_fight_climate_change,
            climatePledgeURL
        ),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center),
        textStyle = FirstRunConstants.getSubtextStyle().copy(
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    ) { annotatedString, offset ->
        annotatedString.getStringAnnotations(
            tag = climatePledgeURL,
            start = offset,
            end = offset
        )
            .firstOrNull()
            ?.let {
                openURL(Uri.parse(it.item))
                return@AnnotatedSpannable
            }
    }
}

@PortraitPreviews
@Composable
fun PlansScreen_Free_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        PlansScreen(
            initialTabIndex = 0,
            subscriptionPlans = PlansScreenData.getPreviewSubscriptionPlans(),
            onSelectSubscriptionPlan = {},
            navigateToSignIn = {}
        )
    }
}

@PortraitPreviews
@Composable
fun PlansScreen_Free_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        PlansScreen(
            initialTabIndex = 0,
            subscriptionPlans = PlansScreenData.getPreviewSubscriptionPlans(),
            onSelectSubscriptionPlan = {},
            navigateToSignIn = {}
        )
    }
}

@PortraitPreviews
@Composable
fun PlansScreen_Annual_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        PlansScreen(
            initialTabIndex = 1,
            subscriptionPlans = PlansScreenData.getPreviewSubscriptionPlans(),
            onSelectSubscriptionPlan = {},
            navigateToSignIn = {}
        )
    }
}

@PortraitPreviews
@Composable
fun PlansScreen_Annual_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        PlansScreen(
            initialTabIndex = 1,
            subscriptionPlans = PlansScreenData.getPreviewSubscriptionPlans(),
            onSelectSubscriptionPlan = {},
            navigateToSignIn = {}
        )
    }
}

@PortraitPreviews
@Composable
fun PlansScreen_Monthly_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        PlansScreen(
            initialTabIndex = 2,
            subscriptionPlans = PlansScreenData.getPreviewSubscriptionPlans(),
            onSelectSubscriptionPlan = {},
            navigateToSignIn = {}
        )
    }
}

@PortraitPreviews
@Composable
fun PlansScreen_Monthly_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        PlansScreen(
            initialTabIndex = 2,
            subscriptionPlans = PlansScreenData.getPreviewSubscriptionPlans(),
            onSelectSubscriptionPlan = {},
            navigateToSignIn = {}
        )
    }
}
