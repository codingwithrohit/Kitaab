package com.kitaab.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kitaab.app.domain.model.DonationRequest
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal900
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onListingClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.SignedOut -> onNavigateToLogin()
                is ProfileEvent.AccountDeleted -> onNavigateToLogin()
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = {
                Text(
                    "This will permanently delete your account and all your listings. This cannot be undone.",
                    fontSize = 14.sp,
                    color = WarmMuted,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text(
                        text = "Profile",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit profile",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { showSignOutDialog = true }) {
                        if (state.isSigningOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Teal500,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = "Sign out",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // Profile header
                    item(key = "header") {
                        state.profile?.let { profile ->
                            ProfileHeader(profile = profile)
                        }
                    }

                    // Stats row
                    item(key = "stats") {
                        state.profile?.let { profile ->
                            StatsRow(profile = profile)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = WarmBorder)
                    }

                    // Tabs
                    item(key = "tabs") {
                        TabRow(
                            selectedTabIndex = state.selectedTab.ordinal,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = Teal500,
                            indicator = { tabPositions ->
                                SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(
                                        tabPositions[state.selectedTab.ordinal]
                                    ),
                                    color = Teal500,
                                )
                            },
                        ) {
                            Tab(
                                selected = state.selectedTab == ProfileTab.LISTINGS,
                                onClick = { viewModel.onTabSelected(ProfileTab.LISTINGS) },
                                text = {
                                    Text(
                                        "My Listings (${state.ownListings.size})",
                                        fontSize = 13.sp,
                                    )
                                },
                            )
                            Tab(
                                selected = state.selectedTab == ProfileTab.REQUESTS,
                                onClick = { viewModel.onTabSelected(ProfileTab.REQUESTS) },
                                text = {
                                    Text(
                                        "My Requests (${state.myRequests.size})",
                                        fontSize = 13.sp,
                                    )
                                },
                            )
                        }
                    }

                    // Tab content
                    if (state.selectedTab == ProfileTab.LISTINGS) {
                        if (state.ownListings.isEmpty()) {
                            item(key = "empty_listings") {
                                EmptyTabState(
                                    emoji = "📚",
                                    message = "No listings yet",
                                    subtitle = "Post your first book to get started",
                                )
                            }
                        } else {
                            items(
                                items = state.ownListings,
                                key = { it.id },
                            ) { listing ->
                                OwnListingRow(
                                    listing = listing,
                                    onClick = { onListingClick(listing.id) },
                                    onPause = { viewModel.pauseListing(listing.id) },
                                    onReactivate = { viewModel.reactivateListing(listing.id) },
                                    onMarkSold = { viewModel.markListingSold(listing.id) },
                                )
                                HorizontalDivider(
                                    color = WarmBorder,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    } else {
                        if (state.myRequests.isEmpty()) {
                            item(key = "empty_requests") {
                                EmptyTabState(
                                    emoji = "🙏",
                                    message = "No requests yet",
                                    subtitle = "Request a donated book from a listing",
                                )
                            }
                        } else {
                            items(
                                items = state.myRequests,
                                key = { it.id },
                            ) { request ->
                                DonationRequestRow(request = request)
                                HorizontalDivider(
                                    color = WarmBorder,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }

                    // Danger zone
                    item(key = "danger_zone") {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = WarmBorder)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !state.isDeletingAccount,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(44.dp),
                        ) {
                            if (state.isDeletingAccount) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delete Account",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar
        Box(
            modifier = Modifier
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

        // Badge
        if (!profile.badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.badge,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Teal900,
                modifier = Modifier
                    .background(Teal50, RoundedCornerShape(20.dp))
                    .border(1.dp, Teal500.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // Rating
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
private fun StatsRow(profile: UserProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(value = profile.totalSold.toString(), label = "Sold")
        VerticalDivider()
        StatItem(value = profile.totalDonated.toString(), label = "Donated")
        VerticalDivider()
        StatItem(
            value = if (profile.examTags.isEmpty()) "—"
            else profile.examTags.take(2).joinToString(", "),
            label = "Studying for",
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
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

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(WarmBorder),
    )
}

@Composable
private fun OwnListingRow(
    listing: Listing,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onReactivate: () -> Unit,
    onMarkSold: () -> Unit,
) {
    val statusColor = when (listing.status) {
        "ACTIVE" -> Teal500
        "PAUSED" -> WarmMuted
        "RESERVED" -> MaterialTheme.colorScheme.tertiary
        "COMPLETED" -> WarmMuted
        else -> WarmMuted
    }

    val statusLabel = when (listing.status) {
        "ACTIVE" -> "Active"
        "PAUSED" -> "Paused"
        "RESERVED" -> "Reserved"
        "COMPLETED" -> "Sold / Donated"
        else -> listing.status
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // Cover thumbnail
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 68.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!listing.photoUrls.firstOrNull().isNullOrBlank()) {
                    AsyncImage(
                        model = listing.photoUrls.first(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = Teal500.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = listing.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )

                if (!listing.author.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = listing.author,
                        fontSize = 12.sp,
                        color = WarmMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Badges row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Status pill
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier
                            .background(
                                statusColor.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )

                    // Type + price
                    Text(
                        text = if (listing.type == "DONATE") "FREE"
                        else listing.price?.let { "₹${it.toInt()}" } ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Teal500,
                    )

                    // Condition
                    Text(
                        text = listing.condition,
                        fontSize = 11.sp,
                        color = WarmMuted,
                    )
                }

                // Location
                val locationParts = listOfNotNull(
                    listing.locality?.takeIf { it.isNotBlank() },
                    listing.city?.takeIf { it.isNotBlank() },
                ).joinToString(", ")

                if (locationParts.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 $locationParts",
                        fontSize = 11.sp,
                        color = WarmMuted,
                    )
                }
            }
        }

        // Action buttons
        if (listing.status != "COMPLETED" && listing.status != "RESERVED") {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (listing.status == "ACTIVE") {
                    OutlinedButton(
                        onClick = onPause,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder),
                    ) {
                        Text("Pause", fontSize = 12.sp, color = WarmMuted)
                    }
                    OutlinedButton(
                        onClick = onMarkSold,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder),
                    ) {
                        Text("Mark sold", fontSize = 12.sp, color = WarmMuted)
                    }
                }

                if (listing.status == "PAUSED") {
                    Button(
                        onClick = onReactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("Reactivate", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationRequestRow(request: DonationRequest) {
    val statusColor = when (request.status) {
        "ACCEPTED" -> Teal500
        "REJECTED" -> MaterialTheme.colorScheme.error
        else -> WarmMuted
    }

    val statusLabel = when (request.status) {
        "ACCEPTED" -> "✓ Accepted"
        "REJECTED" -> "✗ Rejected"
        else -> "Pending"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = statusLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
            if (!request.examTag.isNullOrBlank()) {
                Text(
                    text = request.examTag,
                    fontSize = 11.sp,
                    color = Teal900,
                    modifier = Modifier
                        .background(Teal50, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = request.reason,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyTabState(
    emoji: String,
    message: String,
    subtitle: String,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = WarmMuted,
            )
        }
    }
}