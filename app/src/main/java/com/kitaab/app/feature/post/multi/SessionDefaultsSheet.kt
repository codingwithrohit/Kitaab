package com.kitaab.app.feature.post.multi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kitaab.app.feature.post.ListingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDefaultsSheet(
    viewModel: MultiPostViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val defaults = state.sessionDefaults
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { viewModel.onSessionDefaultsSheetDismissed() },
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Session defaults",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (state.sessionDefaultsRequiredBanner) {
                    "Location is needed to continue. Set it once for all books in this session."
                } else {
                    "Set once for all books. Override per book or bundle anytime."
                },
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (state.sessionDefaultsRequiredBanner) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            // Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("I want to", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = defaults.listingType == ListingType.SELL,
                        onClick = { viewModel.onDefaultTypeSelected(ListingType.SELL) },
                        label = { Text("Sell books") },
                    )
                    FilterChip(
                        selected = defaults.listingType == ListingType.DONATE,
                        onClick = { viewModel.onDefaultTypeSelected(ListingType.DONATE) },
                        label = { Text("Donate books") },
                    )
                }
            }

            // Location
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Pickup location", style = MaterialTheme.typography.labelLarge)
                    TextButton(
                        onClick = { viewModel.fetchCurrentLocation() },
                        enabled = !state.isFetchingLocation,
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(if (state.isFetchingLocation) "Detecting…" else "Use GPS")
                    }
                }

                OutlinedTextField(
                    value = defaults.city,
                    onValueChange = viewModel::onDefaultCityChanged,
                    label = { Text("City *") },
                    isError = state.cityError != null,
                    supportingText = state.cityError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = defaults.pincode,
                        onValueChange = viewModel::onDefaultPincodeChanged,
                        label = { Text("Pincode *") },
                        isError = state.pincodeError != null,
                        supportingText = state.pincodeError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = defaults.locality,
                        onValueChange = viewModel::onDefaultLocalityChanged,
                        label = { Text("Area (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }

            Button(
                onClick = { viewModel.confirmSessionDefaults() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start adding books")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
