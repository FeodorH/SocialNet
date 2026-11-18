package com.example.socialnet.presentation.post

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.socialnet.core.common.UiState
import com.example.socialnet.core.common.ValidationRules
import com.example.socialnet.presentation.post.model.SavePostUiState
import com.example.socialnet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePostScreen(
    state: UiState<SavePostUiState>,
    onTextChange: (String) -> Unit,
    onImagesSelected: (List<String>) -> Unit,
    onNewImageRemoved: (Int) -> Unit,
    onExistingImageRemoved: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = (state as? UiState.Content)?.data
    val isEdit = content?.existingPostId != null

    // Photo Picker — без разрешений, работает на API 33+, fallback на API 21+
    val available = ValidationRules.MAX_POST_IMAGES - (content?.totalImagesCount ?: 0)
    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = maxOf(2, available)
        )
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) onImagesSelected(uris.map { it.toString() })
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) stringResource(R.string.edit_post_title)
                        else stringResource(R.string.create_post)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    if (content != null) {
                        if (content.isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            TextButton(
                                onClick = onSave,
                                enabled = content.canPublish,
                            ) { Text(stringResource(R.string.publish)) }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            UiState.Empty -> FullScreenMessage(
                message = stringResource(R.string.post_not_found),
                actionText = stringResource(R.string.retry),
                onAction = onRetryLoad,
                modifier = Modifier.padding(padding),
            )
            is UiState.Error -> FullScreenMessage(
                message = state.message,
                actionText = if (state.retryable) stringResource(R.string.retry) else null,
                onAction = onRetryLoad,
                modifier = Modifier.padding(padding),
            )
            is UiState.Content -> FormContent(
                state = state.data,
                onTextChange = onTextChange,
                onNewImageRemoved = onNewImageRemoved,
                onExistingImageRemoved = onExistingImageRemoved,
                onAddImagesClick = {
                    pickImagesLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun FormContent(
    state: SavePostUiState,
    onTextChange: (String) -> Unit,
    onNewImageRemoved: (Int) -> Unit,
    onExistingImageRemoved: (Int) -> Unit,
    onAddImagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (!isLandscape) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(R.string.post_text_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 12,
                enabled = !state.isPublishing,
                supportingText = {
                    Text(
                        "${state.text.length} / ${ValidationRules.MAX_POST_TEXT}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            if (state.totalImagesCount > 0) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Уже загруженные картинки (только в edit)
                    itemsIndexed(state.existingImages) { index, image ->
                        ImageThumbnail(
                            model = image.url,
                            onRemove = { onExistingImageRemoved(index) },
                            enabled = !state.isPublishing,
                        )
                    }
                    // Новые локальные
                    itemsIndexed(state.newImageUris) { index, uri ->
                        ImageThumbnail(
                            model = uri,
                            onRemove = { onNewImageRemoved(index) },
                            enabled = !state.isPublishing,
                        )
                    }
                    if (state.totalImagesCount < ValidationRules.MAX_POST_IMAGES) {
                        item {
                            AddImageThumbnail(
                                enabled = !state.isPublishing,
                                onClick = onAddImagesClick,
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.images_count, state.totalImagesCount, ValidationRules.MAX_POST_IMAGES),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedButton(
                    onClick = onAddImagesClick,
                    enabled = !state.isPublishing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_photo))
                }
            }
        }
    }
    else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(R.string.post_text_placeholder)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                minLines = 3,
                maxLines = 12,
                enabled = !state.isPublishing,
                supportingText = {
                    Text(
                        "${state.text.length} / ${ValidationRules.MAX_POST_TEXT}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.totalImagesCount > 0) {
                    ImagesRow(
                        state = state,
                        onNewImageRemoved = onNewImageRemoved,
                        onExistingImageRemoved = onExistingImageRemoved,
                        onAddImagesClick = onAddImagesClick,
                    )
                    Text(
                        stringResource(R.string.images_count, state.totalImagesCount, ValidationRules.MAX_POST_IMAGES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedButton(
                        onClick = onAddImagesClick,
                        enabled = !state.isPublishing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_photo))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageThumbnail(
    model: String,
    onRemove: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = CircleShape,
                ),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AddImageThumbnail(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(R.string.add_image),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImagesRow(
    state: SavePostUiState,
    onNewImageRemoved: (Int) -> Unit,
    onExistingImageRemoved: (Int) -> Unit,
    onAddImagesClick: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(state.existingImages) { index, image ->
            ImageThumbnail(
                model = image.url,
                onRemove = { onExistingImageRemoved(index) },
                enabled = !state.isPublishing,
            )
        }
        itemsIndexed(state.newImageUris) { index, uri ->
            ImageThumbnail(
                model = uri,
                onRemove = { onNewImageRemoved(index) },
                enabled = !state.isPublishing,
            )
        }
        if (state.totalImagesCount < ValidationRules.MAX_POST_IMAGES) {
            item {
                AddImageThumbnail(
                    enabled = !state.isPublishing,
                    onClick = onAddImagesClick,
                )
            }
        }
    }
}

@Composable
private fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FullScreenMessage(
    message: String,
    actionText: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (actionText != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction) { Text(actionText) }
        }
    }
}