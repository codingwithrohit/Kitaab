package com.kitaab.app.feature.donation

data class DonationRequestUiState(
    val reason: String = "",
    val selectedExamTag: String? = null,
    val reasonError: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)
