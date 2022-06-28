package com.neeva.app.ui.widgets

import android.net.Uri
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer

fun parseURLPath(uri: Uri): Pair<String?, List<String>> {
    val sanitizedAuthority = uri.authority?.replace("www.", "")
    val pieces = uri.pathSegments ?: emptyList()
    return Pair(sanitizedAuthority, pieces)
}

@Composable
fun UriDisplayView(
    uri: Uri,
    separator: String = "\u203A"
) {
    val parts = remember(uri) { parseURLPath(uri) }

    parts.first?.let { sanitizedAuthority ->
        SplitStringRow(
            primary = sanitizedAuthority,
            secondaryPieces = parts.second,
            separator = separator
        )
    }
}

@Preview("normal suggestion, 1x", locale = "en")
@Preview("normal suggestion, 2x", locale = "en", fontScale = 2.0f)
@Preview("normal suggestion, RTL, 1x", locale = "he")
@Composable
fun UriDisplayView_Normal() {
    LightDarkPreviewContainer {
        Surface {
            UriDisplayView(Uri.parse("https://www.neeva.com/path/to/x/"))
        }
    }
}

@Preview("only authority, 1x", locale = "en")
@Preview("only authority, 2x", locale = "en", fontScale = 2.0f)
@Preview("only authority, RTL, 1x", locale = "he")
@Composable
fun UriDisplayView_OnlyAuthority() {
    LightDarkPreviewContainer {
        Surface {
            UriDisplayView(Uri.parse("https://www.neeva.com"))
        }
    }
}

@Preview("short authority and long path, 1x", locale = "en")
@Preview("short authority and long path, 2x", locale = "en", fontScale = 2.0f)
@Preview("short authority and long path, RTL, 1x", locale = "he")
@Composable
fun UriDisplayView_shortAuthorityAndLongPath() {
    val urlString =
        "https://www.neeva.com/this/is/a/long/path/to/the/website/and/result/you/want/yours/truly/"
    LightDarkPreviewContainer {
        Surface {
            UriDisplayView(Uri.parse(urlString))
        }
    }
}

@Preview("invalid URI, 2x", fontScale = 2.0f)
@Preview("invalid URI, RTL, 1x", locale = "he")
@Preview("invalid URI, RTL, 2x", locale = "he", fontScale = 2.0f)
@Composable
fun UriDisplayView_invalidURI() {
    val urlString = stringResource(R.string.debug_long_string_secondary)
    LightDarkPreviewContainer {
        Surface {
            UriDisplayView(Uri.parse(urlString))
        }
    }
}
