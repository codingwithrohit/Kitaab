package com.kitaab.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kitaab.app.domain.model.UserProfile
import com.kitaab.app.feature.home.components.ListingGridCard
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal900
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    onNavigateBack: () -> Unit,
    onListingClick: (String) -> Unit,
    viewModel: SellerProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.profile?.name?.ifBlank { "Seller" } ?: "Seller",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.load() },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (state.isLoading && state.profile == null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator(color = Teal500)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = 32.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Profile header — full width
                    item(span = { GridItemSpan(2) }) {
                        state.profile?.let { profile ->
                            SellerProfileHeader(profile = profile)
                        }
                    }

                    // Stats row — full width
                    item(span = { GridItemSpan(2) }) {
                        state.profile?.let { profile ->
                            SellerStatsRow(profile = profile)
                        }
                    }

                    // Divider — full width
                    item(span = { GridItemSpan(2) }) {
                        HorizontalDivider(
                            color = WarmBorder,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    // Listings header — full width
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text =
                                if (state.listings.isEmpty()) {
                                    "No active listings"
                                } else {
                                    "Active listings (${state.listings.size})"
                                },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    start = 4.dp,
                                    top = 4.dp,
                                    bottom = 4.dp,
                                ),
                        )
                    }

                    // Empty state — full width
                    if (state.listings.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "📚", fontSize = 40.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No active listings right now",
                                        fontSize = 14.sp,
                                        color = WarmMuted,
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = state.listings,
                            key = { it.id },
                        ) { listing ->
                            ListingGridCard(
                                listing = listing,
                                onClick = { onListingClick(listing.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerProfileHeader(profile: UserProfile) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!profile.profilePhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile.profilePhotoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    tint = WarmMuted,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = profile.name.ifBlank { "Kitaab user" },
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (!profile.city.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "📍 ${profile.city}",
                fontSize = 13.sp,
                color = WarmMuted,
            )
        }

        if (!profile.badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.badge,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Teal900,
                modifier =
                    Modifier
                        .background(Teal50, RoundedCornerShape(20.dp))
                        .border(1.dp, Teal500.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        if (profile.reviewCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "⭐ ${profile.avgRating} · ${profile.reviewCount} reviews",
                fontSize = 13.sp,
                color = WarmMuted,
            )
        }
    }
}

@Composable
private fun SellerStatsRow(profile: UserProfile) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SellerStatItem(value = profile.totalSold.toString(), label = "Sold")
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(WarmBorder),
        )
        SellerStatItem(value = profile.totalDonated.toString(), label = "Donated")
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(WarmBorder),
        )
        SellerStatItem(
            value =
                if (profile.reviewCount > 0) {
                    "⭐ ${profile.avgRating}"
                } else {
                    "—"
                },
            label = "Rating",
        )
    }
}

@Composable
private fun SellerStatItem(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = WarmMuted,
        )
    }
}
