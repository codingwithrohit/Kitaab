package com.kitaab.app.domain.repository

import com.kitaab.app.domain.model.DonationRequestWithRequester

interface DonationRequestRepository {
    suspend fun submitRequest(
        listingId: String,
        reason: String,
        examTag: String?,
    ): Result<Unit>

    suspend fun getRequestsForListing(listingId: String): Result<List<DonationRequestWithRequester>>

    suspend fun acceptRequest(
        requestId: String,
        listingId: String,
    ): Result<Unit>

    suspend fun getActiveRequestCount(userId: String): Result<Int>

    suspend fun getLastReceivedDonationDate(userId: String): Result<String?>
}
