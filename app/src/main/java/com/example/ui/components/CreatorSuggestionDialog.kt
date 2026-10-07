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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PanuTheme

/**
 * Dialogue permettant aux utilisateurs de suggérer ou d'importer
 * des créateurs talentueux issus d'autres plateformes (TikTok, YouTube, Facebook, Instagram)
 * afin d'enrichir l'écosystème PANU.
 */
@Composable
fun CreatorSuggestionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmitSuggestion: (platform: String, handleOrUrl: String, name: String, cat: String) -> Unit = { _, _, _, _ -> }
) {
    if (!isOpen) return

    val context = LocalContext.current
    val colors = PanuTheme.colors

    val platforms = listOf("TikTok", "YouTube", "Facebook", "Instagram")
    var selectedPlatform by remember { mutableStateOf("TikTok") }
    var creatorHandle by remember { mutableStateOf("") }
    var channelUrl by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Humour & Comédie") }
    var recommendationReason by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    val categories = listOf(
        "Humour & Comédie",
        "Musique & Danse",
        "Tech & IA",
        "Mode & Beauté",
        "Éducation & Savoir",
        "Business & Entrepreneuriat"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("creator_suggestion_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.champagneSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Suggérer un Créateur",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Importation multi-plateformes",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.champagne
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Aidez PANU à inviter et certifier les meilleurs talents d'Afrique et de la diaspora :",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sélecteur de Plateforme
                Text(
                    text = "1. Plateforme d'origine :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    platforms.forEach { platform ->
                        FilterChip(
                            selected = selectedPlatform == platform,
                            onClick = { selectedPlatform = platform },
                            label = { Text(platform, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Handle / Nom
                OutlinedTextField(
                    value = creatorHandle,
                    onValueChange = { creatorHandle = it },
                    label = { Text("Nom ou @pseudo sur $selectedPlatform") },
                    placeholder = { Text("@nom_du_createur") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggestion_handle_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.champagne,
                        unfocusedBorderColor = colors.surfaceBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Lien vers la chaîne
                OutlinedTextField(
                    value = channelUrl,
                    onValueChange = { channelUrl = it },
                    label = { Text("Lien direct vers la chaîne / page") },
                    placeholder = { Text("https://${selectedPlatform.lowercase()}.com/@...") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = colors.champagne) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggestion_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.champagne,
                        unfocusedBorderColor = colors.surfaceBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Raison / Pourquoi ce créateur
                OutlinedTextField(
                    value = recommendationReason,
                    onValueChange = { recommendationReason = it },
                    label = { Text("Pourquoi recommandez-vous ce créateur ? (Optionnel)") },
                    placeholder = { Text("Vidéos hilarantes, tutoriels inspirants...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.champagne,
                        unfocusedBorderColor = colors.surfaceBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bouton Soumettre
                Button(
                    onClick = {
                        if (creatorHandle.isBlank()) {
                            Toast.makeText(context, "Veuillez renseigner le pseudo du créateur", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSubmitSuggestion(selectedPlatform, channelUrl.trim().ifBlank { creatorHandle.trim() }, creatorHandle.trim(), selectedCategory)
                        Toast.makeText(
                            context,
                            "🌟 Merci ! La suggestion de $creatorHandle a été transmise à l'équipe PANU pour importation.",
                            Toast.LENGTH_LONG
                        ).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_creator_suggestion"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.champagne,
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Envoyer la suggestion", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
