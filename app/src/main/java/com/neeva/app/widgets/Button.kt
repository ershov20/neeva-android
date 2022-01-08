package com.neeva.app.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neeva.app.R
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.Roobert

@Composable
fun Button(
    enabled: Boolean,
    resID: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(id = resID),
        contentDescription = contentDescription,
        contentScale = ContentScale.Inside,
        modifier = modifier
            .size(48.dp, 48.dp)
            .clickable(enabled) { onClick() },
        colorFilter = ColorFilter.tint(
            if (enabled) {
                MaterialTheme.colors.onPrimary
            } else {
                Color.LightGray
            }
        )
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
            .background(ColorPalette.Brand.Blue)
            .padding(vertical = 12.dp, horizontal = 48.dp)
            .fillMaxWidth()
            .clickable(enabled) { onClick() },
        text = stringResource(id = stringResID),
        style = TextStyle(
            fontFamily = Roobert,
            fontWeight = FontWeight.W400,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
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
