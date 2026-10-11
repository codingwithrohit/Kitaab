package com.kitaab.app.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@Composable
fun ListingGridCard(
    listing: Listing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        border = BorderStroke(0.5.dp, WarmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // ── Photo / placeholder area ──────────────────────────────
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            ) {
                if (listing.isBundle) {
                    BundleSpineStack(
                        photoUrls = listing.photoUrls,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    BookCoverImage(
                        url = listing.photoUrls.firstOrNull(),
                        modifier = Modifier.matchParentSize(),
                    )
                }

                // Type badge — top-left overlay on photo
                ListingTypeBadge(
                    listing = listing,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                )
            }

            // ── Text content ──────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = listing.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (listing.isBundle) {
                    Text(
                        text = "${listing.bookCount} books",
                        fontSize = 11.sp,
                        color = WarmMuted,
                    )
                } else if (!listing.author.isNullOrBlank()) {
                    Text(
                        text = listing.author,
                        fontSize = 11.sp,
                        color = WarmMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Price — dominant
                    val priceText =
                        when {
                            listing.type == "DONATE" -> "FREE"
                            listing.price != null -> "₹${listing.price.toInt()}"
                            else -> ""
                        }
                    val priceColor =
                        when {
                            listing.isBundle -> MaterialTheme.colorScheme.tertiary
                            else -> Teal500
                        }
                    Text(
                        text = priceText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = priceColor,
                    )

                    // Condition — muted, only for single books
                    if (!listing.isBundle) {
                        Text(
                            text = listing.condition,
                            fontSize = 11.sp,
                            color = WarmMuted,
                        )
                    }
                }

                val locationParts =
                    listOfNotNull(
                        listing.locality?.takeIf { it.isNotBlank() },
                        listing.city?.takeIf { it.isNotBlank() },
                    ).joinToString(", ")

                if (locationParts.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 $locationParts",
                        fontSize = 10.sp,
                        color = WarmMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Overlapping spine stack shown in the photo area of bundle grid cards.
 * Shows up to 3 spines offset horizontally to give a "stack of books" feel.
 * Falls back to a plain tinted placeholder if no photos exist.
 */
@Composable
private fun BundleSpineStack(
    photoUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    val visibleUrls = photoUrls.take(3)
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (visibleUrls.isEmpty()) {
            // No photos — show stacked placeholder spines
            BundlePlaceholderSpines()
        } else {
            // Show overlapping cover photos
            visibleUrls.forEachIndexed { index, url ->
                val offsetX = (index * 16).dp
                val offsetY = (index * 6).dp
                BookCoverImage(
                    url = url,
                    modifier =
                        Modifier
                            .size(width = 80.dp, height = 110.dp)
                            .offset(x = offsetX - 24.dp, y = offsetY - 16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
private fun BundlePlaceholderSpines() {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alphas = listOf(0.35f, 0.55f, 0.8f)
        val widths = listOf(28.dp, 32.dp, 36.dp)
        val heights = listOf(80.dp, 92.dp, 104.dp)
        alphas.forEachIndexed { i, alpha ->
            Box(
                modifier =
                    Modifier
                        .size(width = widths[i], height = heights[i])
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                        ),
            )
        }
    }
}

/**
 * Small pill badge shown over the photo: SELL / DONATE / BUNDLE
 */
@Composable
fun ListingTypeBadge(
    listing: Listing,
    modifier: Modifier = Modifier,
) {
    val (label, bgColor, textColor) =
        when {
            listing.isBundle ->
                Triple(
                    "BUNDLE",
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary,
                )

            listing.type == "DONATE" ->
                Triple(
                    "DONATE",
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.onSecondary,
                )

            else ->
                Triple(
                    "SELL",
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary,
                )
        }

    Box(
        modifier =
            modifier
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.5.sp,
        )
    }
}
