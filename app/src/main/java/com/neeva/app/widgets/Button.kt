package com.neeva.app.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val colorTint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.LightGray
    }

    Button(enabled, resID, contentDescription, colorTint, modifier, onClick)
}

@Composable
fun Button(
    enabled: Boolean,
    resID: Int,
    contentDescription: String,
    colorTint: Color,
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
        colorFilter = ColorFilter.tint(colorTint)
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary)
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
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1.0f else 0.5f),
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true, backgroundColor = 0xff000000)
@Composable
fun Button_PreviewDarkEnabled() {
    NeevaTheme(useDarkTheme = true) {
        Button(enabled = true, R.drawable.btn_close, "Close Button") {}
    }
}

@Preview(showBackground = true, backgroundColor = 0xff000000)
@Composable
fun Button_PreviewDarkDisabled() {
    NeevaTheme(useDarkTheme = true) {
        Button(enabled = false, R.drawable.btn_close, "Close Button") {}
    }
}

@Preview
@Composable
fun Button_PreviewEnabled() {
    NeevaTheme {
        Button(enabled = true, R.drawable.btn_close, "Close Button") {}
    }
}

@Preview
@Composable
fun Button_PreviewDisabled() {
    NeevaTheme {
        Button(enabled = false, R.drawable.btn_close, "Close Button") {}
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

@Preview("Dark, 1x scale")
@Preview("Dark, 2x scale", fontScale = 2.0f)
@Preview("Dark, RTL, 1x scale", locale = "he")
@Preview("Dark, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun BrandedTextButton_PreviewDark() {
    NeevaTheme(useDarkTheme = true) {
        Box(modifier = Modifier.background(Color.Black)) {
            BrandedTextButton(enabled = true, stringResID = R.string.sign_in_with_google) {}
        }
    }
}
