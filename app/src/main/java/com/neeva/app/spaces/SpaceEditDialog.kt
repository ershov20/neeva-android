package com.neeva.app.spaces

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceEntityType
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.NeevaTextField
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.theme.Dimensions
import kotlinx.coroutines.launch

enum class SpaceEditMode {
    ADDING_SPACE_ITEM, EDITING_SPACE_ITEM, EDITING_SPACE
}

data class EditSpaceInfo(
    val editMode: SpaceEditMode = SpaceEditMode.EDITING_SPACE_ITEM,
    val spaceItem: SpaceItem?,
    val space: Space? = null
)

@Composable
fun EditSpaceDialog(
    mode: SpaceEditMode,
    id: String?
) {
    val appNavModel = LocalAppNavModel.current
    val spaceStore = LocalEnvironment.current.spaceStore
    val editSpaceInfo = produceState<EditSpaceInfo?>(initialValue = null, mode, id) {
        value = spaceStore.getEditSpaceInfo(
            mode = mode,
            id = id
        )
    }

    editSpaceInfo.value?.let {
        EditSpaceDialog(
            mode = it.editMode,
            spaceItem = it.spaceItem,
            space = it.space,
            onDismiss = { appNavModel.popBackStack() },
            spaceStore = spaceStore
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSpaceDialog(
    spaceItem: SpaceItem?,
    mode: SpaceEditMode = SpaceEditMode.EDITING_SPACE_ITEM,
    space: Space? = null,
    onDismiss: () -> Unit,
    spaceStore: SpaceStore? = null
) {
    val url = remember(mode) { mutableStateOf("") }

    val title = remember(spaceItem?.title, space?.name, mode) {
        mutableStateOf(
            when (mode) {
                SpaceEditMode.EDITING_SPACE -> space?.name ?: ""
                else -> spaceItem?.title ?: ""
            }
        )
    }

    val description = remember(spaceItem?.snippet, space?.description, mode) {
        mutableStateOf(
            when (mode) {
                SpaceEditMode.EDITING_SPACE -> space?.description ?: ""
                else -> spaceItem?.snippet ?: ""
            }
        )
    }

    Scaffold(
        topBar = {
            val coroutineScope = rememberCoroutineScope()
            FullScreenDialogTopBar(
                title = when (mode) {
                    SpaceEditMode.EDITING_SPACE -> stringResource(id = R.string.edit_space_header)

                    SpaceEditMode.EDITING_SPACE_ITEM ->
                        stringResource(id = R.string.edit_space_item_header)

                    SpaceEditMode.ADDING_SPACE_ITEM ->
                        stringResource(id = R.string.add_space_item_header)
                },
                onBackPressed = onDismiss,
                buttonTitle = when (mode) {
                    SpaceEditMode.EDITING_SPACE ->
                        stringResource(id = R.string.space_edit_dialog_update_action)

                    SpaceEditMode.EDITING_SPACE_ITEM ->
                        stringResource(id = R.string.space_edit_dialog_update_action)

                    SpaceEditMode.ADDING_SPACE_ITEM ->
                        stringResource(id = R.string.space_edit_dialog_add_action)
                },
                onButtonPressed = {
                    coroutineScope.launch {
                        when (mode) {
                            SpaceEditMode.ADDING_SPACE_ITEM ->
                                space?.let {
                                    spaceStore?.addToSpace(
                                        it,
                                        Uri.parse(url.value),
                                        title.value,
                                        description.value
                                    )
                                }
                            SpaceEditMode.EDITING_SPACE_ITEM ->
                                spaceItem?.let {
                                    spaceStore?.updateSpaceItem(
                                        it,
                                        title.value,
                                        description.value
                                    )
                                }
                            SpaceEditMode.EDITING_SPACE ->
                                space?.let {
                                    spaceStore?.updateSpace(
                                        it,
                                        title.value,
                                        description.value
                                    )
                                }
                        }
                    }
                    onDismiss()
                }
            )
        }
    ) { paddingValues ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                if (mode == SpaceEditMode.ADDING_SPACE_ITEM) {
                    Text(
                        text = stringResource(id = R.string.space_edit_dialog_url),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(horizontal = Dimensions.PADDING_LARGE)
                    )

                    Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                    NeevaTextField(
                        text = url.value,
                        onTextChanged = { url.value = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.PADDING_LARGE)
                    )

                    Spacer(Modifier.height(Dimensions.PADDING_LARGE))
                }

                Text(
                    text = stringResource(id = R.string.space_edit_dialog_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = Dimensions.PADDING_LARGE)
                )

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                NeevaTextField(
                    text = title.value,
                    onTextChanged = { title.value = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.PADDING_LARGE)
                )

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                Text(
                    text = stringResource(id = R.string.space_edit_dialog_description),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = Dimensions.PADDING_LARGE)
                )

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                NeevaTextField(
                    text = description.value,
                    onTextChanged = { description.value = it },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.PADDING_LARGE)
                )

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))
            }
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Composable
fun SpaceEditDialogPreview_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        EditSpaceDialog(
            SpaceItem(
                "asjdahjfad",
                "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                Uri.parse("https://example.com"),
                "Facebook papers notes",
                "Facebook likes to portray itself as a social media giant under" +
                    " siege — locked in fierce competition with other companies",
                null,
                0,
                SpaceEntityType.WEB
            ),
            onDismiss = {}
        )
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Composable
fun SpaceEditDialogPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        EditSpaceDialog(
            SpaceItem(
                "asjdahjfad",
                "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                Uri.parse("https://example.com"),
                "Facebook papers notes",
                "Facebook likes to portray itself as a social media giant under" +
                    " siege — locked in fierce competition with other companies",
                null,
                0,
                SpaceEntityType.WEB
            ),
            onDismiss = {}
        )
    }
}
