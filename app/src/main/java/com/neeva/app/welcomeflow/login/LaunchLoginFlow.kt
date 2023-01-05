// Copyright 2023 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.firstrun.ActivityReturnParams
import com.neeva.app.firstrun.LaunchLoginFlowParams
import com.neeva.app.userdata.NeevaUser

@Composable
fun launchLoginFlow(
    activityToReturnTo: String,
    screenToReturnTo: String,
    provider: NeevaUser.SSOProvider? = null,
    onPremiumAvailable: () -> Unit,
    onPremiumUnavailable: () -> Unit
): () -> Unit {
    val firstRunModel = LocalFirstRunModel.current
    val context = LocalContext.current

    val params = LaunchLoginFlowParams(
        provider = provider ?: NeevaUser.SSOProvider.GOOGLE,
        signup = false
    )

    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    return {
        firstRunModel.launchLoginFlow(
            activityReturnParams = ActivityReturnParams(
                activityToReturnTo = activityToReturnTo,
                screenToReturnTo = screenToReturnTo
            ),
            context = context,
            launchLoginFlowParams = params,
            activityResultLauncher = resultLauncher,
            onPremiumAvailable = onPremiumAvailable,
            onPremiumUnavailable = onPremiumUnavailable
        )
    }
}
