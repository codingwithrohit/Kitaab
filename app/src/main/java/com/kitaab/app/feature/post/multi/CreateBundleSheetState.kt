package com.kitaab.app.feature.post.multi

import com.kitaab.app.feature.post.ListingType

data class CreateBundleSheetState(
    val isVisible: Boolean = false,
    val editingBundleId: String? = null,
    val name: String = "",
    val bundlePrice: String = "",
    val typeOverride: ListingType? = null,
    val nameError: String? = null,
    val priceError: String? = null,
) {
    val isEditing: Boolean get() = editingBundleId != null
}