package com.kitaab.app.feature.post.multi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kitaab.app.domain.model.StagedBook
import com.kitaab.app.feature.post.ListingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagingTrayScreen(
    viewModel: MultiPostViewModel,
    onNavigateToOrganise: () -> Unit,
    onDiscardSession: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.totalBookCount == 0) "Add books"
                        else "${state.totalBookCount} book${if (state.totalBookCount == 1) "" else "s"} added",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (state.stagedBooks.isEmpty()) {
                EmptyTrayState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 12.dp,
                        bottom = 160.dp, // room for bottom buttons
                    ),
                ) {
                    items(state.stagedBooks, key = { it.id }) { book ->
                        BookChip(
                            book = book,
                            sessionDefaultType = state.sessionDefaults.listingType,
                            onEdit = { viewModel.openEditBookSheet(book.id) },
                            onDelete = { viewModel.deleteBook(book.id) },
                        )
                    }
                }
            }

            // ── Bottom action bar ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Location default label
                Text(
                    text = "Default: ${
                        state.sessionDefaults.listingType.name.lowercase()
                            .replaceFirstChar { it.uppercaseChar() }
                    } · ${state.sessionDefaults.city.ifBlank { "No location" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { viewModel.openAddBookSheet() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add book")
                }
                if (state.totalBookCount > 0) {
                    Button(
                        onClick = onNavigateToOrganise,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Organise (${state.totalBookCount} books)")
                    }
                }
            }
        }
    }

    // ── Add/Edit book sheet ───────────────────────────────────────────────────
    if (state.addBookSheet.isVisible) {
        AddBookSheet(
            viewModel = viewModel,
            sessionDefaultType = state.sessionDefaults.listingType,
            onDismiss = { viewModel.dismissAddBookSheet() },
        )
    }

    // ── Discard session dialog ────────────────────────────────────────────────
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard session?") },
            text = {
                Text(
                    "All ${state.totalBookCount} book${if (state.totalBookCount == 1) "" else "s"} added so far will be lost.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardCurrentSession()
                    },
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun EmptyTrayState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("No books yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap Add book below to scan or enter your first book.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun BookChip(
    book: StagedBook,
    sessionDefaultType: ListingType,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val effectiveType = book.typeOverride ?: sessionDefaultType
    val conditionColor = when (book.condition?.name) {
        "New", "LikeNew" -> MaterialTheme.colorScheme.primary
        "Good" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Condition colour dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(conditionColor),
            )
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = buildString {
                    append(effectiveType.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                    if (effectiveType == ListingType.SELL) {
                        if (book.individualPrice.isNotBlank()) append(" · ₹${book.individualPrice}")
                        else append(" · no price yet")
                    }
                    book.condition?.let { append(" · ${it.label}") }
                    if (book.photoPaths.isEmpty()) append(" · no photos")
                    else append(" · ${book.photoPaths.size} photo${if (book.photoPaths.size == 1) "" else "s"}")
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove book",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}