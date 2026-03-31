package com.kitaab.app.feature.post.multi

import com.kitaab.app.domain.model.SessionDefaults
import com.kitaab.app.domain.model.StagedBook
import com.kitaab.app.domain.model.StagedBundle

data class MultiPostUiState(
    val sessionId: String? = null,
    val currentScreen: MultiPostScreen = MultiPostScreen.SESSION_DEFAULTS,

    // Session defaults
    val sessionDefaults: SessionDefaults = SessionDefaults(),
    val cityError: String? = null,
    val pincodeError: String? = null,
    val isFetchingLocation: Boolean = false,

    // Books and bundles (live from Room)
    val stagedBooks: List<StagedBook> = emptyList(),
    val stagedBundles: List<StagedBundle> = emptyList(),

    // Organise screen selection state
    val selectedBookIds: Set<String> = emptySet(),

    // Sheet states
    val addBookSheet: AddBookSheetState = AddBookSheetState(),
    val createBundleSheet: CreateBundleSheetState = CreateBundleSheetState(),

    // Publish
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    // Books whose listings failed to publish — shown on error screen for retry
    val failedListingTitles: List<String> = emptyList(),

    // Resume banner — shown on Post tap if an incomplete session exists
    val showResumeBanner: Boolean = false,
) {
    // Convenience groupings used by OrganiseScreen and ReviewPublishScreen

    val unbundledBooks: List<StagedBook>
        get() = stagedBooks.filter { it.bundleId == null }

    fun booksForBundle(bundleId: String): List<StagedBook> =
        stagedBooks.filter { it.bundleId == bundleId }

    // Total listing count = individual books + bundles
    val totalListingCount: Int
        get() = unbundledBooks.size + stagedBundles.size

    val totalBookCount: Int
        get() = stagedBooks.size

    // Review screen readiness — passes session default so DONATE books don't require price
    val allListingsReady: Boolean
        get() {
            val default = sessionDefaults.listingType
            val individualOk = unbundledBooks.all { it.isReadyToPublish(default) }
            val bundlesOk = stagedBundles.all { bundle ->
                bundle.isReadyToPublish(default) && booksForBundle(bundle.id).isNotEmpty()
            }
            return stagedBooks.isNotEmpty() && individualOk && bundlesOk
        }

    // Organise screen: whether the selected books can form a bundle
    // A bundle needs at least 2 books and all selected books must be from the same session
    val canCreateBundleFromSelection: Boolean
        get() = selectedBookIds.size >= 2
}