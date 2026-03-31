package com.kitaab.app.feature.post.multi

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.data.local.db.CachedPostingSession
import com.kitaab.app.data.local.db.CachedStagedBook
import com.kitaab.app.data.local.db.CachedStagedBundle
import com.kitaab.app.data.local.db.PostingSessionDao
import com.kitaab.app.data.local.db.StagedBookDao
import com.kitaab.app.data.local.db.StagedBundleDao
import com.kitaab.app.domain.model.StagedBook
import com.kitaab.app.domain.model.StagedBundle
import com.kitaab.app.domain.repository.UserPreferencesRepository
import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.feature.post.ListingType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class MultiPostViewModel @Inject constructor(
    private val sessionDao: PostingSessionDao,
    private val bookDao: StagedBookDao,
    private val bundleDao: StagedBundleDao,
    private val userPrefs: UserPreferencesRepository,
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiPostUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<MultiPostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            sessionDao.deleteOlderThan(cutoff)
            val existing = sessionDao.getLatest()
            if (existing != null) {
                _uiState.update { it.copy(showResumeBanner = true, sessionId = existing.id) }
                observeSession(existing.id)
            } else {
                prefillDefaults()
            }
        }
    }

    private fun prefillDefaults() {
        viewModelScope.launch {
            val city = userPrefs.city.first()
            val pincode = userPrefs.pincode.first()
            val locality = userPrefs.locality.first()
            _uiState.update { state ->
                state.copy(
                    sessionDefaults = state.sessionDefaults.copy(
                        city = city,
                        pincode = pincode,
                        locality = locality,
                    ),
                )
            }
        }
    }

    private fun observeSession(sessionId: String) {
        viewModelScope.launch {
            combine(
                bookDao.observeForSession(sessionId),
                bundleDao.observeForSession(sessionId),
            ) { books: List<CachedStagedBook>, bundles: List<CachedStagedBundle> ->
                books.map { it.toDomain() } to bundles.map { it.toDomain() }
            }.catch { }
                .collect { (books, bundles) ->
                    _uiState.update { it.copy(stagedBooks = books, stagedBundles = bundles) }
                }
        }
    }

    // ── Resume banner ─────────────────────────────────────────────────────────

    fun onResumeBannerAccepted() {
        _uiState.update { it.copy(showResumeBanner = false) }
        viewModelScope.launch { _events.send(MultiPostEvent.NavigateToTray) }
    }

    fun onResumeBannerDismissed() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId ?: return@launch
            discardSession(sessionId)
            _uiState.update { it.copy(showResumeBanner = false, sessionId = null) }
            prefillDefaults()
        }
    }

    // ── Session defaults ──────────────────────────────────────────────────────

    fun onDefaultTypeSelected(type: ListingType) {
        _uiState.update { it.copy(sessionDefaults = it.sessionDefaults.copy(listingType = type)) }
    }

    fun onDefaultCityChanged(value: String) {
        _uiState.update {
            it.copy(sessionDefaults = it.sessionDefaults.copy(city = value), cityError = null)
        }
    }

    fun onDefaultPincodeChanged(value: String) {
        if (value.length <= 6 && value.all { c -> c.isDigit() }) {
            _uiState.update {
                it.copy(
                    sessionDefaults = it.sessionDefaults.copy(pincode = value),
                    pincodeError = null,
                )
            }
        }
    }

    fun onDefaultLocalityChanged(value: String) {
        _uiState.update {
            it.copy(sessionDefaults = it.sessionDefaults.copy(locality = value))
        }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true) }
            try {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                val location = suspendCancellableCoroutine { cont ->
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(loc: android.location.Location) {
                            locationManager.removeUpdates(this)
                            cont.resume(loc)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {
                        }
                    }
                    val provider = when {
                        locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ->
                            android.location.LocationManager.GPS_PROVIDER

                        locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ->
                            android.location.LocationManager.NETWORK_PROVIDER

                        else -> {
                            cont.resume(null); return@suspendCancellableCoroutine
                        }
                    }

                    @Suppress("MissingPermission")
                    val last = locationManager.getLastKnownLocation(provider)
                    if (last != null) {
                        cont.resume(last)
                    } else {
                        @Suppress("MissingPermission")
                        locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
                        cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
                    }
                }
                if (location != null) {
                    val addresses = Geocoder(context, Locale.getDefault())
                        .getFromLocation(location.latitude, location.longitude, 1)
                    val addr = addresses?.firstOrNull()
                    _uiState.update {
                        it.copy(
                            sessionDefaults = it.sessionDefaults.copy(
                                city = addr?.locality ?: addr?.subAdminArea
                                ?: it.sessionDefaults.city,
                                pincode = addr?.postalCode ?: it.sessionDefaults.pincode,
                                locality = addr?.subLocality ?: addr?.thoroughfare
                                ?: it.sessionDefaults.locality,
                            ),
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

    fun confirmSessionDefaults() {
        val defaults = _uiState.value.sessionDefaults
        var valid = true
        if (defaults.city.isBlank()) {
            _uiState.update { it.copy(cityError = "City is required") }
            valid = false
        }
        if (defaults.pincode.isBlank() || defaults.pincode.length != 6) {
            _uiState.update { it.copy(pincodeError = "Enter a valid 6-digit pincode") }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            sessionDao.upsert(
                CachedPostingSession(
                    id = sessionId,
                    defaultType = defaults.listingType.name,
                    defaultCity = defaults.city.trim(),
                    defaultPincode = defaults.pincode.trim(),
                    defaultLocality = defaults.locality.trim(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
            _uiState.update { it.copy(sessionId = sessionId) }
            observeSession(sessionId)
            _events.send(MultiPostEvent.NavigateToTray)
        }
    }

    // ── Add book sheet ────────────────────────────────────────────────────────

    fun openAddBookSheet() {
        _uiState.update { it.copy(addBookSheet = AddBookSheetState(isVisible = true)) }
    }

    fun openEditBookSheet(bookId: String) {
        viewModelScope.launch {
            val book = bookDao.getById(bookId) ?: return@launch
            val domain = book.toDomain()
            _uiState.update {
                it.copy(
                    addBookSheet = AddBookSheetState(
                        isVisible = true,
                        editingBookId = bookId,
                        title = domain.title,
                        author = domain.author,
                        publisher = domain.publisher,
                        edition = domain.edition,
                        isbn = domain.isbn,
                        subject = domain.subject,
                        examTags = domain.examTags,
                        hasSolutions = domain.hasSolutions,
                        hasNotes = domain.hasNotes,
                        condition = domain.condition,
                        individualPrice = domain.individualPrice,
                        photoUris = domain.photoPaths.map { p -> Uri.parse("file://$p") },
                        typeOverride = domain.typeOverride,
                    ),
                )
            }
        }
    }

    fun dismissAddBookSheet() {
        _uiState.update { it.copy(addBookSheet = AddBookSheetState()) }
    }

    fun onBookTitleChanged(v: String) = updateSheet { it.copy(title = v, titleError = null) }
    fun onBookAuthorChanged(v: String) = updateSheet { it.copy(author = v) }
    fun onBookPublisherChanged(v: String) = updateSheet { it.copy(publisher = v) }
    fun onBookEditionChanged(v: String) = updateSheet { it.copy(edition = v) }
    fun onBookSubjectChanged(v: String) = updateSheet { it.copy(subject = v) }

    fun onBookIsbnScanned(isbn: String) {
        updateSheet { it.copy(isbn = isbn, bookNotFound = false) }
        fetchBookDetails(isbn)
    }

    fun onExamTagToggled(tag: String) = updateSheet { sheet ->
        val tags = sheet.examTags.toMutableSet()
        if (tag in tags) tags.remove(tag) else tags.add(tag)
        sheet.copy(examTags = tags)
    }

    fun onHasSolutionsToggled() = updateSheet { it.copy(hasSolutions = !it.hasSolutions) }
    fun onHasNotesToggled() = updateSheet { it.copy(hasNotes = !it.hasNotes) }

    fun onBookConditionSelected(condition: BookCondition) =
        updateSheet { it.copy(condition = condition, conditionError = null) }

    fun onBookIndividualPriceChanged(v: String) {
        if (v.isEmpty() || v.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?\$"))) {
            updateSheet { it.copy(individualPrice = v, priceError = null) }
        }
    }

    fun onBookTypeOverrideSelected(type: ListingType?) =
        updateSheet { it.copy(typeOverride = type) }

    fun onBookPhotosSelected(uris: List<Uri>) = updateSheet { sheet ->
        val combined = (sheet.photoUris + uris).take(5)
        sheet.copy(photoUris = combined)
    }

    fun onBookPhotoRemoved(uri: Uri) =
        updateSheet { it.copy(photoUris = it.photoUris - uri) }

    fun onBookCoverPhotoSet(uri: Uri) = updateSheet { sheet ->
        val reordered = listOf(uri) + (sheet.photoUris - uri)
        sheet.copy(photoUris = reordered)
    }

    fun fetchBookDetails(isbn: String) {
        if (isbn.isBlank()) return
        viewModelScope.launch {
            updateSheet { it.copy(isFetchingBookDetails = true, bookNotFound = false) }
            runCatching {
                val client = HttpClient(Android)
                val body = client.get(
                    "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn",
                ).bodyAsText()
                client.close()
                body
            }.fold(
                onSuccess = { body ->
                    val found = parseAndApplyBookDetails(body)
                    updateSheet { it.copy(isFetchingBookDetails = false, bookNotFound = !found) }
                },
                onFailure = {
                    updateSheet { it.copy(isFetchingBookDetails = false, bookNotFound = false) }
                },
            )
        }
    }

    private fun parseAndApplyBookDetails(json: String): Boolean {
        return try {
            val parsed =
                Json { ignoreUnknownKeys = true }.decodeFromString<GoogleBooksResponse>(json)
            val info = parsed.items?.firstOrNull()?.volumeInfo ?: return false
            updateSheet { sheet ->
                sheet.copy(
                    title = info.title?.takeIf { it.isNotBlank() } ?: sheet.title,
                    author = info.authors?.joinToString(", ")?.takeIf { it.isNotBlank() }
                        ?: sheet.author,
                    publisher = info.publisher?.takeIf { it.isNotBlank() } ?: sheet.publisher,
                    subject = info.categories?.firstOrNull()?.takeIf { it.isNotBlank() }
                        ?: sheet.subject,
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun confirmBook() {
        val sheet = _uiState.value.addBookSheet
        val sessionId = _uiState.value.sessionId ?: return
        var valid = true
        if (sheet.title.isBlank()) {
            updateSheet { it.copy(titleError = "Title is required") }
            valid = false
        }
        if (sheet.condition == null) {
            updateSheet { it.copy(conditionError = "Please select a condition") }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            updateSheet { it.copy(isCopyingPhotos = true) }
            val existingBook = sheet.editingBookId?.let { bookDao.getById(it) }
            val bookId = existingBook?.id ?: UUID.randomUUID().toString()
            val stablePaths = copyPhotosToInternalStorage(
                sessionId = sessionId,
                bookId = bookId,
                uris = sheet.photoUris,
                existingBook = existingBook,
            )
            val sortOrder = existingBook?.sortOrder ?: bookDao.countForSession(sessionId)
            bookDao.upsert(
                CachedStagedBook(
                    id = bookId,
                    sessionId = sessionId,
                    bundleId = existingBook?.bundleId,
                    title = sheet.title.trim(),
                    author = sheet.author.trim(),
                    publisher = sheet.publisher.trim(),
                    edition = sheet.edition.trim(),
                    isbn = sheet.isbn.trim(),
                    subject = sheet.subject.trim(),
                    examTags = sheet.examTags.toList(),
                    hasSolutions = sheet.hasSolutions,
                    hasNotes = sheet.hasNotes,
                    condition = sheet.condition!!.name,
                    individualPrice = sheet.individualPrice.trim(),
                    photoPaths = stablePaths,
                    typeOverride = sheet.typeOverride?.name,
                    sortOrder = sortOrder,
                ),
            )
            _uiState.update { it.copy(addBookSheet = AddBookSheetState()) }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            val book = bookDao.getById(bookId)
            book?.photoPaths?.forEach { path -> File(path).deleteOnExit() }
            bookDao.delete(bookId)
        }
    }

    // ── Organise screen ───────────────────────────────────────────────────────

    fun onBookSelectionToggled(bookId: String) {
        _uiState.update { state ->
            val selection = state.selectedBookIds.toMutableSet()
            if (bookId in selection) selection.remove(bookId) else selection.add(bookId)
            state.copy(selectedBookIds = selection)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedBookIds = emptySet()) }
    }

    fun openCreateBundleSheet() {
        if (!_uiState.value.canCreateBundleFromSelection) return
        _uiState.update { it.copy(createBundleSheet = CreateBundleSheetState(isVisible = true)) }
    }

    fun openEditBundleSheet(bundleId: String) {
        viewModelScope.launch {
            val bundle = bundleDao.getById(bundleId) ?: return@launch
            _uiState.update {
                it.copy(
                    createBundleSheet = CreateBundleSheetState(
                        isVisible = true,
                        editingBundleId = bundleId,
                        name = bundle.name,
                        bundlePrice = bundle.bundlePrice,
                        typeOverride = bundle.typeOverride?.let { t -> ListingType.valueOf(t) },
                    ),
                )
            }
        }
    }

    fun dismissCreateBundleSheet() {
        _uiState.update { it.copy(createBundleSheet = CreateBundleSheetState()) }
    }

    fun onBundleNameChanged(v: String) {
        _uiState.update {
            it.copy(createBundleSheet = it.createBundleSheet.copy(name = v, nameError = null))
        }
    }

    fun onBundlePriceChanged(v: String) {
        if (v.isEmpty() || v.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?\$"))) {
            _uiState.update {
                it.copy(
                    createBundleSheet = it.createBundleSheet.copy(
                        bundlePrice = v,
                        priceError = null
                    )
                )
            }
        }
    }

    fun onBundleTypeOverrideSelected(type: ListingType?) {
        _uiState.update {
            it.copy(createBundleSheet = it.createBundleSheet.copy(typeOverride = type))
        }
    }

    fun confirmBundle() {
        val sheet = _uiState.value.createBundleSheet
        val sessionId = _uiState.value.sessionId ?: return
        val defaults = _uiState.value.sessionDefaults
        var valid = true
        if (sheet.name.isBlank()) {
            _uiState.update {
                it.copy(createBundleSheet = it.createBundleSheet.copy(nameError = "Bundle name is required"))
            }
            valid = false
        }
        val effectiveType = sheet.typeOverride ?: defaults.listingType
        if (effectiveType == ListingType.SELL && sheet.bundlePrice.isBlank()) {
            _uiState.update {
                it.copy(createBundleSheet = it.createBundleSheet.copy(priceError = "Price is required"))
            }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            val bundleId = sheet.editingBundleId ?: UUID.randomUUID().toString()
            bundleDao.upsert(
                CachedStagedBundle(
                    id = bundleId,
                    sessionId = sessionId,
                    name = sheet.name.trim(),
                    bundlePrice = sheet.bundlePrice.trim(),
                    typeOverride = sheet.typeOverride?.name,
                ),
            )
            if (!sheet.isEditing) {
                bookDao.assignBundle(bundleId, _uiState.value.selectedBookIds.toList())
            }
            _uiState.update {
                it.copy(createBundleSheet = CreateBundleSheetState(), selectedBookIds = emptySet())
            }
        }
    }

    fun ungroupBundle(bundleId: String) {
        viewModelScope.launch {
            bookDao.clearBundle(bundleId)
            bundleDao.delete(bundleId)
        }
    }

    fun navigateToReview() {
        viewModelScope.launch { _events.send(MultiPostEvent.NavigateToReview) }
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    fun publishAll() {
        val sessionId = _uiState.value.sessionId ?: return
        val state = _uiState.value
        if (!state.allListingsReady) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isPublishing = true, publishError = null, failedListingTitles = emptyList())
            }

            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (userId == null) {
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        publishError = "Session expired. Please sign in again."
                    )
                }
                return@launch
            }

            val defaults = state.sessionDefaults
            val failedTitles = mutableListOf<String>()
            var successCount = 0

            // 1. Individual listings
            state.unbundledBooks.forEach { book ->
                val effectiveType = book.typeOverride ?: defaults.listingType
                runCatching {
                    val photoUrls = uploadPhotos(userId, book.id, book.photoPaths)
                    supabase.postgrest["listings"].insert(
                        NewIndividualListing(
                            seller_id = userId,
                            title = book.title.trim(),
                            author = book.author.trim().ifBlank { null },
                            publisher = book.publisher.trim().ifBlank { null },
                            edition = book.edition.trim().ifBlank { null },
                            isbn = book.isbn.trim().ifBlank { null },
                            subject = book.subject.trim().ifBlank { null },
                            exam_tags = book.examTags.toList(),
                            condition = book.condition!!.name,
                            type = effectiveType.name,
                            price = if (effectiveType == ListingType.SELL) book.individualPrice.toDoubleOrNull() else null,
                            has_solutions = book.hasSolutions,
                            has_notes = book.hasNotes,
                            photo_urls = photoUrls,
                            status = "ACTIVE",
                            city = defaults.city.ifBlank { null },
                            pincode = defaults.pincode.ifBlank { null },
                            locality = defaults.locality.ifBlank { null },
                            is_bundle = false,
                            book_count = 1,
                        ),
                    ) { select() }
                }.fold(
                    onSuccess = { successCount++ },
                    onFailure = { e ->
                        Log.e("MultiPost", "Individual publish failed: ${e.message}", e)
                        failedTitles.add(book.title)
                    },
                )
            }

            // 2. Bundle listings
            state.stagedBundles.forEach { bundle ->
                val books = state.booksForBundle(bundle.id)
                if (books.isEmpty()) return@forEach
                val primaryBook = books.first()
                val effectiveType = bundle.typeOverride ?: defaults.listingType
                runCatching {
                    val allPhotoUrls = mutableListOf<String>()
                    books.forEach { book ->
                        allPhotoUrls += uploadPhotos(
                            userId,
                            book.id,
                            book.photoPaths
                        )
                    }
                    supabase.postgrest["listings"].insert(
                        NewBundleListing(
                            seller_id = userId,
                            title = bundle.name.trim(),
                            author = null,
                            publisher = null,
                            edition = null,
                            isbn = null,
                            subject = primaryBook.subject.trim().ifBlank { null },
                            exam_tags = primaryBook.examTags.toList(),
                            condition = primaryBook.condition?.name ?: "Good",
                            type = effectiveType.name,
                            price = if (effectiveType == ListingType.SELL) bundle.bundlePrice.toDoubleOrNull() else null,
                            has_solutions = books.any { it.hasSolutions },
                            has_notes = books.any { it.hasNotes },
                            photo_urls = allPhotoUrls,
                            status = "ACTIVE",
                            city = defaults.city.ifBlank { null },
                            pincode = defaults.pincode.ifBlank { null },
                            locality = defaults.locality.ifBlank { null },
                            is_bundle = true,
                            book_count = books.size,
                            bundle_books = books.map { b ->
                                BundleBookEntry(
                                    title = b.title,
                                    author = b.author,
                                    isbn = b.isbn,
                                    condition = b.condition?.name ?: "",
                                    individual_price = b.individualPrice.toDoubleOrNull(),
                                )
                            },
                        ),
                    ) { select() }
                }.fold(
                    onSuccess = { successCount++ },
                    onFailure = { e ->
                        Log.e("MultiPost", "Bundle publish failed: ${e.message}", e)
                        failedTitles.add(bundle.name)
                    },
                )
            }

            _uiState.update { it.copy(isPublishing = false) }

            if (failedTitles.isEmpty()) {
                discardSession(sessionId)
                _events.send(
                    MultiPostEvent.PublishComplete(
                        successCount = successCount,
                        totalBookCount = state.totalBookCount,
                    ),
                )
            } else {
                _uiState.update { it.copy(failedListingTitles = failedTitles) }
                _events.send(
                    MultiPostEvent.PublishPartialFailure(
                        successCount = successCount,
                        failedTitles = failedTitles,
                    ),
                )
            }
        }
    }

    // ── Session discard ───────────────────────────────────────────────────────

    fun discardCurrentSession() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId ?: return@launch
            discardSession(sessionId)
            _uiState.update { MultiPostUiState() }
            prefillDefaults()
            _events.send(MultiPostEvent.SessionAbandoned)
        }
    }

    private suspend fun discardSession(sessionId: String) {
        bookDao.getForSession(sessionId).forEach { book ->
            book.photoPaths.forEach { path -> File(path).delete() }
        }
        sessionDao.delete(sessionId)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun updateSheet(transform: (AddBookSheetState) -> AddBookSheetState) {
        _uiState.update { it.copy(addBookSheet = transform(it.addBookSheet)) }
    }

    private suspend fun copyPhotosToInternalStorage(
        sessionId: String,
        bookId: String,
        uris: List<Uri>,
        existingBook: CachedStagedBook?,
    ): List<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dir = File(context.filesDir, "staging/$sessionId/$bookId").apply { mkdirs() }
        val stablePaths = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            val uriString = uri.toString()
            if (uriString.startsWith("file://${context.filesDir}")) {
                stablePaths.add(uri.path ?: return@forEachIndexed)
                return@forEachIndexed
            }
            runCatching {
                val destFile = File(dir, "photo_$index.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                stablePaths.add(destFile.absolutePath)
            }
        }
        stablePaths
    }

    private suspend fun uploadPhotos(
        userId: String,
        bookId: String,
        paths: List<String>,
    ): List<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val urls = mutableListOf<String>()
        paths.forEachIndexed { index, path ->
            runCatching {
                val bytes = File(path).readBytes()
                val storagePath = "$userId/$bookId/photo_$index.jpg"
                supabase.storage.from("book-photos").upload(storagePath, bytes) { upsert = true }
                urls.add(supabase.storage.from("book-photos").publicUrl(storagePath))
            }
        }
        urls
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun CachedStagedBook.toDomain(): StagedBook = StagedBook(
    id = id,
    sessionId = sessionId,
    bundleId = bundleId,
    title = title,
    author = author,
    publisher = publisher,
    edition = edition,
    isbn = isbn,
    subject = subject,
    examTags = examTags.toSet(),
    hasSolutions = hasSolutions,
    hasNotes = hasNotes,
    condition = runCatching { BookCondition.valueOf(condition) }.getOrNull(),
    individualPrice = individualPrice,
    photoPaths = photoPaths,
    typeOverride = typeOverride?.let { runCatching { ListingType.valueOf(it) }.getOrNull() },
    sortOrder = sortOrder,
)

private fun CachedStagedBundle.toDomain(): StagedBundle = StagedBundle(
    id = id,
    sessionId = sessionId,
    name = name,
    bundlePrice = bundlePrice,
    typeOverride = typeOverride?.let { runCatching { ListingType.valueOf(it) }.getOrNull() },
)

// ── Serializable models ───────────────────────────────────────────────────────

@Serializable
private data class GoogleBooksResponse(val items: List<GoogleBooksItem>? = null)

@Serializable
private data class GoogleBooksItem(val volumeInfo: VolumeInfo? = null)

@Serializable
private data class VolumeInfo(
    val title: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val categories: List<String>? = null,
)

@Serializable
private data class NewIndividualListing(
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
    val is_bundle: Boolean,
    val book_count: Int,
)

@Serializable
private data class BundleBookEntry(
    val title: String,
    val author: String,
    val isbn: String,
    val condition: String,
    val individual_price: Double?,
)

@Serializable
private data class NewBundleListing(
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
    val is_bundle: Boolean,
    val book_count: Int,
    val bundle_books: List<BundleBookEntry>,
)