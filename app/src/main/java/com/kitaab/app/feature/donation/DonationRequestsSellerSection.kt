package com.kitaab.app.feature.donation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kitaab.app.domain.model.DonationRequestWithRequester
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal900
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@Composable
fun DonationRequestsSellerSection(
    listingId: String,
    onAcceptSuccess: () -> Unit,
    onSeeAllRequests: () -> Unit,
    viewModel: DonationRequestsSellerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(listingId) {
        viewModel.loadRequests(listingId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DonationRequestsSellerEvent.AcceptSuccess -> onAcceptSuccess()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Donation Requests",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (state.requests.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .background(Teal500, CircleShape),
                ) {
                    Text(
                        text = state.requests.count { it.request.status == "PENDING" }.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                ) {
                    CircularProgressIndicator(color = Teal500, modifier = Modifier.size(24.dp))
                }
            }

            state.error != null -> {
                Text(
                    text = state.error!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.requests.isEmpty() -> {
                Text(
                    text = "No requests yet",
                    fontSize = 14.sp,
                    color = WarmMuted,
                )
            }

            else -> {
                val preview = state.requests.take(3)
                val remaining = state.requests.size - preview.size
                val pendingCount = state.requests.count { it.request.status == "PENDING" }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    preview.forEach { item ->
                        DonationRequestCard(
                            item = item,
                            isAccepting = state.isAccepting == item.request.id,
                            onAccept = {
                                viewModel.acceptRequest(item.request.id, listingId)
                            },
                        )
                    }

                    if (remaining > 0) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onSeeAllRequests,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.kitaab.app.ui.theme.WarmBorder),
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "See all $pendingCount pending requests",
                                fontSize = 13.sp,
                                color = Teal500,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonationRequestCard(
    item: DonationRequestWithRequester,
    isAccepting: Boolean,
    onAccept: () -> Unit,
) {
    val statusColor = when (item.request.status) {
        "ACCEPTED" -> Teal500
        "REJECTED" -> WarmMuted
        else -> MaterialTheme.colorScheme.onBackground
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.request.status) {
                "ACCEPTED" -> Teal50
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = when (item.request.status) {
                "ACCEPTED" -> Teal500.copy(alpha = 0.4f)
                else -> WarmBorder
            },
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = item.requesterName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (!item.requesterCity.isNullOrBlank()) {
                        Text(
                            text = item.requesterCity,
                            fontSize = 12.sp,
                            color = WarmMuted,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.requesterBadge.isNullOrBlank()) {
                        Text(
                            text = item.requesterBadge,
                            fontSize = 11.sp,
                            color = Teal900,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(Teal50, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = item.request.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            if (!item.request.examTag.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.request.examTag,
                    fontSize = 11.sp,
                    color = Teal900,
                    modifier = Modifier
                        .background(Teal50, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.request.reason,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 19.sp,
            )

            if (item.request.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAccept,
                    enabled = !isAccepting,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            text = "Accept",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}