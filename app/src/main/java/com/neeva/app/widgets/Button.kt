package com.neeva.app.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun Button(
    enabled: Boolean,
    resID: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(id = resID),
        contentDescription = contentDescription,
        contentScale = ContentScale.Inside,
        modifier = Modifier
            .size(48.dp, 48.dp)
            .clickable(enabled) { onClick() },
        colorFilter = ColorFilter.tint(if (enabled) MaterialTheme.colors.onPrimary else Color.LightGray)
    )
}

@Composable
fun BrandedTextButton(
    enabled: Boolean,
    stringResID: Int,
    onClick: () -> Unit
) {
    Text(
        modifier = Modifier
            .wrapContentHeight(align = Alignment.CenterVertically)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(R.color.brand_blue))
            .padding(vertical = 12.dp, horizontal = 48.dp)
            .fillMaxWidth()
            .clickable(enabled) { onClick() },
        text = stringResource(id = stringResID),
        style = MaterialTheme.typography.body1,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Preview("1x scale")
@Composable
fun Button_Preview() {
    NeevaTheme {
        Button(enabled = true, R.drawable.btn_close, "Close Button") {}
    }
}

@Preview("1x scale")
@Preview("2x scale", fontScale = 2.0f)
@Preview("RTL, 1x scale", locale = "he")
@Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun BrandedTextButton_Preview() {
    NeevaTheme {
        BrandedTextButton(enabled = true, stringResID = R.string.sign_in_with_google) {}
    }
}