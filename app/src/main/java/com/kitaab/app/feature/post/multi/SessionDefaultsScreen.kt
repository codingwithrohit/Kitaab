package com.kitaab.app.feature.post.multi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kitaab.app.feature.post.ListingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDefaultsScreen(
    viewModel: MultiPostViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val defaults = state.sessionDefaults

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing defaults") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "Set defaults for all books in this session. You can override these individually per book or bundle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Listing type ──────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Listing type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = defaults.listingType == ListingType.SELL,
                        onClick = { viewModel.onDefaultTypeSelected(ListingType.SELL) },
                        label = { Text("Sell") },
                    )
                    FilterChip(
                        selected = defaults.listingType == ListingType.DONATE,
                        onClick = { viewModel.onDefaultTypeSelected(ListingType.DONATE) },
                        label = { Text("Donate") },
                    )
                }
            }

            // ── Location ──────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Location", style = MaterialTheme.typography.labelLarge)
                    TextButton(
                        onClick = { viewModel.fetchCurrentLocation() },
                        enabled = !state.isFetchingLocation,
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(if (state.isFetchingLocation) "Detecting…" else "Use current")
                    }
                }

                OutlinedTextField(
                    value = defaults.city,
                    onValueChange = viewModel::onDefaultCityChanged,
                    label = { Text("City") },
                    isError = state.cityError != null,
                    supportingText = state.cityError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = defaults.pincode,
                        onValueChange = viewModel::onDefaultPincodeChanged,
                        label = { Text("Pincode") },
                        isError = state.pincodeError != null,
                        supportingText = state.pincodeError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = defaults.locality,
                        onValueChange = viewModel::onDefaultLocalityChanged,
                        label = { Text("Locality (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.confirmSessionDefaults() },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
            ) {
                Text("Continue — add books")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
