package com.kitaab.app.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.ui.theme.ShelfPlank
import com.kitaab.app.ui.theme.ShelfShadow
import com.kitaab.app.ui.theme.ShelfWood
import kotlin.random.Random

@Composable
fun ShelfRow(
    listings: List<Listing>,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(ShelfWood),
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                listings.forEachIndexed { index, listing ->
                    // Vary heights slightly for a real bookshelf feel
                    val spineHeight = remember(listing.id) {
                        val seed = listing.id.hashCode()
                        val rng = Random(seed)
                        (50 + rng.nextInt(30)).dp
                    }
                    val spineWidth = remember(listing.id) {
                        val seed = listing.id.hashCode() + 1
                        val rng = Random(seed)
                        (18 + rng.nextInt(12)).dp
                    }

                    BookSpine(
                        title = listing.title,
                        color = spineColorForIndex(index),
                        width = spineWidth,
                        height = spineHeight,
                        onClick = { onListingClick(listing.id) },
                    )
                }

                if (listings.isEmpty()) {
                    EmptyShelfPlaceholder()
                }
            }
        }

        // Wooden plank at the bottom of the shelf
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(ShelfPlank),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ShelfShadow),
        )
    }
}

@Composable
private fun EmptyShelfPlaceholder() {
    Text(
        text = "Your shelf is empty — post a book!",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
    )
}