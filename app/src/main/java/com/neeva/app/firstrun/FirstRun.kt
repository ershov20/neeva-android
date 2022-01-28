package com.neeva.app.firstrun

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neeva.app.LocalEnvironment
import com.neeva.app.NeevaUserToken
import com.neeva.app.R
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.NeevaUser
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.Roobert
import com.neeva.app.widgets.BrandedTextButton

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FirstRunContainer() {
    val appNavModel = LocalEnvironment.current.appNavModel

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_baseline_close_24),
            contentDescription = "Close",
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(48.dp, 48.dp)
                .clickable { appNavModel.showBrowser() },
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
        )
        FirstRunScreen()
    }
}

@Composable
fun FirstRunScreen() {
    val activityContext = LocalContext.current
    var emailProvided by remember { mutableStateOf("") }
    var signup by remember { mutableStateOf(true) }

    Column(
        Modifier
            .wrapContentSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(72.dp).fillMaxWidth())

        if (signup) {
            Image(
                painter = painterResource(id = R.drawable.ic_wordmark),
                contentDescription = null,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
            )
            Text(
                modifier = Modifier
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 50.dp),
                text = stringResource(id = R.string.first_run_intro),
                style = TextStyle(
                    fontFamily = Roobert,
                    fontWeight = FontWeight.Light,
                    fontSize = 40.sp,
                    lineHeight = 48.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Start
            )
        } else {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(align = Alignment.Center),
                text = stringResource(id = R.string.sign_in),
                style = TextStyle(
                    fontFamily = Roobert,
                    fontWeight = FontWeight.W400,
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Start
            )
            ToggleSignUpText(false) {
                signup = true
            }
            TextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                value = emailProvided,
                onValueChange = { emailProvided = it },
                label = {
                    Text(
                        text = stringResource(id = R.string.email_label),
                        style = TextStyle(
                            fontFamily = Roobert,
                            fontWeight = FontWeight.W500,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = Roobert,
                    fontWeight = FontWeight.W400,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { }
                ),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.colorScheme.onPrimary,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(size = 12.dp),
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mail),
                        contentDescription = null,
                        modifier = Modifier
                            .wrapContentSize(),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                    )
                }
            )
            BrandedTextButton(
                enabled = emailProvided.contains("@"),
                stringResID = R.string.sign_in_with_okta
            ) {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(
                        activityContext,
                        FirstRun.authUri(
                            signup,
                            NeevaUser.SSOProvider.OKTA,
                            emailProvided
                        )
                    )
            }
            if (!emailProvided.contains("@")) {
                Text(
                    "OR",
                    modifier = Modifier
                        .padding(vertical = 20.dp)
                        .fillMaxWidth()
                        .wrapContentSize(align = Alignment.Center),
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.W600,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (!emailProvided.contains("@")) {
            BrandedTextButton(
                enabled = true,
                stringResID = if (signup) {
                    R.string.sign_up_with_google
                } else {
                    R.string.sign_up_with_google
                }
            ) {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(
                        activityContext,
                        FirstRun.authUri(signup, NeevaUser.SSOProvider.GOOGLE)
                    )
            }
            BrandedTextButton(
                enabled = true,
                stringResID = if (signup) {
                    R.string.sign_up_with_microsoft
                } else {
                    R.string.sign_in_with_microsoft
                }
            ) {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(
                        activityContext,
                        FirstRun.authUri(signup, NeevaUser.SSOProvider.MICROSOFT)
                    )
            }
            if (signup) {
                Spacer(modifier = Modifier.weight(1.0f))
                ToggleSignUpText(true) {
                    signup = false
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun ToggleSignUpText(signup: Boolean, onClick: () -> Unit) {
    Text(
        buildAnnotatedString {
            withStyle(
                TextStyle(
                    fontFamily = FontFamily.Default,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ).toSpanStyle()
            ) {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    append(
                        stringResource(
                            id = if (signup) {
                                R.string.already_have_account
                            } else {
                                R.string.dont_have_account
                            }
                        )
                    )
                }

                append(" ")

                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    append(
                        stringResource(
                            id = if (signup) {
                                R.string.sign_in
                            } else {
                                R.string.sign_up
                            }
                        )
                    )
                }
            }
        },
        modifier = Modifier
            .padding(vertical = 24.dp)
            .clickable { onClick() }
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center)
    )
}

@Preview("1x scale")
@Preview("RTL, 1x scale", locale = "he")
@Composable
fun FirstRun_Preview() {
    NeevaTheme {
        FirstRunScreen()
    }
}

@Preview("Landscape, 1x scale", widthDp = 512, heightDp = 384, locale = "he")
@Preview("Landscape, RTL, 1x scale", locale = "he")
@Composable
fun FirstRun_Preview_Landscape() {
    NeevaTheme {
        FirstRunScreen()
    }
}

@Preview("Dark, 1x scale")
@Preview("Dark, RTL, 1x scale", locale = "he")
@Composable
fun FirstRun_PreviewDark() {
    NeevaTheme(useDarkTheme = true) {
        FirstRunScreen()
    }
}

object FirstRun {
    private const val FIRST_RUN_PREFS_FOLDER_NAME = "FIRST_RUN_PREFERENCES"
    private const val FIRST_RUN_DONE_KEY = "HAS_FINISHED_FIRST_RUN"

    fun authUri(signup: Boolean, provider: NeevaUser.SSOProvider, loginHint: String = ""): Uri {
        val builder = Uri.Builder()
            .scheme("https")
            .authority("neeva.com")
            .path("login")
            .appendQueryParameter("provider", provider.url)
            .appendQueryParameter("finalPath", provider.finalPath)
            .appendQueryParameter("signup", signup.toString())
            .appendQueryParameter("ignoreCountryCode", "true")
            .appendQueryParameter("loginCallbackType", "ios")
        return when (provider) {
            NeevaUser.SSOProvider.OKTA ->
                builder
                    .appendQueryParameter("loginHint", loginHint)
                    .build()
            else -> builder.build()
        }
    }

    fun shouldShowFirstRun(
        sharedPreferencesModel: SharedPreferencesModel,
        neevaUserToken: NeevaUserToken
    ): Boolean {
        return neevaUserToken.getToken().isNullOrEmpty() &&
            !sharedPreferencesModel
                .getBoolean(SharedPrefFolder.FIRST_RUN, FIRST_RUN_DONE_KEY, false)
    }

    fun firstRunDone(sharedPreferencesModel: SharedPreferencesModel) {
        sharedPreferencesModel.setValue(SharedPrefFolder.FIRST_RUN, FIRST_RUN_DONE_KEY, true)
    }
}
