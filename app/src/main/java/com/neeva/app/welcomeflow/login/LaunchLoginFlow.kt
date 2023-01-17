// Copyright 2023 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.firstrun.LaunchLoginFlowParams
import com.neeva.app.firstrun.LoginReturnParams
import com.neeva.app.userdata.NeevaUser

@Composable
fun launchLoginFlow(
    loginReturnParams: LoginReturnParams,
    provider: NeevaUser.SSOProvider? = null,
    signup: Boolean,
    mktEmailOptOut: Boolean,
): () -> Unit {
    val firstRunModel = LocalFirstRunModel.current
    val context = LocalContext.current

    val params = LaunchLoginFlowParams(
        provider = provider ?: NeevaUser.SSOProvider.GOOGLE,
        signup = signup,
        mktEmailOptOut = mktEmailOptOut
    )

    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    return {
        firstRunModel.launchLoginFlow(
            loginReturnParams = LoginReturnParams(
                activityToReturnTo = loginReturnParams.activityToReturnTo,
                screenToReturnTo = loginReturnParams.screenToReturnTo
            ),
            context = context,
            launchLoginFlowParams = params,
            activityResultLauncher = resultLauncher
        )
    }
}
