package com.kitaab.app.feature.post.multi

sealed interface MultiPostEvent {
    // Navigate forward through the session flow
    data object NavigateToTray : MultiPostEvent
    data object NavigateToOrganise : MultiPostEvent
    data object NavigateToReview : MultiPostEvent

    // Session complete — navigate back to home and show success snackbar
    data class PublishComplete(
        val successCount: Int,
        val totalBookCount: Int,
    ) : MultiPostEvent

    // Partial publish — some listings failed; ViewModel retains failed state for retry
    data class PublishPartialFailure(
        val successCount: Int,
        val failedTitles: List<String>,
    ) : MultiPostEvent

    // User abandoned session from tray (back press / discard)
    data object SessionAbandoned : MultiPostEvent
}