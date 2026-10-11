package com.kitaab.app.feature.post.multi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kitaab.app.domain.model.StagedBook
import com.kitaab.app.domain.model.StagedBundle
import com.kitaab.app.feature.post.ListingType
import com.kitaab.app.ui.theme.Amber50
import com.kitaab.app.ui.theme.Amber500
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal700
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StagingTrayScreen(
    viewModel: MultiPostViewModel,
    onNavigateToReview: () -> Unit,
    onDiscardSession: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val isSelecting = state.selectedBookIds.isNotEmpty()

    val anySheetOpen = (
        state.showSessionDefaultsSheet ||
            state.addBookSheet.isVisible ||
            state.createBundleSheet.isVisible
    )

    BackHandler(enabled = !anySheetOpen) {
        if (isSelecting) {
            viewModel.clearSelection()
        } else if (state.sessionDefaultsRequiredBanner && state.sessionId == null) {
            // No session started yet — exit directly, nothing to save
            viewModel.saveAndExitSession()
        } else {
            showDiscardDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting) {
                        Text(
                            "${state.selectedBookIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    } else {
                        Column {
                            Text(
                                "My books",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (state.totalBookCount > 0) {
                                Text(
                                    "${state.totalBookCount} book${if (state.totalBookCount == 1) "" else "s"} · ${
                                        state.sessionDefaults.listingType.name
                                            .lowercase().replaceFirstChar { it.uppercaseChar() }
                                    } · ${state.sessionDefaults.city.ifBlank { "No location" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = { showDiscardDialog = true }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
                        }
                    }
                },
                actions = {
                    if (isSelecting && state.canCreateBundleFromSelection) {
                        TextButton(onClick = { viewModel.openCreateBundleSheet() }) {
                            Text("Bundle (${state.selectedBookIds.size})")
                        }
                    } else if (!isSelecting) {
                        // Settings chip — tap to change session defaults
                        TextButton(onClick = { viewModel.openSessionDefaultsSheet() }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit defaults",
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Defaults", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (state.stagedBooks.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.sessionDefaultsRequiredBanner) {
                        // Location required banner — shown after sheet dismissed without confirming
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "Location required",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    "Set your pickup location to start adding books.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Button(
                                    onClick = { viewModel.openSessionDefaultsSheet() },
                                    colors =
                                        androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                        ),
                                ) {
                                    Text("Set location")
                                }
                            }
                        }
                    } else {
                        TrayEmptyState(onAddBook = { viewModel.openAddBookSheet() })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 88.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // ── Bundles ──────────────────────────────────────────────
                    if (state.stagedBundles.isNotEmpty()) {
                        item(key = "bundles_header") {
                            TraySectionHeader(
                                title = "Bundles",
                                count = state.stagedBundles.size,
                            )
                        }
                        items(state.stagedBundles, key = { "bundle_${it.id}" }) { bundle ->
                            BundleTrayCard(
                                bundle = bundle,
                                books = state.booksForBundle(bundle.id),
                                sessionDefaultType = state.sessionDefaults.listingType,
                                onEdit = { viewModel.openEditBundleSheet(bundle.id) },
                                onUngroup = { viewModel.ungroupBundle(bundle.id) },
                            )
                        }
                        item(key = "bundles_divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = WarmBorder,
                            )
                        }
                    }

                    // ── Individual books ─────────────────────────────────────
                    if (state.unbundledBooks.isNotEmpty()) {
                        item(key = "books_header") {
                            TraySectionHeader(
                                title = "Individual books",
                                count = state.unbundledBooks.size,
                                hint =
                                    if (state.unbundledBooks.size >= 2) {
                                        "Long-press to select and bundle"
                                    } else {
                                        null
                                    },
                            )
                        }
                        items(state.unbundledBooks, key = { it.id }) { book ->
                            BookTrayCard(
                                book = book,
                                isSelected = book.id in state.selectedBookIds,
                                isSelectionMode = isSelecting,
                                sessionDefaultType = state.sessionDefaults.listingType,
                                onTap = {
                                    if (isSelecting) {
                                        viewModel.onBookSelectionToggled(book.id)
                                    } else {
                                        viewModel.openEditBookSheet(book.id)
                                    }
                                },
                                onLongPress = {
                                    viewModel.onBookSelectionToggled(book.id)
                                },
                                onDelete = { viewModel.deleteBook(book.id) },
                            )
                        }
                    }
                }
            }

            // ── Bottom bar ────────────────────────────────────────────────────
            if (state.totalBookCount > 0 && !isSelecting) {
                BottomPublishBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    totalListingCount = state.totalListingCount,
                    allReady = state.allListingsReady,
                    onAddBook = { viewModel.openAddBookSheet() },
                    onReview = onNavigateToReview,
                )
            }

            // ── Selection action bar ──────────────────────────────────────────
            if (isSelecting) {
                SelectionActionBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    selectedCount = state.selectedBookIds.size,
                    canBundle = state.canCreateBundleFromSelection,
                    onBundle = { viewModel.openCreateBundleSheet() },
                    onClear = { viewModel.clearSelection() },
                )
            }
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────────
    if (state.showSessionDefaultsSheet) {
        SessionDefaultsSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.onSessionDefaultsSheetDismissed() },
        )
    }

    if (state.addBookSheet.isVisible) {
        AddBookSheet(
            viewModel = viewModel,
            sessionDefaultType = state.sessionDefaults.listingType,
            onDismiss = { viewModel.dismissAddBookSheet() },
        )
    }

    if (state.createBundleSheet.isVisible) {
        CreateBundleSheet(
            viewModel = viewModel,
            sessionDefaultType = state.sessionDefaults.listingType,
            selectedBookTitles =
                state.stagedBooks
                    .filter { it.id in state.selectedBookIds }
                    .map { it.title },
            onDismiss = { viewModel.dismissCreateBundleSheet() },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Leave session?") },
            text = {
                val bookCount = state.totalBookCount
                val message =
                    if (bookCount == 0) {
                        "You haven't added any books yet."
                    } else {
                        val pluralSuffix = if (bookCount == 1) "" else "s"
                        "Your $bookCount book$pluralSuffix will be saved. You can continue later."
                    }

                Text(text = message)
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.saveAndExitSession()
                }) {
                    Text("Save & exit")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Keep editing")
                    }
                    TextButton(onClick = {
                        showDiscardDialog = false
                        viewModel.discardCurrentSession()
                    }) {
                        Text(
                            "Discard",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun TraySectionHeader(
    title: String,
    count: Int,
    hint: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = WarmMuted,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookTrayCard(
    book: StagedBook,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    sessionDefaultType: ListingType,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
) {
    val effectiveType = book.effectiveType(sessionDefaultType)
    val isOverridden = book.typeOverride != null

    val borderColor by animateColorAsState(
        if (isSelected) Teal500 else WarmBorder,
        label = "border",
    )
    val borderWidth by animateDpAsState(
        if (isSelected) 1.5.dp else 0.5.dp,
        label = "borderWidth",
    )
    val bgColor by animateColorAsState(
        if (isSelected) Teal50 else MaterialTheme.colorScheme.surface,
        label = "bg",
    )

    val hasIssue = !book.isReadyToPublish(sessionDefaultType)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = onLongPress,
                ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Selection indicator or condition dot
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelectionMode) {
                    Box(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Teal500 else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) Teal500 else WarmBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                } else {
                    // Condition colour dot
                    val conditionColor =
                        when (book.condition?.name) {
                            "New", "LikeNew" -> Teal500
                            "Good" -> Teal700
                            "Fair" -> Amber500
                            else -> WarmMuted
                        }
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(conditionColor),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Type pill
                    val pillBg = if (effectiveType == ListingType.DONATE) Teal50 else Amber50
                    val pillText = if (effectiveType == ListingType.DONATE) Teal700 else Amber500
                    Box(
                        modifier =
                            Modifier
                                .background(pillBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (effectiveType == ListingType.DONATE) "Donate" else "Sell",
                            fontSize = 10.sp,
                            color = pillText,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (isOverridden) {
                        Text("overridden", fontSize = 10.sp, color = WarmMuted)
                    }

                    val priceText =
                        when {
                            effectiveType == ListingType.SELL && book.individualPrice.isNotBlank() ->
                                "₹${book.individualPrice}"

                            effectiveType == ListingType.SELL -> "no price"
                            else -> ""
                        }
                    if (priceText.isNotBlank()) {
                        Text(
                            priceText,
                            fontSize = 11.sp,
                            color =
                                if (effectiveType == ListingType.SELL && book.individualPrice.isBlank()) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    WarmMuted
                                },
                            fontWeight =
                                if (effectiveType == ListingType.SELL && book.individualPrice.isBlank()) {
                                    FontWeight.Medium
                                } else {
                                    FontWeight.Normal
                                },
                        )
                    }

                    if (hasIssue) {
                        Text(
                            "·",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            book.warningText(sessionDefaultType) ?: "Incomplete",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Actions — only shown when not in selection mode
            if (!isSelectionMode) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = WarmMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove book?") },
            text = { Text("\"${book.title}\" will be removed from this session.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun BundleTrayCard(
    bundle: StagedBundle,
    books: List<StagedBook>,
    sessionDefaultType: ListingType,
    onEdit: () -> Unit,
    onUngroup: () -> Unit,
) {
    val effectiveType = bundle.typeOverride ?: sessionDefaultType
    val hasIssue = !bundle.isReadyToPublish(sessionDefaultType) || books.isEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = Amber50),
        shape = RoundedCornerShape(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .border(0.5.dp, Amber500.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Bundle label
                        Box(
                            modifier =
                                Modifier
                                    .background(Amber500, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "Bundle",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            bundle.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        buildString {
                            append("${books.size} book${if (books.size == 1) "" else "s"}")
                            append(" · ")
                            append(
                                effectiveType.name.lowercase()
                                    .replaceFirstChar { it.uppercaseChar() },
                            )
                            if (effectiveType == ListingType.SELL && bundle.bundlePrice.isNotBlank()) {
                                append(" · ₹${bundle.bundlePrice}")
                            } else if (effectiveType == ListingType.SELL) {
                                append(" · no price")
                            }
                        },
                        fontSize = 11.sp,
                        color = if (hasIssue) MaterialTheme.colorScheme.error else Amber500,
                    )
                }

                TextButton(onClick = onEdit) { Text("Edit", fontSize = 12.sp) }
                TextButton(onClick = onUngroup) {
                    Text("Ungroup", fontSize = 12.sp, color = WarmMuted)
                }
            }

            // Book list inside bundle
            if (books.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Amber500.copy(alpha = 0.2f))
                Spacer(Modifier.height(6.dp))
                books.forEach { book ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(5.dp)
                                    .background(Amber500.copy(alpha = 0.6f), CircleShape),
                        )
                        Text(
                            book.title,
                            fontSize = 12.sp,
                            color = WarmMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        book.condition?.let {
                            Text(it.label, fontSize = 10.sp, color = WarmMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomPublishBar(
    modifier: Modifier,
    totalListingCount: Int,
    allReady: Boolean,
    onAddBook: () -> Unit,
    onReview: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$totalListingCount listing${if (totalListingCount == 1) "" else "s"} ready",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (!allReady) {
                    Text(
                        "Some listings need attention",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedButton(onClick = onAddBook) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
            Button(
                onClick = onReview,
                enabled = allReady,
            ) {
                Text("Review")
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    modifier: Modifier,
    selectedCount: Int,
    canBundle: Boolean,
    onBundle: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!canBundle) {
                Text(
                    "Select at least 2 books to create a bundle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                ) { Text("Clear selection") }
                Button(
                    onClick = onBundle,
                    enabled = canBundle,
                    modifier = Modifier.weight(1f),
                ) { Text("Bundle $selectedCount books") }
            }
        }
    }
}

@Composable
private fun TrayEmptyState(onAddBook: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Your tray is empty",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Add books by scanning a barcode, taking a cover photo, or typing manually.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Button(onClick = onAddBook) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add your first book")
        }
    }
}

// @Composable
// private fun TrayEmptyState(
//    modifier: Modifier = Modifier,
//    onAddBook: () -> Unit,
// ) {
//    Column(
//        modifier = modifier.padding(32.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(12.dp),
//    ) {
//        Text(
//            "Your tray is empty",
//            style = MaterialTheme.typography.titleMedium,
//        )
//        Text(
//            "Add books by scanning a barcode, taking a cover photo, or typing manually.",
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
//        )
//        Button(onClick = onAddBook) {
//            Icon(Icons.Default.Add, contentDescription = null)
//            Spacer(Modifier.width(6.dp))
//            Text("Add your first book")
//        }
//    }
// }
