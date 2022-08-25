package com.neeva.app.neevascope

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import com.neeva.app.R
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

fun LazyListScope.RedditDiscussionsList(
    discussions: List<NeevaScopeDiscussion>,
    showAllDiscussions: MutableState<Boolean>,
    openUrl: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val numDiscussionsDisplayed =
        mutableStateOf(if (showAllDiscussions.value) discussions.count() else 3)
    item {
        NeevaScopeSectionHeader(title = R.string.neevascope_reddit_discussion)
        Spacer(modifier = Modifier.padding(Dimensions.PADDING_MEDIUM))
    }

    items(discussions.take(numDiscussionsDisplayed.value)) { discussion ->
        RedditDiscussionRow(discussion, openUrl, onDismiss)
        Spacer(modifier = Modifier.padding(Dimensions.PADDING_MEDIUM))
    }

    if (!showAllDiscussions.value) {
        item {
            ShowMoreButton(
                text = R.string.neevascope_discussion_show_more,
                showAll = showAllDiscussions
            )
        }
    }

    item {
        NeevaScopeDivider()
    }
}

@Composable
fun RedditDiscussionHeader(
    discussion: NeevaScopeDiscussion
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY)
        ) {
            RedditDiscussionIcon(
                painterId = R.drawable.outbrain,
                text = discussion.slash,
                lightColor = false
            )

            RedditDiscussionIcon(
                icon = Icons.Outlined.ArrowUpward,
                text = "${discussion.upvotes}",
                hasDot = true
            )

            RedditDiscussionIcon(
                icon = Icons.Outlined.ChatBubbleOutline,
                text = "${discussion.numComments}",
                hasDot = true
            )

            RedditDiscussionIcon(
                text = discussion.interval.toString(),
                hasDot = true
            )
        }

        Text(
            text = discussion.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RedditDiscussionIcon(
    icon: ImageVector? = null,
    painterId: Int? = null,
    text: String,
    lightColor: Boolean = true,
    hasDot: Boolean = false
) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.outline
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY)
        ) {
            if (hasDot) {
                Text(
                    text = stringResource(id = R.string.dot_separator),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val iconModifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
            when {
                icon != null -> {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = iconModifier
                    )
                }

                painterId != null -> {
                    Image(
                        painter = painterResource(id = painterId),
                        contentDescription = null,
                        modifier = iconModifier
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = if (lightColor) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun RedditDiscussionRow(
    discussion: NeevaScopeDiscussion,
    openUrl: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                openUrl(discussion.url)
                onDismiss()
            },
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
    ) {
        val showMoreCommentsButton: Boolean = discussion.numComments?.let {
            it > discussion.content.comments.count()
        } == true

        RedditDiscussionHeader(discussion = discussion)

        if (discussion.content.comments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_LARGE)
            ) {
                items(discussion.content.comments.take(10)) { comment ->
                    Row {
                        Column(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(horizontal = Dimensions.PADDING_SMALL)
                                .clickable {
                                    if (comment.url == Uri.EMPTY) {
                                        openUrl(discussion.url)
                                    } else {
                                        comment.url?.let { openUrl(it) }
                                    }
                                    onDismiss()
                                }
                        ) {
                            ExpandableTextView(
                                text = comment.body,
                                upvotes = comment.upvotes,
                                maxLine = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (showMoreCommentsButton) {
                    item {
                        OutlinedButton(
                            onClick = {
                                openUrl(discussion.url)
                                onDismiss()
                            }
                        ) {
                            Icon(
                                Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
                            )

                            Spacer(modifier = Modifier.padding(Dimensions.PADDING_TINY))

                            Text(text = stringResource(id = R.string.discussion_more_comments))
                        }
                    }
                }
            }
        } else if (discussion.content.body.isNotEmpty()) {
            Text(
                text = discussion.content.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
        }
    }
}

@Composable
fun ExpandableTextView(
    text: String,
    upvotes: Int?,
    maxLine: Int,
    modifier: Modifier = Modifier
) {
    var readMoreOrLess by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    var isClickable by remember { mutableStateOf(false) }

    val textLayoutResultState = remember { mutableStateOf<TextLayoutResult?>(null) }
    val textLayoutResult = textLayoutResultState.value

    when {
        isExpanded -> {
            readMoreOrLess = R.string.discussion_comment_less
        }

        textLayoutResult?.hasVisualOverflow == true -> {
            readMoreOrLess = R.string.discussion_comment_more
            isClickable = true
        }
    }

    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = if (isExpanded) Int.MAX_VALUE else maxLine,
        onTextLayout = { textLayoutResultState.value = it },
        modifier = Modifier.animateContentSize()
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RedditDiscussionIcon(
            icon = Icons.Outlined.ArrowUpward,
            text = "$upvotes"
        )

        if (isClickable) {
            Text(
                text = stringResource(id = readMoreOrLess),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
                    .clickable { isExpanded = !isExpanded }
            )
        }
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun RedditDiscussionRow_withComments_Preview() {
    LightDarkPreviewContainer {
        RedditDiscussionRow(
            discussion = NeevaScopeDiscussion(
                title = "GTA Vice City",
                content = DiscussionContent(
                    body = "",
                    comments = listOf(
                        DiscussionComment(
                            body = "UPDATE: I got the plug-in to recognize my playlist",
                            url = Uri.parse(""),
                            upvotes = 1
                        )
                    )
                ),
                url = Uri.parse(""),
                slash = "/r/85uyan",
                upvotes = 4,
                numComments = 20,
                interval = "4 years ago"
            ),
            onDismiss = {},
            openUrl = {}
        )
    }
}
