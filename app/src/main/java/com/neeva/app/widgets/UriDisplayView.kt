package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

fun parseURLPath(path: String?): List<String> {
    if (path == null) {
        return listOf()
    }
    return path.split("/").filter { it.isNotEmpty() }
}

@Composable
fun UriDisplayView(
    uri: Uri,
    separator: String = "\u203A"
) {
    Row {
        var authority = uri.authority
        var path = uri.path
        if (authority != null && path != null) {
            Text(
                text = authority.replace("www.", ""),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1
            )
            var pathText = ""
            parseURLPath(path).forEach { pathText += " $separator $it" }
            if (pathText.isNotEmpty()) {
                Text(
                    text = pathText,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview("normal suggestion, 1x")
@Preview("normal suggestion, 2x", fontScale = 2.0f)
@Preview("normal suggestion, RTL, 1x", locale = "he")
@Preview("normal suggestion, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun UriDisplayView_Normal() {
    NeevaTheme {
        UriDisplayView(Uri.parse("www.google.com/path/to/x/"))
    }
}

@Preview("only authority, 1x")
@Preview("only authority, 2x", fontScale = 2.0f)
@Preview("only authority, RTL, 1x", locale = "he")
@Preview("only authority, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun UriDisplayView_OnlyAuthority() {
    NeevaTheme {
        UriDisplayView(Uri.parse("www.google.com"))
    }
}

@Preview("short authority and long path, 1x")
@Preview("short authority and long path, 2x", fontScale = 2.0f)
@Preview("short authority and long path, RTL, 1x", locale = "he")
@Preview("short authority and long path, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun UriDisplayView_shortAuthorityAndLongPath() {
    val urlString =
        "www.google.com/this/is/a/long/path/to/the/website/and/result/you/want/yours/truly/neeva"
    NeevaTheme {
        UriDisplayView(
            Uri.parse(urlString)
        )
    }
}

@Preview("invalid uri, 1x")
@Preview("long authority and long path, 2x", fontScale = 2.0f)
@Preview("long authority and long path, RTL, 1x", locale = "he")
@Preview("long authority and long path, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun UriDisplayView_invalidURI() {
    val urlString = stringResource(R.string.debug_long_string_secondary) +
            "/this/is/a/long/path/to/the/website/and/result/you/want/yours/truly/neeva"
    NeevaTheme {
        UriDisplayView(
            Uri.parse(urlString)
        )
    }
}
