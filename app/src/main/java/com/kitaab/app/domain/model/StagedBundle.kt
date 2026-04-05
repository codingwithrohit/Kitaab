package com.kitaab.app.domain.model

import com.kitaab.app.feature.post.ListingType

data class StagedBundle(
    val id: String,
    val sessionId: String,
    val name: String = "",
    val bundlePrice: String = "",
    val typeOverride: ListingType? = null,
) {
    fun effectiveType(sessionDefault: ListingType): ListingType = typeOverride ?: sessionDefault

    fun isReadyToPublish(sessionDefault: ListingType): Boolean {
        val type = effectiveType(sessionDefault)
        val priceOk = type == ListingType.DONATE || bundlePrice.isNotBlank()
        return name.isNotBlank() && priceOk
    }

    fun warningText(sessionDefault: ListingType): String? {
        val type = effectiveType(sessionDefault)
        return when {
            name.isBlank() -> "Bundle name required"
            type == ListingType.SELL && bundlePrice.isBlank() -> "Price required"
            else -> null
        }
    }
}
