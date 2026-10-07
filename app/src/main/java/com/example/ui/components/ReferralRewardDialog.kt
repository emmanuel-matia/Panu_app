package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PanuTheme

/**
 * Dialogue du Programme de Parrainage & Acquisition Virale PANU
 * Offre +50 Crédits Studio IA pour chaque nouvel utilisateur invité.
 */
@Composable
fun ReferralRewardDialog(
    isOpen: Boolean,
    userId: String?,
    username: String?,
    onDismiss: () -> Unit,
    onClaimSponsorCode: (code: String) -> Unit = {}
) {
    if (!isOpen) return

    val context = LocalContext.current
    val colors = PanuTheme.colors

    val cleanUser = (username ?: userId ?: "createur").removePrefix("@").take(6).uppercase()
    val referralCode = remember(cleanUser) { "PANU-REF-$cleanUser" }
    val shareLink = "https://panu.app/join?ref=$referralCode"

    var inputSponsorCode by remember { mutableStateOf("") }
    var codeClaimed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("referral_reward_dialog"),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.champagneSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Programme de Parrainage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "+50 Crédits IA par ami invité 🎁",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.champagne
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Carte du Code Personnel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.champagne.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VOTRE CODE DE PARRAINAGE UNIQUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = referralCode,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = colors.champagne
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Copier le lien
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Code Parrainage PANU", shareLink)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Lien copié : $shareLink", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Partager
                            Button(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Rejoins-moi sur PANU Studio, le réseau et studio créatif des créateurs africains ! " +
                                                    "Utilise mon code de parrainage $referralCode pour recevoir 60 crédits IA offerts : $shareLink"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Inviter des amis sur PANU"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.champagne,
                                    contentColor = if (colors.isDark) Color.Black else Color.White
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inviter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Entrer un code parrain
                Text(
                    text = "Avez-vous été invité par un créateur ?",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputSponsorCode,
                        onValueChange = { inputSponsorCode = it.uppercase() },
                        placeholder = { Text("PANU-REF-XXXX") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("referral_input_code"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.champagne,
                            unfocusedBorderColor = colors.surfaceBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputSponsorCode.trim().length >= 6) {
                                onClaimSponsorCode(inputSponsorCode.trim())
                                codeClaimed = true
                                Toast.makeText(
                                    context,
                                    "🎉 Félicitations ! +50 Crédits Studio IA ajoutés à votre compte.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Veuillez entrer un code valide", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !codeClaimed,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_claim_referral")
                    ) {
                        Text(if (codeClaimed) "Validé" else "Activer", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Chaque ami inscrit avec votre code vous rapporte instantanément 50 crédits sans limite d'invitation.",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
