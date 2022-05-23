package com.neeva.app.cookiecutter.ui.popover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.cookiecutter.TrackersAllowList
import com.neeva.app.ui.NeevaSwitch

/**
 * A [NeevaSwitch] that disables when tapped and enables when the [trackersAllowList]'s job.
 * succeeds.
 */
@Composable
internal fun CookieCutterPopoverSwitch(
    isIncognito: Boolean,
    cookieCutterEnabled: Boolean,
    host: String,
    trackersAllowList: TrackersAllowList,
    onSuccess: (newValue: Boolean) -> Unit
) {
    val allowClickingSwitch = remember { mutableStateOf(true) }
    // TODO(dan): remove isIncognito if when https://github.com/neevaco/neeva-android/issues/641 is fixed.
    NeevaSwitch(
        primaryLabel = stringResource(id = R.string.cookie_cutter),
        isChecked = cookieCutterEnabled,
        enabled = !isIncognito && allowClickingSwitch.value,
        onCheckedChange = { isCookieCutterEnabled ->
            allowClickingSwitch.value = false
            val jobDidRun = trackersAllowList.getAllowListSetter(
                host = host,
                onSuccess = {
                    allowClickingSwitch.value = true
                    onSuccess(isCookieCutterEnabled)
                }
            ).invoke(isCookieCutterEnabled)

            if (jobDidRun) {
                allowClickingSwitch.value = true
            }
        }
    )
}
