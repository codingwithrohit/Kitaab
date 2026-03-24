package com.kitaab.app.feature.post.steps

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitaab.app.feature.post.ListingType
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@Composable
fun ChooseTypeStep(
    selectedType: ListingType?,
    onTypeSelected: (ListingType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "What do you want to do?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose how you want to list your book",
            fontSize = 14.sp,
            color = WarmMuted,
        )
        Spacer(modifier = Modifier.height(32.dp))

        ListingTypeCard(
            title = "Sell",
            subtitle = "Set a price and earn from books you no longer need",
            icon = Icons.Outlined.Sell,
            isSelected = selectedType == ListingType.SELL,
            onClick = { onTypeSelected(ListingType.SELL) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        ListingTypeCard(
            title = "Donate",
            subtitle = "Give your books to students who need them most",
            icon = Icons.Outlined.CardGiftcard,
            isSelected = selectedType == ListingType.DONATE,
            onClick = { onTypeSelected(ListingType.DONATE) },
        )
    }
}

@Composable
private fun ListingTypeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) Teal500 else WarmBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val containerColor = if (isSelected) Teal50 else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Teal500 else WarmMuted,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Teal500 else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = WarmMuted,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}