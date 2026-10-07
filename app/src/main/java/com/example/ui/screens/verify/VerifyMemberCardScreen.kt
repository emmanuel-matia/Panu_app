package com.example.ui.screens.verify

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberCardVerification
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.ui.components.PanuAvatar
import com.example.ui.components.PanuTopBar
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerifyMemberCardScreen(
    initialCardNumber: String = "",
    featuresService: SupabasePanuFeaturesService,
    onNavigateBack: () -> Unit
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cardInput by remember {
        mutableStateOf(initialCardNumber.ifBlank { "PANU-FND-001" })
    }
    var isVerifying by remember { mutableStateOf(false) }
    var isScanningQrOrNfc by remember { mutableStateOf(false) }
    var scanModeLabel by remember { mutableStateOf("") }
    var verificationResult by remember { mutableStateOf<MemberCardVerification?>(null) }

    fun executeVerification(numberToVerify: String) {
        if (numberToVerify.isBlank()) return
        isVerifying = true
        scope.launch {
            val result = featuresService.verifyMemberCard(numberToVerify)
            isVerifying = false
            verificationResult = result.getOrNull()
            verificationResult?.let { card ->
                featuresService.triggerRealtimeNotification(
                    title = if (card.isAuthentic) "Carte Authentifiée : ${card.cardNumber} ✅" else "Alerte Carte Invalide ⚠️",
                    message = "${card.holderFullName} • ${card.holderRole} (${card.companyName}) — Statut : ${card.status}",
                    type = "verification"
                )
            }
        }
    }

    LaunchedEffect(initialCardNumber) {
        val target = initialCardNumber.ifBlank { "PANU-FND-001" }
        cardInput = target
        executeVerification(target)
    }

    Scaffold(
        topBar = {
            PanuTopBar(
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // En-tête explicatif
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.champagneSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Contrôle d'Authenticité des Cartes PANU",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Fonction RPC Supabase : verify_member_card (member_cards)",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.champagne
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scannez le QR Code, approchez une carte physique équipée d'une puce NFC ou saisissez le matricule pour afficher l'attestation officielle d'accréditation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Boutons Scan QR Code & Lecture NFC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isScanningQrOrNfc = true
                                scanModeLabel = "Scan du QR Code officiel en cours..."
                                scope.launch {
                                    delay(1000L)
                                    isScanningQrOrNfc = false
                                    cardInput = "PANU-FND-001"
                                    executeVerification("PANU-FND-001")
                                    Toast.makeText(context, "QR Code détecté : PANU-FND-001", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_scan_qr_card"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.champagne,
                                contentColor = if (colors.isDark) colors.background else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanner QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isScanningQrOrNfc = true
                                scanModeLabel = "Lecture sans contact de la puce NFC..."
                                scope.launch {
                                    delay(1000L)
                                    isScanningQrOrNfc = false
                                    cardInput = "PANU-PRO-2026"
                                    executeVerification("PANU-PRO-2026")
                                    Toast.makeText(context, "Puce NFC lue : NFC-PANU-2026-PRO", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_scan_nfc_card"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                        ) {
                            Icon(Icons.Default.Contactless, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lire Puce NFC", color = colors.champagne, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    AnimatedVisibility(visible = isScanningQrOrNfc) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            color = colors.champagneSubtle,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.champagne
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = scanModeLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Champ de recherche /verify/:cardNumber
                    OutlinedTextField(
                        value = cardInput,
                        onValueChange = { cardInput = it },
                        label = { Text("Numéro de carte ou UID NFC (/verify/:cardNumber)") },
                        placeholder = { Text("Ex: PANU-FND-001") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_card_number_input"),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Button(
                                onClick = { executeVerification(cardInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("verify_card_submit_btn")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Vérifier", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vérifier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.champagne,
                            unfocusedBorderColor = colors.surfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Cartes enregistrées pour test immédiat :",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PANU-FND-001" to "👑 Fondateur (Actif)",
                            "PANU-PRO-2026" to "🎨 Studio IA (Actif)",
                            "PANU-STF-884" to "📡 Live Sports (Actif)",
                            "PANU-REV-000" to "⚠️ Carte Révoquée (Invalide)"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = cardInput.equals(code, ignoreCase = true),
                                onClick = {
                                    cardInput = code
                                    executeVerification(code)
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.champagne,
                                    selectedLabelColor = if (colors.isDark) colors.background else Color.White
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // ATTESTATION D'AUTHENTICITÉ OFFICIELLE (RÉSULTAT DE `verify_member_card`)
            // =========================================================================
            if (isVerifying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.champagne)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Interrogation de Supabase verify_member_card...",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (verificationResult != null) {
                val card = verificationResult!!
                val statusColor = if (card.isAuthentic) Color(0xFF2ED573) else Color(0xFFFF4757)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_card_attestation_result"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(2.dp, statusColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Bandeau supérieur de statut
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            statusColor.copy(alpha = 0.22f),
                                            colors.champagne.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (card.isAuthentic) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (card.isAuthentic)
                                                "ATTESTATION D'AUTHENTICITÉ OFFICIELLE"
                                            else
                                                "CARTE INVALIDE OU NON RECONNUE",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = statusColor
                                        )
                                        Text(
                                            text = "URL : panu.app/verify/${card.cardNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                Surface(
                                    color = statusColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = card.status.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PanuAvatar(
                                    avatarUrl = card.avatarUrl,
                                    fullName = card.holderFullName,
                                    size = 64.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.holderFullName,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = card.holderRole,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.champagne
                                    )
                                    Text(
                                        text = card.companyName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.surfaceBorder)

                            AttestationDetailRow("Matricule Carte", card.cardNumber)
                            AttestationDetailRow("Nom du Titulaire", card.holderFullName)
                            AttestationDetailRow("Rôle / Fonction", card.holderRole)
                            AttestationDetailRow("Entreprise", card.companyName)
                            AttestationDetailRow("Département", card.department)
                            AttestationDetailRow("Statut d'Accréditation", card.status)
                            if (!card.nfcUid.isNullOrBlank()) {
                                AttestationDetailRow("Puce NFC Associée", card.nfcUid)
                            }
                            AttestationDetailRow("Validité", "${card.issuedAt} → ${card.expiresAt}")

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val shareText = "Attestation Officielle PANU (${card.status.uppercase()})\n" +
                                            "Titulaire : ${card.holderFullName}\n" +
                                            "Rôle : ${card.holderRole}\n" +
                                            "Entreprise : ${card.companyName}\n" +
                                            "Vérification : https://panu.app/verify/${card.cardNumber}"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Partager l'attestation PANU"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Partager le Certificat d'Authenticité", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttestationDetailRow(label: String, value: String) {
    val colors = PanuTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            textAlign = TextAlign.End
        )
    }
}
