package com.neeva.app.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PopupModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var snackbarHostState: SnackbarHostState
    private lateinit var showCallbacks: MutableList<CompletableDeferred<SnackbarResult>>

    private lateinit var popupModel: PopupModel

    override fun setUp() {
        super.setUp()

        showCallbacks = mutableListOf()
        snackbarHostState = mockk {
            coEvery { showSnackbar(any(), any(), any(), any()) } coAnswers {
                val completableDeferred = CompletableDeferred<SnackbarResult>()
                showCallbacks.add(completableDeferred)
                completableDeferred.await()
            }
        }

        popupModel = PopupModel(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            snackbarHostState = snackbarHostState
        )
    }

    private fun showAndVerifySnackbar(
        message: String,
        actionLabel: String,
        onActionPerformed: () -> Unit,
        onDismissed: () -> Unit
    ) {
        popupModel.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            onActionPerformed = onActionPerformed,
            onDismissed = onDismissed
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        coVerify {
            snackbarHostState.showSnackbar(
                eq(message),
                eq(actionLabel),
                eq(false),
                eq(SnackbarDuration.Short)
            )
        }

        // None of the callbacks should have been called yet because the callback created by the
        // mock needs to be fired.
        verify(exactly = 0) { onActionPerformed.invoke() }
        verify(exactly = 0) { onDismissed.invoke() }
    }

    @Test
    fun showSnackbar_firesDismissCallback_onlyOnce() {
        val firstOnActionPerformed: () -> Unit = mockk()
        val firstOnDismissed: () -> Unit = mockk()
        every { firstOnDismissed.invoke() } returns Unit

        showAndVerifySnackbar(
            message = "Test message",
            actionLabel = "Action",
            onActionPerformed = firstOnActionPerformed,
            onDismissed = firstOnDismissed
        )
        expectThat(showCallbacks).hasSize(1)

        // Say that the snackbar was dismissed.
        showCallbacks.last().complete(SnackbarResult.Dismissed)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(exactly = 0) { firstOnActionPerformed.invoke() }
        verify(exactly = 1) { firstOnDismissed.invoke() }

        // Show another snackbar.
        val secondOnActionPerformed: () -> Unit = mockk()
        val secondOnDismissed: () -> Unit = mockk()
        showAndVerifySnackbar(
            message = "Test message 2",
            actionLabel = "Action 2",
            onActionPerformed = secondOnActionPerformed,
            onDismissed = secondOnDismissed
        )
        expectThat(showCallbacks).hasSize(2)

        // The first snackbar's callbacks shouldn't have fired again.
        verify(exactly = 0) { firstOnActionPerformed.invoke() }
        verify(exactly = 1) { firstOnDismissed.invoke() }
    }

    @Test
    fun showSnackbar_firesActionCallback_onlyOnce() {
        val firstOnActionPerformed: () -> Unit = mockk()
        val firstOnDismissed: () -> Unit = mockk()
        every { firstOnActionPerformed.invoke() } returns Unit

        showAndVerifySnackbar(
            message = "Test message",
            actionLabel = "Action",
            onActionPerformed = firstOnActionPerformed,
            onDismissed = firstOnDismissed
        )
        expectThat(showCallbacks).hasSize(1)

        // Say that the user tapped on the snackbar's action.
        showCallbacks.last().complete(SnackbarResult.ActionPerformed)
        coroutineScopeRule.scope.advanceUntilIdle()
        verify(exactly = 1) { firstOnActionPerformed.invoke() }
        verify(exactly = 0) { firstOnDismissed.invoke() }

        // Show another snackbar.
        val secondOnActionPerformed: () -> Unit = mockk()
        val secondOnDismissed: () -> Unit = mockk()
        showAndVerifySnackbar(
            message = "Test message 2",
            actionLabel = "Action 2",
            onActionPerformed = secondOnActionPerformed,
            onDismissed = secondOnDismissed
        )
        expectThat(showCallbacks).hasSize(2)

        // The first snackbar's callbacks shouldn't have fired again.
        verify(exactly = 1) { firstOnActionPerformed.invoke() }
        verify(exactly = 0) { firstOnDismissed.invoke() }
    }

    @Test
    fun showSnackbar_twice_firesDismissCallback() {
        // Show the first snackbar.
        val firstOnActionPerformed: () -> Unit = mockk()
        val firstOnDismissed: () -> Unit = mockk()
        every { firstOnDismissed.invoke() } returns Unit
        showAndVerifySnackbar(
            message = "Test message",
            actionLabel = "Action",
            onActionPerformed = firstOnActionPerformed,
            onDismissed = firstOnDismissed
        )

        // Show another snackbar.
        val secondOnActionPerformed: () -> Unit = mockk()
        val secondOnDismissed: () -> Unit = mockk()
        showAndVerifySnackbar(
            message = "Test message 2",
            actionLabel = "Action 2",
            onActionPerformed = secondOnActionPerformed,
            onDismissed = secondOnDismissed
        )
        expectThat(showCallbacks).hasSize(2)

        // The first snackbar's dismiss callback should have fired.
        verify(exactly = 0) { firstOnActionPerformed.invoke() }
        verify(exactly = 1) { firstOnDismissed.invoke() }

        // The second snackbar is still waiting for a response.
        verify(exactly = 0) { secondOnActionPerformed.invoke() }
        verify(exactly = 0) { secondOnDismissed.invoke() }

        // Say that the user acted on the second snackbar.
        showCallbacks.last().complete(SnackbarResult.ActionPerformed)
        coroutineScopeRule.scope.advanceUntilIdle()

        // Only the second snackbar's callbacks should have fired.
        verify(exactly = 0) { firstOnActionPerformed.invoke() }
        verify(exactly = 1) { firstOnDismissed.invoke() }
        verify(exactly = 1) { secondOnActionPerformed.invoke() }
        verify(exactly = 0) { secondOnDismissed.invoke() }
    }
}
