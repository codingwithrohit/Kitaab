package com.kitaab.app.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kitaab.app.feature.auth.kitaabTextFieldColors
import com.kitaab.app.feature.home.components.ListingCard
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal900
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

private val CONDITIONS = listOf("New", "LikeNew", "Good", "Fair", "Poor")
private val EXAM_TAGS = listOf("JEE", "NEET", "UPSC", "CAT", "GATE", "College", "Other")


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    onNavigateBack: () -> Unit,
    onListingClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd && !state.isLoadingMore && state.hasMorePages && state.hasSearched) {
                viewModel.loadNextPage()
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (state.showFilters) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onToggleFilters() },
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets.safeDrawing },
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ){
            FilterSheet(
                filters = state.filters,
                onTypeChanged = viewModel::onTypeFilterChanged,
                onConditionChanged = viewModel::onConditionFilterChanged,
                onExamTagChanged = viewModel::onExamTagFilterChanged,
                onMinPriceChanged = viewModel::onMinPriceChanged,
                onMaxPriceChanged = viewModel::onMaxPriceChanged,
                onApply = viewModel::onApplyFilters,
                onClear = viewModel::onClearFilters,
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Search bar row — no statusBarsPadding, MainScreen owns insets ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    placeholder = {
                        Text(
                            "Search books, authors...",
                            fontSize = 14.sp,
                            color = WarmMuted,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(onClick = viewModel::onClearQuery) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.onSearchSubmit()
                        },
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = kitaabTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )

                Spacer(modifier = Modifier.width(8.dp))

                BadgedBox(
                    badge = {
                        if (state.filters.isActive) {
                            Badge(containerColor = Teal500)
                        }
                    },
                ) {
                    IconButton(onClick = viewModel::onToggleFilters) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = "Filters",
                            tint = if (state.filters.isActive) Teal500
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            HorizontalDivider(color = WarmBorder)

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !state.hasSearched -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🔍", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Search for books",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try a title, author name, or subject",
                                    fontSize = 14.sp,
                                    color = WarmMuted,
                                )
                            }
                        }
                    }

                    state.isLoading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            CircularProgressIndicator(color = Teal500)
                        }
                    }

                    state.listings.isEmpty() -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "😕", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No results found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try different keywords or clear filters",
                                    fontSize = 14.sp,
                                    color = WarmMuted,
                                )
                                if (state.filters.isActive) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(onClick = viewModel::onClearFilters) {
                                        Text("Clear filters", color = Teal500)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item(key = "results_count") {
                                Text(
                                    text = if (state.hasMorePages) "Results"
                                    else "${state.listings.size} result${if (state.listings.size != 1) "s" else ""}",
                                    fontSize = 13.sp,
                                    color = WarmMuted,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }

                            itemsIndexed(
                                items = state.listings,
                                key = { _, listing -> listing.id },
                            ) { _, listing ->
                                ListingCard(
                                    listing = listing,
                                    onClick = { onListingClick(listing.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                                )
                            }

                            if (state.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Teal500,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }

                            if (!state.hasMorePages) {
                                item(key = "end_of_list") {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                    ) {
                                        Text(
                                            text = "You've seen all results",
                                            fontSize = 13.sp,
                                            color = WarmMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filters: ExploreFilters,
    onTypeChanged: (String?) -> Unit,
    onConditionChanged: (String?) -> Unit,
    onExamTagChanged: (String?) -> Unit,
    onMinPriceChanged: (String) -> Unit,
    onMaxPriceChanged: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filters",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )

                if (filters.isActive) {
                    TextButton(onClick = onClear) {
                        Text("Clear all", color = Teal500)
                    }
                }
            }
        }

        // ── Type ─────────────────────────────
        item {
            FilterSectionLabel("Type")

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SELL" to "For sale", "DONATE" to "Free / Donate")
                    .forEach { (value, label) ->
                        val selected = filters.type == value

                        FilterChip(
                            selected = selected,
                            onClick = { onTypeChanged(if (selected) null else value) },
                            label = { Text(label, fontSize = 13.sp) },
                            colors = chipColors(),
                            border = chipBorder(selected),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
            }
        }

        item { HorizontalDivider(color = WarmBorder) }

        // ── Condition ────────────────────────
        item {
            FilterSectionLabel("Condition")

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CONDITIONS.forEach { condition ->
                    val selected = filters.condition == condition

                    FilterChip(
                        selected = selected,
                        onClick = {
                            onConditionChanged(if (selected) null else condition)
                        },
                        label = { Text(condition, fontSize = 13.sp) },
                        colors = chipColors(),
                        border = chipBorder(selected),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        }

        item { HorizontalDivider(color = WarmBorder) }

        // ── Exam Tag ─────────────────────────
        item {
            FilterSectionLabel("Exam")

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EXAM_TAGS.forEach { tag ->
                    val selected = filters.examTag == tag

                    FilterChip(
                        selected = selected,
                        onClick = {
                            onExamTagChanged(if (selected) null else tag)
                        },
                        label = { Text(tag, fontSize = 13.sp) },
                        colors = chipColors(),
                        border = chipBorder(selected),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        }

        // ── Price Range ──────────────────────
        if (filters.type != "DONATE") {
            item { HorizontalDivider(color = WarmBorder) }

            item {
                FilterSectionLabel("Price range (₹)")

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = filters.minPrice,
                        onValueChange = onMinPriceChanged,
                        label = { Text("Min") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = kitaabTextFieldColors(),
                    )

                    OutlinedTextField(
                        value = filters.maxPrice,
                        onValueChange = onMaxPriceChanged,
                        label = { Text("Max") },
                        placeholder = { Text("Any") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = kitaabTextFieldColors(),
                    )
                }
            }
        }

        // ── Apply Button ─────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal500),
            ) {
                Text("Apply filters")
            }
        }

        // prevents last item being hidden by keyboard
        item {
            Spacer(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.ime)
            )
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Teal50,
    selectedLabelColor = Teal900,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    selectedBorderColor = Teal500,
    selectedBorderWidth = 1.5.dp,
)