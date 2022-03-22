package com.neeva.app.suggestions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neeva.app.R

data class SuggestionRowActionParams(
    val onTapAction: () -> Unit,
    val actionType: ActionType
) {
    enum class ActionType {
        NONE, REFINE, DELETE
    }
}

@Composable
fun SuggestionRowAction(params: SuggestionRowActionParams) {
    IconButton(onClick = params.onTapAction) {
        when (params.actionType) {
            SuggestionRowActionParams.ActionType.REFINE -> {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_north_west_24),
                    contentDescription = stringResource(
                        R.string.refine_content_description
                    )
                )
            }

            SuggestionRowActionParams.ActionType.DELETE -> {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove)
                )
            }

            else -> throw IllegalArgumentException()
        }
    }
}
