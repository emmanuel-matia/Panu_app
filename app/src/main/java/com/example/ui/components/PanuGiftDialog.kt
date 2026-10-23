package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveGiftCatalogItem
import com.example.data.model.Post
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.launch

@Composable
fun PanuGiftDialog(
    isOpen: Boolean,
    post: Post,
    featuresService: SupabasePanuFeaturesService?,
    onOpenRechargeModal: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val gifts = featuresService?.liveGiftsCatalog ?: listOf(
        LiveGiftCatalogItem("gift_rose", "Rose PANU", "🌹", 5, "75 FCFA"),
        LiveGiftCatalogItem("gift_crown", "Couronne Royale", "👑", 20, "300 FCFA"),
        LiveGiftCatalogItem("gift_diamond", "Diamant d'Afrique", "💎", 50, "750 FCFA"),
        LiveGiftCatalogItem("gift_lion", "Lion Majestueux", "🦁", 100, "1500 FCFA")
    )

    val currentCredits = featuresService?.panuCreditsBalance?.collectAsState()?.value ?: 60
    var selectedGift by remember { mutableStateOf(gifts.firstOrNull()) }
    var customMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        containerColor = colors.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = colors.champagne)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🎁 Envoyer un Cadeau",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "À @${post.authorUsername ?: post.authorName ?: "createur"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.champagne
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = colors.textSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Solde disponible + Bouton Recharger
                Surface(
                    color = colors.champagneSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Votre solde actuel :", fontSize = 11.sp, color = colors.textSecondary)
                            Text(
                                "$currentCredits Crédits PANU",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = colors.champagne
                            )
                        }
                        TextButton(onClick = onOpenRechargeModal) {
                            Text("+ Recharger", fontWeight = FontWeight.Bold, color = colors.emerald, fontSize = 12.sp)
                        }
                    }
                }

                Text(
                    "Choisissez une récompense pour soutenir ce contenu :",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                // Grille des cadeaux
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(gifts) { gift ->
                        val isSelected = selectedGift?.id == gift.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.champagneSubtle else colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) colors.champagne else colors.surfaceBorder
                            ),
                            modifier = Modifier
                                .clickable { selectedGift = gift }
                                .testTag("gift_item_${gift.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(gift.iconEmoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    gift.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    "${gift.creditsCost} cr.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.champagne
                                )
                            }
                        }
                    }
                }

                // Message d'encouragement
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Message d'encouragement (Optionnel)") },
                    placeholder = { Text("Superbe vidéo ! Continue comme ça 👏") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            val gift = selectedGift
            val cost = gift?.creditsCost ?: 0
            val canAfford = currentCredits >= cost

            Button(
                onClick = {
                    if (gift == null || isSending) return@Button
                    if (!canAfford) {
                        Toast.makeText(context, "Solde insuffisant ($currentCredits cr.). Rechargez vos crédits.", Toast.LENGTH_SHORT).show()
                        onOpenRechargeModal()
                        return@Button
                    }
                    isSending = true
                    scope.launch {
                        val res = featuresService?.sendLiveGift(
                            streamId = post.id,
                            receiverName = post.authorName ?: "Créateur PANU",
                            gift = gift,
                            customMessage = customMessage
                        )
                        isSending = false
                        if (res?.isSuccess == true) {
                            Toast.makeText(
                                context,
                                "🎉 ${gift.iconEmoji} ${gift.name} envoyé à @${post.authorUsername ?: post.authorName} !",
                                Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, res?.exceptionOrNull()?.localizedMessage ?: "Erreur d'envoi", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = gift != null && !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_send_gift_confirm")
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text(
                        text = if (canAfford) "Envoyer (${cost} Crédits)" else "Recharger (${cost} Crédits requis)",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = colors.textSecondary)
            }
        }
    )
}
