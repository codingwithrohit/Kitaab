package com.kitaab.app.domain.model

import com.kitaab.app.feature.post.ListingType

data class SessionDefaults(
    val listingType: ListingType = ListingType.SELL,
    val city: String = "",
    val pincode: String = "",
    val locality: String = "",
)
