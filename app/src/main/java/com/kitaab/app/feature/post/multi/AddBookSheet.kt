package com.kitaab.app.feature.post.multi

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kitaab.app.feature.post.BarcodeScanButton
import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.feature.post.ListingType

private val EXAM_TAGS = listOf("JEE", "NEET", "UPSC", "CA", "GATE", "Board", "Other")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBookSheet(
    viewModel: MultiPostViewModel,
    sessionDefaultType: ListingType,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheet = state.addBookSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.onBookPhotosSelected(uris)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Title bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (sheet.isEditing) "Edit book" else "Add book",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (sheet.isFetchingBookDetails) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (sheet.bookNotFound) {
                Text(
                    "Book not found — fill in the details below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Barcode scanner ───────────────────────────────────────────────
            BarcodeScanButton(
                isFetchingBookDetails = sheet.isFetchingBookDetails,
                onIsbnScanned = viewModel::onBookIsbnScanned,
            )

            if (sheet.isbn.isNotBlank()) {
                Text(
                    "ISBN: ${sheet.isbn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (sheet.bookNotFound) {
                Text(
                    "Book not found — fill in the details below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // ── Title ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = sheet.title,
                onValueChange = viewModel::onBookTitleChanged,
                label = { Text("Title *") },
                isError = sheet.titleError != null,
                supportingText = sheet.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // ── Author + Publisher ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sheet.author,
                    onValueChange = viewModel::onBookAuthorChanged,
                    label = { Text("Author") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = sheet.publisher,
                    onValueChange = viewModel::onBookPublisherChanged,
                    label = { Text("Publisher") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            // ── Subject + Edition ─────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sheet.subject,
                    onValueChange = viewModel::onBookSubjectChanged,
                    label = { Text("Subject") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = sheet.edition,
                    onValueChange = viewModel::onBookEditionChanged,
                    label = { Text("Edition") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            // ── Exam tags ─────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Exam tags", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EXAM_TAGS.forEach { tag ->
                        FilterChip(
                            selected = tag in sheet.examTags,
                            onClick = { viewModel.onExamTagToggled(tag) },
                            label = { Text(tag) },
                        )
                    }
                }
            }

            // ── Condition ─────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Condition *",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sheet.conditionError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BookCondition.entries.forEach { condition ->
                        FilterChip(
                            selected = sheet.condition == condition,
                            onClick = { viewModel.onBookConditionSelected(condition) },
                            label = { Text(condition.label) },
                        )
                    }
                }
                sheet.conditionError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ── Toggles ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Has solutions", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = sheet.hasSolutions,
                    onCheckedChange = { viewModel.onHasSolutionsToggled() })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Has handwritten notes", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = sheet.hasNotes,
                    onCheckedChange = { viewModel.onHasNotesToggled() })
            }

            // ── Type override ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Listing type", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Default: ${
                        sessionDefaultType.name.lowercase().replaceFirstChar { it.uppercaseChar() }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = (sheet.typeOverride ?: sessionDefaultType) == ListingType.SELL,
                        onClick = {
                            val newType =
                                if (sheet.typeOverride == ListingType.SELL) null else ListingType.SELL
                            viewModel.onBookTypeOverrideSelected(newType)
                        },
                        label = { Text("Sell") },
                    )
                    FilterChip(
                        selected = (sheet.typeOverride ?: sessionDefaultType) == ListingType.DONATE,
                        onClick = {
                            val newType =
                                if (sheet.typeOverride == ListingType.DONATE) null else ListingType.DONATE
                            viewModel.onBookTypeOverrideSelected(newType)
                        },
                        label = { Text("Donate") },
                    )
                }
            }

            // ── Individual price ──────────────────────────────────────────────
            val effectiveType = sheet.typeOverride ?: sessionDefaultType
            OutlinedTextField(
                value = sheet.individualPrice,
                onValueChange = viewModel::onBookIndividualPriceChanged,
                label = {
                    Text(if (effectiveType == ListingType.DONATE) "MRP / reference price (optional)" else "Price (₹) *")
                },
                isError = sheet.priceError != null,
                supportingText = sheet.priceError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹") },
            )

            // ── Photos ────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Photos", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${sheet.photoUris.size}/5 · first photo is cover",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sheet.photoUris.forEachIndexed { index, uri ->
                        PhotoThumbnail(
                            uri = uri,
                            isCover = index == 0,
                            onRemove = { viewModel.onBookPhotoRemoved(uri) },
                            onSetCover = { viewModel.onBookCoverPhotoSet(uri) },
                        )
                    }
                    if (sheet.photoUris.size < 5) {
                        AddPhotoPlaceholder(onClick = { photoPickerLauncher.launch("image/*") })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.confirmBook() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sheet.isCopyingPhotos,
            ) {
                Text(if (sheet.isCopyingPhotos) "Saving…" else if (sheet.isEditing) "Save changes" else "Add to session")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhotoThumbnail(
    uri: Uri,
    isCover: Boolean,
    onRemove: () -> Unit,
    onSetCover: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isCover) 2.dp else 0.5.dp,
                color = if (isCover) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onSetCover),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        // Cover star badge
        if (isCover) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Cover photo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp),
            )
        }
        // Remove button
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove photo",
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun AddPhotoPlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add photo",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}