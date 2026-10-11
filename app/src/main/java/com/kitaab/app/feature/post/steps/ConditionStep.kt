package com.kitaab.app.feature.post.steps

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.ui.theme.Teal50
import com.kitaab.app.ui.theme.Teal500
import com.kitaab.app.ui.theme.WarmBorder
import com.kitaab.app.ui.theme.WarmMuted

@Composable
fun ConditionStep(
    selectedCondition: BookCondition?,
    conditionError: String?,
    onConditionSelected: (BookCondition) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "What's the condition?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Be honest — it builds trust with buyers",
            fontSize = 14.sp,
            color = WarmMuted,
        )

        if (conditionError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = conditionError,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BookCondition.entries.forEach { condition ->
            ConditionCard(
                condition = condition,
                isSelected = selectedCondition == condition,
                onClick = { onConditionSelected(condition) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ConditionCard(
    condition: BookCondition,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) Teal500 else WarmBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val containerColor = if (isSelected) Teal50 else MaterialTheme.colorScheme.surface

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
                .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = condition.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Teal500 else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = condition.description,
                    fontSize = 13.sp,
                    color = WarmMuted,
                )
            }
            Icon(
                imageVector =
                    if (isSelected) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                contentDescription = null,
                tint = if (isSelected) Teal500 else WarmMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
