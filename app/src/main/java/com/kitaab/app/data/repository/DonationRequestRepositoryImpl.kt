package com.kitaab.app.data.repository

import com.kitaab.app.domain.model.DonationRequest
import com.kitaab.app.domain.model.DonationRequestWithRequester
import com.kitaab.app.domain.repository.DonationRequestRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class UserRow(
    val id: String,
    val name: String,
    val city: String? = null,
    val badge: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class TransactionRow(
    @SerialName("completed_at") val completedAt: String? = null,
)

class DonationRequestRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : DonationRequestRepository {

    private val currentUserId get() = supabase.auth.currentUserOrNull()?.id ?: ""

    override suspend fun submitRequest(
        listingId: String,
        reason: String,
        examTag: String?,
    ): Result<Unit> = runCatching {
        // Rate limit: max 3 active requests
        val activeCount = supabase.postgrest["donation_requests"]
            .select {
                filter {
                    eq("requester_id", currentUserId)
                    eq("status", "PENDING")
                }
            }
            .decodeList<DonationRequest>()
            .size

        if (activeCount >= 3) {
            error("You already have 3 active donation requests. Wait for a response before requesting more.")
        }

        // Cooldown: 21 days after receiving a donated book
        val lastReceivedDate = getLastReceivedDonationDate(currentUserId).getOrNull()
        if (lastReceivedDate != null) {
            val sdf = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                java.util.Locale.getDefault(),
            )
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val lastDate = sdf.parse(
                lastReceivedDate.substringBefore(".").substringBefore("+")
            )
            if (lastDate != null) {
                val diffDays = (java.util.Date().time - lastDate.time) / (1000 * 60 * 60 * 24)
                if (diffDays < 21) {
                    val daysLeft = 21 - diffDays
                    error("You received a donated book recently. You can request again in $daysLeft days.")
                }
            }
        }

        // Account age gate: 7 days
        val userRow = supabase.postgrest["users"]
            .select { filter { eq("id", currentUserId) } }
            .decodeList<UserRow>()
            .firstOrNull() ?: error("User not found")

        if (userRow.createdAt != null) {
            val sdf = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                java.util.Locale.getDefault(),
            )
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val createdDate = sdf.parse(
                userRow.createdAt.substringBefore(".").substringBefore("+")
            )
            if (createdDate != null) {
                val ageDays = (java.util.Date().time - createdDate.time) / (1000 * 60 * 60 * 24)
                if (ageDays < 7) {
                    val daysLeft = 7 - ageDays
                    error("Your account must be at least 7 days old to request donations. $daysLeft more days to go.")
                }
            }
        }

        supabase.postgrest["donation_requests"].insert(
            mapOf(
                "listing_id" to listingId,
                "requester_id" to currentUserId,
                "reason" to reason,
                "exam_tag" to examTag,
            )
        )
        Unit
    }

    override suspend fun getRequestsForListing(
        listingId: String,
    ): Result<List<DonationRequestWithRequester>> = runCatching {
        val requests = supabase.postgrest["donation_requests"]
            .select {
                filter { eq("listing_id", listingId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<DonationRequest>()

        requests.map { request ->
            val user = runCatching {
                supabase.postgrest["users"]
                    .select { filter { eq("id", request.requesterId) } }
                    .decodeList<UserRow>()
                    .firstOrNull()
            }.getOrNull()

            DonationRequestWithRequester(
                request = request,
                requesterName = user?.name ?: "User",
                requesterCity = user?.city,
                requesterBadge = user?.badge,
                requesterCreatedAt = user?.createdAt,
            )
        }
    }

    override suspend fun acceptRequest(
        requestId: String,
        listingId: String,
    ): Result<Unit> = runCatching {
        // Accept this one
        supabase.postgrest["donation_requests"]
            .update(mapOf("status" to "ACCEPTED")) {
                filter { eq("id", requestId) }
            }

        // Reject all others for this listing
        supabase.postgrest["donation_requests"]
            .update(mapOf("status" to "REJECTED")) {
                filter {
                    eq("listing_id", listingId)
                    eq("status", "PENDING")
                    neq("id", requestId)
                }
            }

        // Mark listing as RESERVED
        supabase.postgrest["listings"]
            .update(mapOf("status" to "RESERVED")) {
                filter { eq("id", listingId) }
            }

        Unit
    }

    override suspend fun getActiveRequestCount(userId: String): Result<Int> = runCatching {
        supabase.postgrest["donation_requests"]
            .select {
                filter {
                    eq("requester_id", userId)
                    eq("status", "PENDING")
                }
            }
            .decodeList<DonationRequest>()
            .size
    }

    override suspend fun getLastReceivedDonationDate(userId: String): Result<String?> =
        runCatching {
            supabase.postgrest["transactions"]
                .select {
                    filter {
                        eq("buyer_id", userId)
                        eq("type", "DONATE")
                    }
                    order(
                        "completed_at",
                        io.github.jan.supabase.postgrest.query.Order.DESCENDING,
                    )
                    limit(1)
                }
                .decodeList<TransactionRow>()
                .firstOrNull()
                ?.completedAt
        }
}