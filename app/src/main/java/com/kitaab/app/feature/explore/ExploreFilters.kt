package com.kitaab.app.feature.explore

data class ExploreFilters(
    val type: String? = null,
    val condition: String? = null,
    val examTag: String? = null,
    val minPrice: String = "",
    val maxPrice: String = "",
) {
    val isActive: Boolean get() = type != null || condition != null ||
            examTag != null || minPrice.isNotBlank() || maxPrice.isNotBlank()
}