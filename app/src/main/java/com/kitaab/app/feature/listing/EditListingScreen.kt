package com.kitaab.app.feature.listing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kitaab.app.feature.post.BarcodeScanButton
import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.feature.post.ListingType

private val EXAM_TAGS = listOf("JEE", "NEET", "UPSC", "CAT", "GATE", "College", "Other")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditListingScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditListingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents(),
        ) { uris: List<Uri> -> if (uris.isNotEmpty()) viewModel.onNewPhotosSelected(uris) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditListingEvent.SaveSuccess -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit listing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Barcode scanner ───────────────────────────────────────────────
            BarcodeScanButton(
                isFetchingBookDetails = state.isFetchingBookDetails,
                onIsbnScanned = viewModel::onIsbnScanned,
            )

            // ── Core fields ───────────────────────────────────────────────────
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("Title *") },
                isError = state.titleError != null,
                supportingText = state.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.author,
                    onValueChange = viewModel::onAuthorChanged,
                    label = { Text("Author") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.publisher,
                    onValueChange = viewModel::onPublisherChanged,
                    label = { Text("Publisher") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = viewModel::onSubjectChanged,
                    label = { Text("Subject") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.edition,
                    onValueChange = viewModel::onEditionChanged,
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
                            selected = tag in state.examTags,
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
                    color =
                        if (state.conditionError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BookCondition.entries.forEach { condition ->
                        FilterChip(
                            selected = state.condition == condition,
                            onClick = { viewModel.onConditionSelected(condition) },
                            label = { Text(condition.label) },
                        )
                    }
                }
                state.conditionError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ── Toggles ───────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Has solutions", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.hasSolutions,
                    onCheckedChange = { viewModel.onHasSolutionsToggled() },
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Has handwritten notes", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.hasNotes,
                    onCheckedChange = { viewModel.onHasNotesToggled() },
                )
            }

            // ── Listing type ──────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Listing type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.listingType == ListingType.SELL,
                        onClick = { viewModel.onListingTypeChanged(ListingType.SELL) },
                        label = { Text("Sell") },
                    )
                    FilterChip(
                        selected = state.listingType == ListingType.DONATE,
                        onClick = { viewModel.onListingTypeChanged(ListingType.DONATE) },
                        label = { Text("Donate") },
                    )
                }
            }

            if (state.listingType == ListingType.SELL) {
                OutlinedTextField(
                    value = state.price, onValueChange = viewModel::onPriceChanged,
                    label = { Text("Price (₹) *") },
                    isError = state.priceError != null,
                    supportingText = state.priceError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹") },
                )
            }

            // ── Location ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.city,
                onValueChange = viewModel::onCityChanged,
                label = { Text("City *") },
                isError = state.cityError != null,
                supportingText = state.cityError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.pincode,
                    onValueChange = viewModel::onPincodeChanged,
                    label = { Text("Pincode *") },
                    isError = state.pincodeError != null,
                    supportingText = state.pincodeError?.let { { Text(it) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.locality,
                    onValueChange = viewModel::onLocalityChanged,
                    label = { Text("Locality") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            // ── Photos ────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Photos", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${state.totalPhotoCount}/5 · first is cover",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Existing remote photos
                    state.existingPhotoUrls.forEachIndexed { index, url ->
                        EditPhotoThumbnail(
                            model = url,
                            isCover = index == 0 && state.newPhotoUris.isEmpty(),
                            onRemove = { viewModel.onExistingPhotoRemoved(url) },
                            onSetCover = { viewModel.onExistingPhotoCover(url) },
                        )
                    }
                    // New local photos
                    state.newPhotoUris.forEachIndexed { index, uri ->
                        EditPhotoThumbnail(
                            model = uri,
                            isCover = index == 0 && state.existingPhotoUrls.isEmpty(),
                            onRemove = { viewModel.onNewPhotoRemoved(uri) },
                            onSetCover = { viewModel.onNewPhotoCover(uri) },
                        )
                    }
                    // Add button
                    if (state.canAddMorePhotos) {
                        Box(
                            modifier =
                                Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                enabled = !state.isSubmitting,
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save changes")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EditPhotoThumbnail(
    model: Any,
    isCover: Boolean,
    onRemove: () -> Unit,
    onSetCover: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isCover) 2.dp else 0.5.dp,
                    color = if (isCover) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable(onClick = onSetCover),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        if (isCover) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Cover",
                tint = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
