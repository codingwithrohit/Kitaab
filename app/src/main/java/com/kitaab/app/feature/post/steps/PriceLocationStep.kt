package com.kitaab.app.feature.post.steps

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitaab.app.feature.auth.kitaabTextFieldColors
import com.kitaab.app.feature.post.ListingType
import com.kitaab.app.feature.post.PostUiState
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted
import com.kitaab.app.ui.theme.WarmSurface

@Composable
fun PriceLocationStep(
    state: PostUiState,
    onPriceChanged: (String) -> Unit,
    onCityChanged: (String) -> Unit,
    onPincodeChanged: (String) -> Unit,
    onLocalityChanged: (String) -> Unit,
    onFetchLocation: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) onFetchLocation()
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Almost done",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                if (state.listingType == ListingType.SELL) {
                    "Set your price and pickup location"
                } else {
                    "Set your pickup location"
                },
            fontSize = 14.sp,
            color = WarmMuted,
        )
        Spacer(modifier = Modifier.height(32.dp))

        // ── Price (SELL only) ─────────────────────────────────────────────────
        if (state.listingType == ListingType.SELL) {
            OutlinedTextField(
                value = state.price,
                onValueChange = onPriceChanged,
                label = { Text("Price (₹)") },
                isError = state.priceError != null,
                supportingText = state.priceError?.let { { Text(it) } },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.CurrencyRupee,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = kitaabTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WarmSurface),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎁", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Free donation",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Students submit a reason why they need this book. You pick the most deserving.",
                            fontSize = 13.sp,
                            color = WarmMuted,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Location header with GPS button ───────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Pickup location",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                enabled = !state.isFetchingLocation,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Teal500),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 6.dp,
                    ),
            ) {
                if (state.isFetchingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Teal500,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Outlined.MyLocation,
                        contentDescription = null,
                        tint = Teal500,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Use GPS",
                        color = Teal500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── City ──────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = state.city,
            onValueChange = onCityChanged,
            label = { Text("City") },
            isError = state.cityError != null,
            supportingText = state.cityError?.let { { Text(it) } },
            leadingIcon = {
                Icon(
                    Icons.Outlined.LocationCity,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Pincode ───────────────────────────────────────────────────────────
        OutlinedTextField(
            value = state.pincode,
            onValueChange = onPincodeChanged,
            label = { Text("Pincode") },
            isError = state.pincodeError != null,
            supportingText = state.pincodeError?.let { { Text(it) } },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Pin,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Locality ──────────────────────────────────────────────────────────
        OutlinedTextField(
            value = state.locality,
            onValueChange = onLocalityChanged,
            label = { Text("Area / Locality (optional)") },
            placeholder = { Text("e.g. Rajouri Garden, Saket", color = WarmMuted) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kitaabTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Preview card ──────────────────────────────────────────────────────
        if (state.title.isNotBlank()) {
            Text(
                text = "Preview",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ListingPreviewCard(state = state)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ListingPreviewCard(state: PostUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, WarmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = state.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.author.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = state.author, fontSize = 13.sp, color = WarmMuted)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                if (state.condition != null) {
                    PreviewBadge(text = state.condition.label, color = Teal500)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                val typeLabel =
                    if (state.listingType == ListingType.SELL) {
                        if (state.price.isNotBlank()) "₹${state.price}" else "SELL"
                    } else {
                        "FREE"
                    }
                PreviewBadge(
                    text = typeLabel,
                    color =
                        if (state.listingType == ListingType.DONATE) {
                            Teal500
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            }
            val locationParts =
                listOfNotNull(
                    state.locality.trim().ifBlank { null },
                    state.city.trim().ifBlank { null },
                    state.pincode.trim().ifBlank { null },
                )
            if (locationParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📍 ${locationParts.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = WarmMuted,
                )
            }
        }
    }
}

@Composable
private fun PreviewBadge(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
