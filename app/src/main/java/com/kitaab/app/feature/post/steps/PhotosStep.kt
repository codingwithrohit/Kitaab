package com.kitaab.app.feature.post.steps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import coil.compose.AsyncImage
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@Composable
fun PhotosStep(
    photoUris: List<Uri>,
    onPhotosSelected: (List<Uri>) -> Unit,
    onPhotoRemoved: (Uri) -> Unit,
) {
    val remaining = 5 - photoUris.size

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = remaining.coerceAtLeast(1))
    ) { uris ->
        if (uris.isNotEmpty()) onPhotosSelected(uris)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Add photos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Up to 5 photos — listings with photos sell 3x faster",
            fontSize = 14.sp,
            color = WarmMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${photoUris.size}/5 photos added",
            fontSize = 13.sp,
            color = if (photoUris.isNotEmpty()) Teal500 else WarmMuted,
            fontWeight = if (photoUris.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(
                // Fixed height to avoid nested scroll conflict
                ((photoUris.size + 1).coerceAtMost(6) / 3 + 1).coerceAtLeast(1) * 120.dp
            ),
        ) {
            items(photoUris) { uri ->
                PhotoThumbnail(
                    uri = uri,
                    onRemove = { onPhotoRemoved(uri) },
                )
            }

            if (photoUris.size < 5) {
                item {
                    AddPhotoButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tip: Include front cover, back cover, and any highlighted pages",
            fontSize = 12.sp,
            color = WarmMuted,
            lineHeight = 18.sp,
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PhotoThumbnail(
    uri: Uri,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp)),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Remove photo",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(1.dp, WarmBorder, RoundedCornerShape(10.dp))
            .background(Teal50, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Add photo",
                tint = Teal500,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add",
                fontSize = 12.sp,
                color = Teal500,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}