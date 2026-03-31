package com.kitaab.app.feature.listing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile
import com.kitaab.app.feature.donation.DonationRequestBottomSheet
import com.kitaab.app.feature.donation.DonationRequestsSellerSection
import com.kitaab.app.feature.home.components.BookCoverImage
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal900
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListingDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (conversationId: String) -> Unit,
    onNavigateToDonationRequest: (listingId: String) -> Unit,
    onNavigateToDonationRequests: (listingId: String) -> Unit,
    onNavigateToEditListing: (listingId: String) -> Unit,
    onSellerClick: (userId: String) -> Unit,
    onSimilarListingClick: (listingId: String) -> Unit,
    viewModel: ListingDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDonationSheet by rememberSaveable { mutableStateOf(false) }
    val donationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ListingDetailEvent.NavigateToChat ->
                    onNavigateToChat(event.conversationId)

                is ListingDetailEvent.NavigateToDonationRequest ->
                    showDonationSheet = true

                is ListingDetailEvent.NavigateToDonationRequests ->
                    onNavigateToDonationRequests(event.listingId)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
        bottomBar = {
            if (state.listing != null && !state.isOwnListing) {
                ActionBar(
                    listing = state.listing!!,
                    onMessageSeller = { viewModel.onMessageSellerClick() },
                    onRequestDonation = { viewModel.onRequestDonationClick() },
                )
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    CircularProgressIndicator(color = Teal500)
                }
            }

            state.listing == null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Listing not found", fontSize = 16.sp, color = WarmMuted)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.load() }) {
                            Text("Try again")
                        }
                    }
                }
            }

            else -> {
                val listing = state.listing!!
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                ) {
                    // ── Photo carousel ────────────────────────────────────────
                    PhotoCarousel(photoUrls = listing.photoUrls)

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Type + condition badges ────────────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeBadge(type = listing.type, price = listing.price)
                            ConditionChip(condition = listing.condition)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Title + author ────────────────────────────────────
                        Text(
                            text = listing.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 26.sp,
                        )

                        if (!listing.author.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listing.author,
                                fontSize = 14.sp,
                                color = WarmMuted,
                            )
                        }

                        if (!listing.publisher.isNullOrBlank() || !listing.edition.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            val pubInfo =
                                listOfNotNull(
                                    listing.publisher,
                                    listing.edition?.let { "Edition: $it" },
                                ).joinToString(" · ")
                            Text(text = pubInfo, fontSize = 13.sp, color = WarmMuted)
                        }

                        // ── Exam tags ─────────────────────────────────────────
                        if (listing.examTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listing.examTags.forEach { tag ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {},
                                        label = { Text(tag, fontSize = 12.sp) },
                                        colors =
                                            FilterChipDefaults.filterChipColors(
                                                containerColor = Teal50,
                                                labelColor = Teal900,
                                            ),
                                        shape = RoundedCornerShape(6.dp),
                                    )
                                }
                            }
                        }

                        // ── Extras ────────────────────────────────────────────
                        val extras =
                            buildList {
                                if (listing.hasSolutions) add("Has solutions manual")
                                if (listing.hasNotes) add("Has handwritten notes")
                            }
                        if (extras.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            extras.forEach { extra ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(6.dp)
                                                .background(Teal500, CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = extra, fontSize = 13.sp, color = WarmMuted)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // ── Location ──────────────────────────────────────────
                        val locationParts =
                            listOfNotNull(
                                listing.locality?.takeIf { it.isNotBlank() },
                                listing.city?.takeIf { it.isNotBlank() },
                                listing.pincode?.takeIf { it.isNotBlank() },
                            ).joinToString(", ")

                        if (locationParts.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "📍 $locationParts",
                                fontSize = 13.sp,
                                color = WarmMuted,
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = WarmBorder)
                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Seller row ────────────────────────────────────────
                        state.seller?.let { seller ->
                            SellerRow(
                                seller = seller,
                                onClick = { onSellerClick(seller.id) },
                            )
                        }

                        // ── Own listing notice ────────────────────────────────
                        if (state.isOwnListing) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = Teal50,
                                    ),
                            ) {
                                Text(
                                    text = "This is your listing",
                                    fontSize = 13.sp,
                                    color = Teal900,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                        if (state.isOwnListing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onNavigateToEditListing(listing.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Edit listing")
                            }
                        }

                        if (state.isOwnListing && state.listing?.type == "DONATE") {
                            Spacer(modifier = Modifier.height(20.dp))

                            HorizontalDivider(color = WarmBorder)

                            Spacer(modifier = Modifier.height(20.dp))

                            DonationRequestsSellerSection(
                                listingId = state.listing!!.id,
                                onAcceptSuccess = { viewModel.load() },
                                onSeeAllRequests = {
                                    onNavigateToDonationRequests(state.listing!!.id)
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── Similar listings ──────────────────────────────────
                        if (state.similarListings.isNotEmpty()) {
                            HorizontalDivider(color = WarmBorder)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "More nearby",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // ── Similar listings horizontal scroll ────────────────────
                    if (state.similarListings.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = state.similarListings,
                                key = { it.id },
                            ) { similar ->
                                SimilarListingCard(
                                    listing = similar,
                                    onClick = { onSimilarListingClick(similar.id) },
                                )
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .height(32.dp)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
    if (showDonationSheet) {
        DonationRequestBottomSheet(
            listingId = viewModel.uiState.collectAsState().value.listing?.id ?: "",
            sheetState = donationSheetState,
            onDismiss = { showDonationSheet = false },
            onSuccess = {
                showDonationSheet = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Request submitted! The seller will review it.")
                }
            },
        )
    }
}

@Composable
private fun PhotoCarousel(photoUrls: List<String>) {
    if (photoUrls.isEmpty()) {
        // Full-width default cover
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(Teal50),
            contentAlignment = Alignment.Center,
        ) {
            BookCoverImage(
                url = null,
                modifier = Modifier.size(80.dp),
            )
        }
        return
    }

    val pagerState = rememberPagerState { photoUrls.size }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            AsyncImage(
                model = photoUrls[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Page indicator dots
        if (photoUrls.size > 1) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                photoUrls.indices.forEach { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                .background(
                                    if (index == pagerState.currentPage) {
                                        Color.White
                                    } else {
                                        Color.White.copy(alpha = 0.5f)
                                    },
                                    CircleShape,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(
    type: String,
    price: Double?,
) {
    val (text, bgColor, textColor) =
        when (type) {
            "DONATE" -> Triple("FREE", Teal500.copy(alpha = 0.15f), Teal500)
            else ->
                Triple(
                    if (price != null) "₹${price.toInt()}" else "SELL",
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
        }
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ConditionChip(condition: String) {
    val color =
        when (condition) {
            "New", "LikeNew" -> Teal500
            "Good" -> MaterialTheme.colorScheme.primary
            else -> WarmMuted
        }
    Card(
        shape = RoundedCornerShape(6.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f),
            ),
    ) {
        Text(
            text = condition,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SellerRow(
    seller: UserProfile,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!seller.profilePhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = seller.profilePhotoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape),
            )
        } else {
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = WarmMuted,
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = seller.name.ifBlank { "Kitaab user" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (seller.reviewCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = WarmMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${seller.avgRating} · ${seller.reviewCount} reviews",
                        fontSize = 12.sp,
                        color = WarmMuted,
                    )
                }
            }
            if (!seller.city.isNullOrBlank()) {
                Text(
                    text = seller.city,
                    fontSize = 12.sp,
                    color = WarmMuted,
                )
            }
        }

        Text(
            text = "View profile",
            fontSize = 13.sp,
            color = Teal500,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ActionBar(
    listing: Listing,
    onMessageSeller: () -> Unit,
    onRequestDonation: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        HorizontalDivider(color = WarmBorder)
        Spacer(modifier = Modifier.height(12.dp))

        if (listing.type == "DONATE") {
            Button(
                onClick = onRequestDonation,
                colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
            ) {
                Text(
                    text = "Request This Book",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            Button(
                onClick = onMessageSeller,
                colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
            ) {
                Text(
                    text = "Message Seller",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SimilarListingCard(
    listing: Listing,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .width(140.dp)
                .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, WarmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            BookCoverImage(
                url = listing.photoUrls.firstOrNull(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(90.dp),
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = listing.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        if (listing.type == "DONATE") {
                            "FREE"
                        } else {
                            listing.price?.let { "₹${it.toInt()}" } ?: "SELL"
                        },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Teal500,
                )
            }
        }
    }
}
