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

    // controls the defaults sheet shown over the tray
    val showSessionDefaultsSheet: Boolean = false,
    val sessionDefaultsRequiredBanner: Boolean = false, // shown on tray after first dismissal

    val isInitializing: Boolean = true, // true until init coroutine completes

    // Books and bundles (live from Room)
    val stagedBooks: List<StagedBook> = emptyList(),
    val stagedBundles: List<StagedBundle> = emptyList(),

    // Tray selection state (for inline bundle creation)
    val selectedBookIds: Set<String> = emptySet(),

    // Sheet states
    val addBookSheet: AddBookSheetState = AddBookSheetState(),
    val createBundleSheet: CreateBundleSheetState = CreateBundleSheetState(),

    // Publish
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val failedListingTitles: List<String> = emptyList(),

    // Resume banner
    val showResumeBanner: Boolean = false,
) {
    val unbundledBooks: List<StagedBook>
        get() = stagedBooks.filter { it.bundleId == null }

    fun booksForBundle(bundleId: String): List<StagedBook> =
        stagedBooks.filter { it.bundleId == bundleId }

    val totalListingCount: Int
        get() = unbundledBooks.size + stagedBundles.size

    val totalBookCount: Int
        get() = stagedBooks.size

    val allListingsReady: Boolean
        get() {
            val default = sessionDefaults.listingType
            val individualOk = unbundledBooks.all { it.isReadyToPublish(default) }
            val bundlesOk = stagedBundles.all { bundle ->
                bundle.isReadyToPublish(default) && booksForBundle(bundle.id).isNotEmpty()
            }
            return stagedBooks.isNotEmpty() && individualOk && bundlesOk
        }

    val canCreateBundleFromSelection: Boolean
        get() = selectedBookIds.size >= 2
}