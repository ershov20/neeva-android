package com.neeva.app.cookiecutter.ui.popover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.cookiecutter.TrackersAllowList
import com.neeva.app.ui.NeevaSwitch

/**
 * [NeevaSwitch] that disables when tapped and enables when the [trackersAllowList]'s job succeeds.
 */
@Composable
internal fun CookieCutterPopoverSwitch(
    cookieCutterEnabled: Boolean,
    host: String,
    trackersAllowList: TrackersAllowList,
    onSuccess: () -> Unit
) {
    val allowClickingSwitch = remember { mutableStateOf(true) }
    NeevaSwitch(
        primaryLabel = stringResource(id = R.string.cookie_cutter),
        secondaryLabel = stringResource(id = R.string.cookie_cutter_subtitle),
        isChecked = cookieCutterEnabled,
        enabled = allowClickingSwitch.value,
        onCheckedChange = {
            // Disallow the switch from doing anything until the TrackersAllowList is updated.
            allowClickingSwitch.value = false

            val jobDidRun = trackersAllowList.toggleHostInAllowList(host = host) {
                allowClickingSwitch.value = true
                onSuccess()
            }

            if (!jobDidRun) {
                // The job couldn't be started because another was already in progress.
                // Re-enable the switch so the user can try again.
                allowClickingSwitch.value = true
            }
        }
    )
}
