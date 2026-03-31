package com.kitaab.app.feature.post.multi

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kitaab.app.domain.model.StagedBook
import com.kitaab.app.domain.model.StagedBundle
import com.kitaab.app.feature.post.ListingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganiseScreen(
    viewModel: MultiPostViewModel,
    onBack: () -> Unit,
    onNavigateToReview: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organise listings") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelection()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.selectedBookIds.isEmpty()) {
                        TextButton(onClick = onNavigateToReview) { Text("Review") }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 140.dp,
                ),
            ) {
                // ── Bundles (with their member books) ──────────────────────────
                if (state.stagedBundles.isNotEmpty()) {
                    item {
                        Text(
                            "Bundles",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(state.stagedBundles, key = { "bundle_${it.id}" }) { bundle ->
                        BundleCard(
                            bundle = bundle,
                            books = state.booksForBundle(bundle.id),
                            sessionDefaultType = state.sessionDefaults.listingType,
                            onEditBundle = { viewModel.openEditBundleSheet(bundle.id) },
                            onUngroup = { viewModel.ungroupBundle(bundle.id) },
                        )
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                }

                // ── Individual books (unbundled) ────────────────────────────────
                if (state.unbundledBooks.isNotEmpty()) {
                    item {
                        Text(
                            "Individual listings",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(state.unbundledBooks, key = { it.id }) { book ->
                        SelectableBookRow(
                            book = book,
                            isSelected = book.id in state.selectedBookIds,
                            sessionDefaultType = state.sessionDefaults.listingType,
                            onToggle = { viewModel.onBookSelectionToggled(book.id) },
                        )
                    }
                }
            }

            // ── Bottom action bar ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.selectedBookIds.isNotEmpty()) {
                    val count = state.selectedBookIds.size
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "$count book${if (count == 1) "" else "s"} selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            if (count < 2) {
                                Text(
                                    "Select at least 2 books to create a bundle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.openCreateBundleSheet() },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Create bundle") }
                                    TextButton(onClick = { viewModel.clearSelection() }) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onNavigateToReview,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Review ${state.totalListingCount} listing${if (state.totalListingCount == 1) "" else "s"}")
                    }
                }
            }
        }
    }

    // Bundle sheet
    if (state.createBundleSheet.isVisible) {
        CreateBundleSheet(
            viewModel = viewModel,
            sessionDefaultType = state.sessionDefaults.listingType,
            selectedBookTitles = state.stagedBooks
                .filter { it.id in state.selectedBookIds }
                .map { it.title },
            onDismiss = { viewModel.dismissCreateBundleSheet() },
        )
    }
}

@Composable
private fun SelectableBookRow(
    book: StagedBook,
    isSelected: Boolean,
    sessionDefaultType: ListingType,
    onToggle: () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "border",
    )

    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val effectiveType = book.typeOverride ?: sessionDefaultType
                Text(
                    buildString {
                        append(
                            effectiveType.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                        if (book.individualPrice.isNotBlank()) append(" · ₹${book.individualPrice}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BundleCard(
    bundle: StagedBundle,
    books: List<StagedBook>,
    sessionDefaultType: ListingType,
    onEditBundle: () -> Unit,
    onUngroup: () -> Unit,
) {
    val effectiveType = bundle.typeOverride ?: sessionDefaultType
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        bundle.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        buildString {
                            append(
                                "Bundle · ${books.size} books · ${
                                    effectiveType.name.lowercase()
                                        .replaceFirstChar { it.uppercaseChar() }
                                }"
                            )
                            if (effectiveType == ListingType.SELL && bundle.bundlePrice.isNotBlank()) {
                                append(" · ₹${bundle.bundlePrice}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                TextButton(onClick = onEditBundle) { Text("Edit") }
                TextButton(onClick = onUngroup) { Text("Ungroup") }
            }
            books.forEach { book ->
                Text(
                    "· ${book.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

// ── Create / Edit bundle sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBundleSheet(
    viewModel: MultiPostViewModel,
    sessionDefaultType: ListingType,
    selectedBookTitles: List<String>,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheet = state.createBundleSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val effectiveType = sheet.typeOverride ?: sessionDefaultType

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (sheet.isEditing) "Edit bundle" else "Create bundle",
                style = MaterialTheme.typography.titleMedium,
            )

            // Show which books are in this bundle (only for new bundles)
            if (!sheet.isEditing && selectedBookTitles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${selectedBookTitles.size} books in this bundle:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    selectedBookTitles.forEach { title ->
                        Text(
                            "· $title",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = sheet.name,
                onValueChange = viewModel::onBundleNameChanged,
                label = { Text("Bundle name *") },
                placeholder = { Text("e.g. Physics combo, JEE Maths set") },
                isError = sheet.nameError != null,
                supportingText = sheet.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Type override
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Listing type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = effectiveType == ListingType.SELL,
                        onClick = {
                            val t =
                                if (sheet.typeOverride == ListingType.SELL) null else ListingType.SELL
                            viewModel.onBundleTypeOverrideSelected(t)
                        },
                        label = { Text("Sell") },
                    )
                    FilterChip(
                        selected = effectiveType == ListingType.DONATE,
                        onClick = {
                            val t =
                                if (sheet.typeOverride == ListingType.DONATE) null else ListingType.DONATE
                            viewModel.onBundleTypeOverrideSelected(t)
                        },
                        label = { Text("Donate") },
                    )
                }
            }

            if (effectiveType == ListingType.SELL) {
                OutlinedTextField(
                    value = sheet.bundlePrice,
                    onValueChange = viewModel::onBundlePriceChanged,
                    label = { Text("Bundle price (₹) *") },
                    isError = sheet.priceError != null,
                    supportingText = sheet.priceError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹") },
                )
            }

            Button(
                onClick = { viewModel.confirmBundle() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (sheet.isEditing) "Save bundle" else "Create bundle")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}