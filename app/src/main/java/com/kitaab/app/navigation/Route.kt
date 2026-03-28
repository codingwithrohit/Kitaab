package com.kitaab.app.navigation

sealed class Route(val route: String) {
    // Entry flow
    data object Splash : Route("splash")

    data object Onboarding : Route("onboarding")

    data object Login : Route("login")

    data object SignUp : Route("signup")

    data object ProfileSetup : Route("profile_setup")
    data object EditProfile : Route("edit_profile")

    // Main tabs
    data object Home : Route("home")

    data object Explore : Route("explore")

    data object Post : Route("post")

    data object Inbox : Route("inbox")

    data object Profile : Route("profile")

    // Detail screens
    data object ListingDetail : Route("listing_detail/{listingId}?referrer={referrerId}") {
        fun createRoute(listingId: String, referrerId: String? = null) =
            if (referrerId != null) "listing_detail/$listingId?referrer=$referrerId"
            else "listing_detail/$listingId"
    }

    data object SellerProfile : Route("seller_profile/{userId}") {
        fun createRoute(userId: String) = "seller_profile/$userId"
    }

    data object Chat : Route("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }

    data object DonationRequests : Route("donation_requests/{listingId}") {
        fun createRoute(listingId: String) = "donation_requests/$listingId"
    }
}
