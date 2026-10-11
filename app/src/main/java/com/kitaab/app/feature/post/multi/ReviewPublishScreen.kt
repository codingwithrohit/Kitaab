package com.kitaab.app.feature.post.multi

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kitaab.app.feature.post.ListingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPublishScreen(
    viewModel: MultiPostViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Review ${state.totalListingCount} listing${if (state.totalListingCount == 1) "" else "s"}")
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isPublishing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 120.dp,
                    ),
            ) {
                // Summary header
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "${state.totalListingCount} listings",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "${state.totalBookCount} books total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${state.stagedBundles.size} bundle${if (state.stagedBundles.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${state.unbundledBooks.size} individual",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Partial failure banner
                if (state.failedListingTitles.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Some listings failed to publish",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                state.failedListingTitles.forEach {
                                    Text(
                                        "· $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                // Bundle listings
                if (state.stagedBundles.isNotEmpty()) {
                    item {
                        Text(
                            "Bundles",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.stagedBundles, key = { "review_bundle_${it.id}" }) { bundle ->
                        val books = state.booksForBundle(bundle.id)
                        val default = state.sessionDefaults.listingType
                        val effectiveType = bundle.effectiveType(default)
                        ReviewListingCard(
                            title = bundle.name,
                            subtitle =
                                "${books.size} books · ${
                                    effectiveType.name.lowercase()
                                        .replaceFirstChar { it.uppercaseChar() }
                                }" +
                                    if (effectiveType == ListingType.SELL && bundle.bundlePrice.isNotBlank()) {
                                        " · ₹${bundle.bundlePrice}"
                                    } else {
                                        ""
                                    },
                            isBundle = true,
                            isReady = bundle.isReadyToPublish(default) && books.isNotEmpty(),
                            warningText =
                                if (books.isEmpty()) {
                                    "Add at least one book"
                                } else {
                                    bundle.warningText(default)
                                },
                        )
                    }
                }

                // Individual listings
                if (state.unbundledBooks.isNotEmpty()) {
                    item {
                        Text(
                            "Individual",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(state.unbundledBooks, key = { "review_book_${it.id}" }) { book ->
                        val default = state.sessionDefaults.listingType
                        val effectiveType = book.effectiveType(default)
                        ReviewListingCard(
                            title = book.title,
                            subtitle =
                                buildString {
                                    append(
                                        effectiveType.name.lowercase()
                                            .replaceFirstChar { it.uppercaseChar() },
                                    )
                                    if (effectiveType == ListingType.SELL && book.individualPrice.isNotBlank()) {
                                        append(" · ₹${book.individualPrice}")
                                    }
                                    book.condition?.let { append(" · ${it.label}") }
                                    val photoCount = book.photoPaths.size
                                    append(" · $photoCount photo${if (photoCount == 1) "" else "s"}")
                                },
                            isBundle = false,
                            isReady = book.isReadyToPublish(default),
                            warningText = book.warningText(default),
                        )
                    }
                }
            }

            // ── Publish button ────────────────────────────────────────────────
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.publishError != null) {
                    Text(
                        state.publishError.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = { viewModel.publishAll() },
                    enabled = state.allListingsReady && !state.isPublishing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isPublishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            if (state.failedListingTitles.isNotEmpty()) {
                                "Retry failed listings"
                            } else {
                                "Publish ${state.totalListingCount} listing${if (state.totalListingCount == 1) "" else "s"}"
                            },
                        )
                    }
                }
                if (!state.allListingsReady) {
                    Text(
                        "Fix the issues above before publishing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewListingCard(
    title: String,
    subtitle: String,
    isBundle: Boolean,
    isReady: Boolean,
    warningText: String?,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isReady) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    },
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isBundle) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                "Bundle",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                warningText?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            Icon(
                if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
