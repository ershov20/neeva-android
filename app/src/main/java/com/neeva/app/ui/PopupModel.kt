package com.neeva.app.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import com.neeva.app.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DialogState(
    val content: @Composable () -> Unit
)

/** Manages state that allows the app to display Snackbars and Dialogs within the Composition. */
class PopupModel(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers
) {
    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> get() = _dialogState

    fun showSnackbar(
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

    /** Shows a full-screen dialog with the given content. */
    fun showDialog(content: @Composable () -> Unit) {
        _dialogState.value = DialogState(content)
    }

    /** Removes any dialog that is currently showing. */
    fun hideDialog() {
        _dialogState.value = null
    }
}
