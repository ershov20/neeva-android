package com.neeva.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.neeva.app.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Manages state that allows the app to display Snackbars within the Composable hierarchy. */
class SnackbarModel(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers
) {
    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    @OptIn(ExperimentalMaterial3Api::class)
    fun show(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onActionPerformed: () -> Unit = {},
        onDismissed: () -> Unit = {}
    ) {
        coroutineScope.launch(dispatchers.main) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = duration == SnackbarDuration.Indefinite,
                duration = duration
            )

            when (result) {
                SnackbarResult.ActionPerformed -> onActionPerformed()
                SnackbarResult.Dismissed -> onDismissed()
            }
        }
    }
}
