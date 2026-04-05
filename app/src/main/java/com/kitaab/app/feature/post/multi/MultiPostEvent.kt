package com.kitaab.app.feature.post.multi

sealed interface MultiPostEvent {
    data object NavigateToTray : MultiPostEvent

    data object NavigateToReview : MultiPostEvent

    data class PublishComplete(
        val successCount: Int,
        val totalBookCount: Int,
    ) : MultiPostEvent

    data class PublishPartialFailure(
        val successCount: Int,
        val failedTitles: List<String>,
    ) : MultiPostEvent

    data object SessionAbandoned : MultiPostEvent
}
