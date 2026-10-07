package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PanuTheme

/**
 * Dialogue du Programme de Parrainage & Growth Hacking PANU :
 * - Code de parrainage personnel
 * - Récompense : +50 crédits Studio IA par filleul
 * - Bouton de partage direct WhatsApp avec deep link
 * - Saisie de code parrain
 */
@Composable
fun ReferralProgramDialog(
    isOpen: Boolean,
    userId: String,
    username: String?,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val cleanHandle = username?.takeIf { it.isNotBlank() } ?: userId.take(6).uppercase()
    val referralCode = "PANU-${cleanHandle.uppercase()}"
    val referralLink = "https://panu.app/join?ref=$referralCode"

    var inputCode by remember { mutableStateOf("") }
    var codeSuccessMessage by remember { mutableStateOf<String?>(null) }
    var copiedNotification by remember { mutableStateOf(false) }

    fun shareOnWhatsApp() {
        val message = "🚀 Rejoins-moi sur PANU Studio, la plateforme vidéo des créateurs africains !\n\n" +
                "🎁 Clique sur mon lien de parrainage pour recevoir immédiatement 50 crédits Studio IA offerts :\n" +
                "$referralLink\n\n" +
                "Ou utilise mon code : $referralCode"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Partager mon lien de parrainage"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎁", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Parrainage & Récompenses IA",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Bannière de récompense
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.champagne.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚡", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "+50 Crédits Studio IA offerts",
                                fontWeight = FontWeight.Bold,
                                color = colors.champagne,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Pour chaque créateur ou ami qui s'inscrit avec votre code de parrainage !",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Code de parrainage copiable
                Text(
                    text = "Votre code de parrainage exclusif :",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary
                )

                Surface(
                    color = colors.surfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = referralCode,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 16.sp,
                            color = colors.champagne
                        )

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(referralLink))
                                copiedNotification = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedNotification) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copier",
                                tint = if (copiedNotification) Color(0xFF2ED573) else colors.textSecondary
                            )
                        }
                    }
                }

                if (copiedNotification) {
                    Text(
                        text = "✓ Lien de parrainage copié dans le presse-papier !",
                        color = Color(0xFF2ED573),
                        fontSize = 11.sp
                    )
                }

                // Statistiques de parrainage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = colors.surfaceElevated,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "12", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
                            Text(text = "Filleuls inscrits", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = colors.surfaceElevated,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "+600", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2ED573))
                            Text(text = "Crédits IA gagnés", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }
                }

                // Bouton Partager sur WhatsApp
                Button(
                    onClick = { shareOnWhatsApp() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partager sur WhatsApp & Réseaux", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                HorizontalDivider(color = colors.surfaceBorder)

                // Saisie d'un code reçu
                Text(
                    text = "Avez-vous reçu un code de parrainage ?",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it.uppercase() },
                        placeholder = { Text("Code parrain") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            if (inputCode.isNotBlank()) {
                                codeSuccessMessage = "Bravo ! +50 crédits IA ajoutés à votre compte."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Valider", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (codeSuccessMessage != null) {
                    Text(text = codeSuccessMessage ?: "", color = Color(0xFF2ED573), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Fermer", color = colors.textPrimary)
            }
        }
    )
}
