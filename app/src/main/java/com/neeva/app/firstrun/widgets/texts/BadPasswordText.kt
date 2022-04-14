package com.neeva.app.firstrun.widgets.texts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

@Composable
fun BadPasswordText(password: String) {
    val message = if (!isGoodPassword(password = password)) {
        stringResource(R.string.weak_password)
    } else {
        ""
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
        modifier = Modifier.padding(
            top = Dimensions.PADDING_TINY,
            bottom = Dimensions.PADDING_SMALL,
            start = Dimensions.PADDING_LARGE
        )
    )
}

private fun isGoodPassword(password: String): Boolean {
    // TODO(kobec): How would this work for non-English strings?
    return password.length >= 8 &&
        password.any { it.isDigit() } &&
        password.any { it.isUpperCase() } &&
        password.any { it.isLowerCase() } &&
        password.any { it.isLetter() }
}
