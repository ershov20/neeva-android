package com.neeva.app.firstrun.widgets.texts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.firstrun.FirstRunConstants.getSubtextStyle
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

@Composable
fun OnboardingTextField(
    text: String,
    onTextChanged: (String) -> Unit,
    label: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().then(modifier),
        value = text,
        onValueChange = onTextChanged,
        label = {
            // It's important to use Material2 text instead of Material3 because Material3 does not render
            // the focus label color correctly. https://stackoverflow.com/questions/69780322/applying-material3-colors-to-outlinedtextfield-in-jetpack-compose
            Text(
                text = label,
                style = getSubtextStyle(color = Color.Unspecified)
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = { }
        ),
        colors = TextFieldDefaults.textFieldColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(size = Dimensions.RADIUS_SMALL)
    )
}

// TODO(kobec): The focused state is not working properly in the previews, not sure if its a Material3 bug...
@Preview("Email Preview LTR 1x scale", locale = "en")
@Preview("Email Preview LTR 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Email Preview RTL 1x scale", locale = "he")
@Composable
fun OnboardingTextField_Email_Preview() {
    TwoBooleanPreviewContainer { hasText, isFocused ->
        val startingString = if (hasText) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            ""
        }

        val email = remember { mutableStateOf(startingString) }
        val focusRequester = remember { FocusRequester() }

        val modifier = if (isFocused) {
            Modifier.focusRequester(focusRequester)
        } else {
            Modifier
        }

        OnboardingTextField(
            text = email.value,
            onTextChanged = {},
            label = "Email",
            modifier = modifier
        )

        if (isFocused) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}
