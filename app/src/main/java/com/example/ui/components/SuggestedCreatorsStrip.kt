package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PanuTheme

// Modèle temporaire pour les créateurs suggérés
data class SuggestedCreator(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String? = null
)

@Composable
fun SuggestedCreatorsStrip(
    creators: List<SuggestedCreator>,
    onNavigateToProfile: (String) -> Unit,
    onFollow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PanuTheme.colors

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = "Créateurs suggérés",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(creators, key = { it.id }) { creator ->
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .testTag("creator_suggestion_card_${creator.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PanuAvatar(
                            avatarUrl = creator.avatarUrl,
                            fullName = creator.name,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = creator.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = creator.handle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onFollow(creator.id) },
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.champagne,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            )
                        ) {
                            Text("Suivre", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
