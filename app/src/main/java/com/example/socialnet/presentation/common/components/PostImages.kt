package com.example.socialnet.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.socialnet.core.model.ImageRef

@Composable
fun PostImages(
    modifier: Modifier = Modifier,
    images: List<ImageRef.Remote>,
    onImageClick: (index: Int) -> Unit = {},
) {
    if (images.isEmpty()) return

    val shape = RoundedCornerShape(12.dp)

    when (images.size) {
        1 -> {
            AsyncImage(
                model = images[0].url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onImageClick(0) },
            )
        }
        2 -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            images.forEachIndexed { index, img ->
                ImageCell(
                    url = img.url,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = shape,
                    onClick = { onImageClick(index) },
                )
            }
        }
        3 -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ImageCell(
                url = images[0].url,
                modifier = Modifier.weight(2f).aspectRatio(2f / 3f),
                shape = shape,
                onClick = { onImageClick(0) },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ImageCell(
                    url = images[1].url,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = shape,
                    onClick = { onImageClick(1) },
                )
                ImageCell(
                    url = images[2].url,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = shape,
                    onClick = { onImageClick(2) },
                )
            }
        }
        else -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ImageCell(
                    url = images[0].url,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = shape,
                    onClick = { onImageClick(0) },
                )
                ImageCell(
                    url = images[1].url,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = shape,
                    onClick = { onImageClick(1) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ImageCell(
                    url = images[2].url,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = shape,
                    onClick = { onImageClick(2) },
                )
                ImageCell(
                    url = images[3].url,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = shape,
                    overlayCount = if (images.size > 4) images.size - 4 else null,
                    // Клик по ячейке с +N открывает просмотр с 4-го изображения
                    onClick = { onImageClick(3) },
                )
            }
        }
    }
}

@Composable
private fun ImageCell(
    url: String,
    modifier: Modifier,
    shape: Shape,
    overlayCount: Int? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (overlayCount != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$overlayCount",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}