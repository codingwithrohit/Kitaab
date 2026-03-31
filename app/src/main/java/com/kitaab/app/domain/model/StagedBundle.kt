package com.kitaab.app.domain.model

import com.kitaab.app.feature.post.ListingType

data class StagedBundle(
    val id: String,
    val sessionId: String,
    val name: String = "",
    val bundlePrice: String = "",                    // empty = not yet set
    val typeOverride: ListingType? = null,
) {
    val isReadyToPublish: Boolean
        get() = name.isNotBlank() && bundlePrice.isNotBlank()
}