package com.kitaab.app.feature.listing

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.feature.post.ListingType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditListingViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle["listingId"])

    private val _uiState = MutableStateFlow(EditListingUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<EditListingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadListing()
    }

    private fun loadListing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                supabase.postgrest["listings"]
                    .select { filter { eq("id", listingId) } }
                    .decodeList<com.kitaab.app.domain.model.Listing>()
                    .firstOrNull() ?: error("Listing not found")
            }.fold(
                onSuccess = { listing ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = listing.title,
                            author = listing.author ?: "",
                            publisher = listing.publisher ?: "",
                            edition = listing.edition ?: "",
                            isbn = listing.isbn ?: "",
                            subject = listing.subject ?: "",
                            examTags = listing.examTags.toSet(),
                            hasSolutions = listing.hasSolutions,
                            hasNotes = listing.hasNotes,
                            condition = runCatching { BookCondition.valueOf(listing.condition) }.getOrNull(),
                            listingType = runCatching { ListingType.valueOf(listing.type) }.getOrElse { ListingType.SELL },
                            price = listing.price?.toInt()?.toString() ?: "",
                            city = listing.city ?: "",
                            pincode = listing.pincode ?: "",
                            locality = listing.locality ?: "",
                            existingPhotoUrls = listing.photoUrls,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun onTitleChanged(v: String) = _uiState.update { it.copy(title = v, titleError = null) }
    fun onAuthorChanged(v: String) = _uiState.update { it.copy(author = v) }
    fun onPublisherChanged(v: String) = _uiState.update { it.copy(publisher = v) }
    fun onEditionChanged(v: String) = _uiState.update { it.copy(edition = v) }
    fun onSubjectChanged(v: String) = _uiState.update { it.copy(subject = v) }
    fun onHasSolutionsToggled() = _uiState.update { it.copy(hasSolutions = !it.hasSolutions) }
    fun onHasNotesToggled() = _uiState.update { it.copy(hasNotes = !it.hasNotes) }
    fun onCityChanged(v: String) = _uiState.update { it.copy(city = v, cityError = null) }
    fun onLocalityChanged(v: String) = _uiState.update { it.copy(locality = v) }

    fun onPincodeChanged(v: String) {
        if (v.length <= 6 && v.all { it.isDigit() }) {
            _uiState.update { it.copy(pincode = v, pincodeError = null) }
        }
    }

    fun onPriceChanged(v: String) {
        if (v.isEmpty() || v.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?\$"))) {
            _uiState.update { it.copy(price = v, priceError = null) }
        }
    }

    fun onListingTypeChanged(type: ListingType) = _uiState.update { it.copy(listingType = type) }

    fun onConditionSelected(condition: BookCondition) =
        _uiState.update { it.copy(condition = condition, conditionError = null) }

    fun onExamTagToggled(tag: String) = _uiState.update { state ->
        val tags = state.examTags.toMutableSet()
        if (tag in tags) tags.remove(tag) else tags.add(tag)
        state.copy(examTags = tags)
    }

    fun onIsbnScanned(isbn: String) {
        _uiState.update { it.copy(isbn = isbn, bookNotFound = false) }
        fetchBookDetails(isbn)
    }

    private fun fetchBookDetails(isbn: String) {
        if (isbn.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingBookDetails = true) }
            runCatching {
                val client = HttpClient(Android)
                val body = client.get("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn")
                    .bodyAsText()
                client.close()
                body
            }.fold(
                onSuccess = { body ->
                    runCatching {
                        val parsed =
                            Json { ignoreUnknownKeys = true }.decodeFromString<GBResponse>(body)
                        val info = parsed.items?.firstOrNull()?.volumeInfo
                        if (info != null) {
                            _uiState.update { state ->
                                state.copy(
                                    isFetchingBookDetails = false,
                                    bookNotFound = false,
                                    title = info.title?.takeIf { it.isNotBlank() } ?: state.title,
                                    author = info.authors?.joinToString(", ")
                                        ?.takeIf { it.isNotBlank() } ?: state.author,
                                    publisher = info.publisher?.takeIf { it.isNotBlank() }
                                        ?: state.publisher,
                                    subject = info.categories?.firstOrNull()
                                        ?.takeIf { it.isNotBlank() } ?: state.subject,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isFetchingBookDetails = false,
                                    bookNotFound = true
                                )
                            }
                        }
                    }.onFailure {
                        _uiState.update {
                            it.copy(
                                isFetchingBookDetails = false,
                                bookNotFound = true
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isFetchingBookDetails = false) }
                },
            )
        }
    }

    // ── Photo management ──────────────────────────────────────────────────────

    fun onNewPhotosSelected(uris: List<Uri>) {
        _uiState.update { state ->
            val remaining = 5 - state.existingPhotoUrls.size
            val toAdd = uris.take(remaining)
            state.copy(newPhotoUris = (state.newPhotoUris + toAdd).take(5))
        }
    }

    fun onExistingPhotoRemoved(url: String) {
        _uiState.update { it.copy(existingPhotoUrls = it.existingPhotoUrls - url) }
    }

    fun onNewPhotoRemoved(uri: Uri) {
        _uiState.update { it.copy(newPhotoUris = it.newPhotoUris - uri) }
    }

    // Move existing URL to front (cover)
    fun onExistingPhotoCover(url: String) {
        _uiState.update { state ->
            val reordered = listOf(url) + (state.existingPhotoUrls - url)
            state.copy(existingPhotoUrls = reordered)
        }
    }

    // Move new URI to front (cover) — also moves before existing photos if any
    fun onNewPhotoCover(uri: Uri) {
        _uiState.update { state ->
            // Promote this uri to very front: make it first existingPhotoUrl slot is tricky,
            // so instead we store it as first newPhotoUri and clear existingPhotoUrls cover
            val reordered = listOf(uri) + (state.newPhotoUris - uri)
            state.copy(newPhotoUris = reordered)
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (userId == null) {
                _uiState.update { it.copy(isSubmitting = false, error = "Session expired.") }
                return@launch
            }

            runCatching {
                val state = _uiState.value

                // Upload any new photos
                val newUrls = uploadNewPhotos(userId, state.newPhotoUris)

                // Final photo list: existing (minus removed) + newly uploaded
                val finalPhotoUrls = state.existingPhotoUrls + newUrls

                supabase.postgrest["listings"].update(
                    {
                        set("title", state.title.trim())
                        set("author", state.author.trim().ifBlank { null })
                        set("publisher", state.publisher.trim().ifBlank { null })
                        set("edition", state.edition.trim().ifBlank { null })
                        set("isbn", state.isbn.trim().ifBlank { null })
                        set("subject", state.subject.trim().ifBlank { null })
                        set("exam_tags", state.examTags.toList())
                        set("condition", state.condition!!.name)
                        set("type", state.listingType.name)
                        set(
                            "price",
                            if (state.listingType == ListingType.SELL) state.price.toDoubleOrNull() else null
                        )
                        set("has_solutions", state.hasSolutions)
                        set("has_notes", state.hasNotes)
                        set("photo_urls", finalPhotoUrls)
                        set("city", state.city.trim().ifBlank { null })
                        set("pincode", state.pincode.trim().ifBlank { null })
                        set("locality", state.locality.trim().ifBlank { null })
                    },
                ) {
                    filter { eq("id", listingId) }
                }
            }.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(EditListingEvent.SaveSuccess)
                },
                onFailure = { e ->
                    val msg = when (e) {
                        is RestException -> "Failed to save. Please try again."
                        is HttpRequestException -> "No internet connection."
                        else -> e.message ?: "Something went wrong."
                    }
                    _uiState.update { it.copy(isSubmitting = false, error = msg) }
                },
            )
        }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        var valid = true
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            valid = false
        }
        if (state.condition == null) {
            _uiState.update { it.copy(conditionError = "Condition is required") }
            valid = false
        }
        if (state.listingType == ListingType.SELL) {
            if (state.price.isBlank()) {
                _uiState.update { it.copy(priceError = "Price is required") }
                valid = false
            }
        }
        if (state.city.isBlank()) {
            _uiState.update { it.copy(cityError = "City is required") }
            valid = false
        }
        if (state.pincode.isBlank() || state.pincode.length != 6) {
            _uiState.update { it.copy(pincodeError = "Enter a valid 6-digit pincode") }
            valid = false
        }
        return valid
    }

    private suspend fun uploadNewPhotos(userId: String, uris: List<Uri>): List<String> =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val urls = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching
                    val path = "$userId/$listingId/edit_photo_${UUID.randomUUID()}_$index.jpg"
                    supabase.storage.from("book-photos").upload(path, bytes) { upsert = true }
                    urls.add(supabase.storage.from("book-photos").publicUrl(path))
                }
            }
            urls
        }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

@Serializable
private data class GBResponse(val items: List<GBItem>? = null)
@Serializable
private data class GBItem(val volumeInfo: GBVolumeInfo? = null)
@Serializable
private data class GBVolumeInfo(
    val title: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val categories: List<String>? = null,
)