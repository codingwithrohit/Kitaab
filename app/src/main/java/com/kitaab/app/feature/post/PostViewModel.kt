package com.kitaab.app.feature.post

import android.annotation.SuppressLint
import android.location.Geocoder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

enum class ListingType { SELL, DONATE }

enum class PostStep { CHOOSE_TYPE, BOOK_DETAILS, CONDITION, PHOTOS, PRICE_LOCATION }

enum class BookCondition(val label: String, val description: String) {
    New("New", "Unused, no markings"),
    LikeNew("Like New", "Minimal use, no markings"),
    Good("Good", "Some highlighting or notes"),
    Fair("Fair", "Heavy use, intact pages"),
    Poor("Poor", "Worn but readable"),
}

data class PostUiState(
    val currentStep: PostStep = PostStep.CHOOSE_TYPE,
    val listingType: ListingType? = null,
    // Step 2 — Book details
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val edition: String = "",
    val isbn: String = "",
    val subject: String = "",
    val examTags: Set<String> = emptySet(),
    val hasSolutions: Boolean = false,
    val hasNotes: Boolean = false,
    val isFetchingBookDetails: Boolean = false,
    val bookNotFound: Boolean = false,
    val titleError: String? = null,
    // Step 3 — Condition
    val condition: BookCondition? = null,
    val conditionError: String? = null,
    // Step 4 — Photos
    val photoUris: List<Uri> = emptyList(),
    val isUploadingPhotos: Boolean = false,
    // Step 5 — Price + location
    val price: String = "",
    val city: String = "",
    val pincode: String = "",
    val locality: String = "",
    val priceError: String? = null,
    val cityError: String? = null,
    val pincodeError: String? = null,
    val isFetchingLocation: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

sealed interface PostEvent {
    data object PostSuccess : PostEvent
}

@Serializable
private data class NewListing(
    val seller_id: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val edition: String?,
    val isbn: String?,
    val subject: String?,
    val exam_tags: List<String>,
    val condition: String,
    val type: String,
    val price: Double?,
    val has_solutions: Boolean,
    val has_notes: Boolean,
    val photo_urls: List<String>,
    val status: String,
    val city: String?,
    val pincode: String?,
    val locality: String?,
)

@HiltViewModel
class PostViewModel
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val userPrefs: UserPreferencesRepository,
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PostUiState())
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<PostEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            prefillLocation()
        }

        private fun prefillLocation() {
            viewModelScope.launch {
                val city = userPrefs.city.first()
                val pincode = userPrefs.pincode.first()
                val locality = userPrefs.locality.first()
                _uiState.update {
                    it.copy(city = city, pincode = pincode, locality = locality)
                }
            }
        }

        // ── Step navigation ───────────────────────────────────────────────────────

        fun goToNextStep() {
            val next =
                when (_uiState.value.currentStep) {
                    PostStep.CHOOSE_TYPE -> PostStep.BOOK_DETAILS
                    PostStep.BOOK_DETAILS -> PostStep.CONDITION
                    PostStep.CONDITION -> PostStep.PHOTOS
                    PostStep.PHOTOS -> PostStep.PRICE_LOCATION
                    PostStep.PRICE_LOCATION -> return
                }
            _uiState.update { it.copy(currentStep = next) }
        }

        fun goToPreviousStep() {
            val prev =
                when (_uiState.value.currentStep) {
                    PostStep.CHOOSE_TYPE -> return
                    PostStep.BOOK_DETAILS -> PostStep.CHOOSE_TYPE
                    PostStep.CONDITION -> PostStep.BOOK_DETAILS
                    PostStep.PHOTOS -> PostStep.CONDITION
                    PostStep.PRICE_LOCATION -> PostStep.PHOTOS
                }
            _uiState.update { it.copy(currentStep = prev) }
        }

        val isOnFirstStep get() = _uiState.value.currentStep == PostStep.CHOOSE_TYPE

        // ── Step 1 ────────────────────────────────────────────────────────────────

        fun onListingTypeSelected(type: ListingType) {
            _uiState.update { it.copy(listingType = type) }
            goToNextStep()
        }

        // ── Step 2 — Book details ─────────────────────────────────────────────────

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value, titleError = null) }
        }

        fun onAuthorChanged(value: String) {
            _uiState.update { it.copy(author = value) }
        }

        fun onPublisherChanged(value: String) {
            _uiState.update { it.copy(publisher = value) }
        }

        fun onEditionChanged(value: String) {
            _uiState.update { it.copy(edition = value) }
        }

        fun onSubjectChanged(value: String) {
            _uiState.update { it.copy(subject = value) }
        }

        fun onIsbnScanned(isbn: String) {
            _uiState.update { it.copy(isbn = isbn, bookNotFound = false) }
            fetchBookDetails(isbn)
        }

        fun onExamTagToggled(tag: String) {
            _uiState.update { state ->
                val updated = state.examTags.toMutableSet()
                if (tag in updated) updated.remove(tag) else updated.add(tag)
                state.copy(examTags = updated)
            }
        }

        fun onHasSolutionsToggled() {
            _uiState.update { it.copy(hasSolutions = !it.hasSolutions) }
        }

        fun onHasNotesToggled() {
            _uiState.update { it.copy(hasNotes = !it.hasNotes) }
        }

        fun fetchBookDetails(isbn: String) {
            if (isbn.isBlank()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isFetchingBookDetails = true, bookNotFound = false) }
                runCatching {
                    val client = HttpClient(OkHttp)
                    val response =
                        client.get(
                            "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn",
                        )
                    client.close()
                    response.bodyAsText()
                }.fold(
                    onSuccess = { body ->
                        val found = parseGoogleBooksResponse(body)
                        _uiState.update {
                            it.copy(isFetchingBookDetails = false, bookNotFound = !found)
                        }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(isFetchingBookDetails = false, bookNotFound = false)
                        }
                    },
                )
            }
        }

        // Returns true if book was found, false if not
        private fun parseGoogleBooksResponse(json: String): Boolean {
            return try {
                val parsed =
                    kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                    }.decodeFromString<GoogleBooksResponse>(json)

                val info = parsed.items?.firstOrNull()?.volumeInfo ?: return false

                _uiState.update { state ->
                    state.copy(
                        title = info.title?.takeIf { it.isNotBlank() } ?: state.title,
                        author =
                            info.authors?.joinToString(", ")?.takeIf { it.isNotBlank() }
                                ?: state.author,
                        publisher = info.publisher?.takeIf { it.isNotBlank() } ?: state.publisher,
                        subject =
                            info.categories?.firstOrNull()?.takeIf { it.isNotBlank() }
                                ?: state.subject,
                    )
                }
                true
            } catch (_: Exception) {
                false
            }
        }

        fun validateBookDetails(): Boolean {
            if (_uiState.value.title.isBlank()) {
                _uiState.update { it.copy(titleError = "Title is required") }
                return false
            }
            return true
        }

        // ── Step 3 — Condition ────────────────────────────────────────────────────

        fun onConditionSelected(condition: BookCondition) {
            _uiState.update { it.copy(condition = condition, conditionError = null) }
        }

        fun validateCondition(): Boolean {
            if (_uiState.value.condition == null) {
                _uiState.update { it.copy(conditionError = "Please select a condition") }
                return false
            }
            return true
        }

        // ── Step 4 — Photos ───────────────────────────────────────────────────────

        fun onPhotosSelected(uris: List<Uri>) {
            _uiState.update { state ->
                val combined = (state.photoUris + uris).take(5)
                state.copy(photoUris = combined)
            }
        }

        fun onPhotoRemoved(uri: Uri) {
            _uiState.update { it.copy(photoUris = it.photoUris - uri) }
        }

        // ── Step 5 — Price + location ─────────────────────────────────────────────

        fun onPriceChanged(value: String) {
            if (value.isEmpty() || value.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?\$"))) {
                _uiState.update { it.copy(price = value, priceError = null) }
            }
        }

        fun onCityChanged(value: String) {
            _uiState.update { it.copy(city = value, cityError = null) }
        }

        fun onPincodeChanged(value: String) {
            if (value.length <= 6 && value.all { it.isDigit() }) {
                _uiState.update { it.copy(pincode = value, pincodeError = null) }
            }
        }

        fun onLocalityChanged(value: String) {
            _uiState.update { it.copy(locality = value) }
        }

        @SuppressLint("MissingPermission")
        fun fetchCurrentLocation() {
            viewModelScope.launch {
                _uiState.update { it.copy(isFetchingLocation = true) }
                try {
                    val locationManager =
                        context.getSystemService(
                            android.content.Context.LOCATION_SERVICE,
                        ) as android.location.LocationManager

                    val location =
                        suspendCancellableCoroutine { cont ->
                            val listener =
                                object : android.location.LocationListener {
                                    override fun onLocationChanged(location: android.location.Location) {
                                        locationManager.removeUpdates(this)
                                        cont.resume(location)
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun onStatusChanged(
                                        provider: String?,
                                        status: Int,
                                        extras: android.os.Bundle?,
                                    ) {
                                    }
                                }
                            val provider =
                                when {
                                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ->
                                        android.location.LocationManager.GPS_PROVIDER

                                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ->
                                        android.location.LocationManager.NETWORK_PROVIDER

                                    else -> {
                                        cont.resume(null)
                                        return@suspendCancellableCoroutine
                                    }
                                }
                            val lastKnown = locationManager.getLastKnownLocation(provider)
                            if (lastKnown != null) {
                                cont.resume(lastKnown)
                            } else {
                                locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
                                cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
                            }
                        }

                    if (location != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()
                        _uiState.update {
                            it.copy(
                                city = address?.locality ?: address?.subAdminArea ?: it.city,
                                pincode = address?.postalCode ?: it.pincode,
                                locality = address?.subLocality ?: address?.thoroughfare ?: it.locality,
                                isFetchingLocation = false,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isFetchingLocation = false) }
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isFetchingLocation = false) }
                }
            }
        }

        fun validatePriceLocation(): Boolean {
            var valid = true
            val state = _uiState.value

            if (state.listingType == ListingType.SELL) {
                if (state.price.isBlank()) {
                    _uiState.update { it.copy(priceError = "Price is required for sell listings") }
                    valid = false
                } else if (state.price.toDoubleOrNull() == null || state.price.toDouble() <= 0) {
                    _uiState.update { it.copy(priceError = "Enter a valid price") }
                    valid = false
                }
            }

            if (state.city.isBlank()) {
                _uiState.update { it.copy(cityError = "City is required") }
                valid = false
            }

            if (state.pincode.isBlank()) {
                _uiState.update { it.copy(pincodeError = "Pincode is required") }
                valid = false
            } else if (state.pincode.length != 6) {
                _uiState.update { it.copy(pincodeError = "Enter a valid 6-digit pincode") }
                valid = false
            }

            return valid
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        // ── Submit ────────────────────────────────────────────────────────────────

        fun submitListing() {
            if (!validatePriceLocation()) return

            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true, error = null) }

                val userId = supabase.auth.currentSessionOrNull()?.user?.id
                if (userId == null) {
                    _uiState.update {
                        it.copy(isSubmitting = false, error = "Session expired. Please sign in again.")
                    }
                    return@launch
                }

                runCatching {
                    val state = _uiState.value
                    val uploadedUrls = uploadPhotos(userId, state.photoUris)

                    supabase.postgrest["listings"].insert(
                        NewListing(
                            seller_id = userId,
                            title = state.title.trim(),
                            author = state.author.trim().ifBlank { null },
                            publisher = state.publisher.trim().ifBlank { null },
                            edition = state.edition.trim().ifBlank { null },
                            isbn = state.isbn.trim().ifBlank { null },
                            subject = state.subject.trim().ifBlank { null },
                            exam_tags = state.examTags.toList(),
                            condition = state.condition!!.name,
                            type = state.listingType!!.name,
                            price =
                                if (state.listingType == ListingType.SELL) {
                                    state.price.toDoubleOrNull()
                                } else {
                                    null
                                },
                            has_solutions = state.hasSolutions,
                            has_notes = state.hasNotes,
                            photo_urls = uploadedUrls,
                            status = "ACTIVE",
                            city = state.city.trim().ifBlank { null },
                            pincode = state.pincode.trim().ifBlank { null },
                            locality = state.locality.trim().ifBlank { null },
                        ),
                    )
                }.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _events.send(PostEvent.PostSuccess)
                    },
                    onFailure = { cause ->
                        val message =
                            when (cause) {
                                is RestException -> "Failed to post listing. Please try again."
                                is HttpRequestException -> "No internet connection. Please try again."
                                else -> cause.message ?: "Something went wrong. Please try again."
                            }
                        _uiState.update { it.copy(isSubmitting = false, error = message) }
                    },
                )
            }
        }

        private suspend fun uploadPhotos(
            userId: String,
            uris: List<Uri>,
        ): List<String> {
            if (uris.isEmpty()) return emptyList()
            val urls = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                runCatching {
                    val listingId = java.util.UUID.randomUUID().toString()
                    val path = "$userId/$listingId/photo_$index.jpg"
                    supabase.storage.from("book-photos").upload(path, readUriBytes(uri)) {
                        upsert = true
                    }
                    val url = supabase.storage.from("book-photos").publicUrl(path)
                    urls.add(url)
                }
            }
            return urls
        }

        fun readUriBytes(uri: Uri): ByteArray = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    }

@Serializable
private data class GoogleBooksResponse(
    val items: List<GoogleBooksItem>? = null,
)

@Serializable
private data class GoogleBooksItem(
    val volumeInfo: VolumeInfo? = null,
)

@Serializable
private data class VolumeInfo(
    val title: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val categories: List<String>? = null,
)
