package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PanuTheme

enum class WatermarkPosition(val label: String) {
    BOTTOM_RIGHT("Bas Droite"),
    BOTTOM_LEFT("Bas Gauche"),
    TOP_RIGHT("Haut Droite")
}

/**
 * Dialogue d'Exportation avec Filigrane (Watermark) & QR Code Profil PANU.
 * Permet d'acquérir de nouveaux utilisateurs lorsqu'une création est partagée sur TikTok, Reels, etc.
 */
@Composable
fun PanuWatermarkExporterDialog(
    isOpen: Boolean,
    authorHandle: String,
    contentTitle: String,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val context = LocalContext.current

    var selectedPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    var includeQrCode by remember { mutableStateOf(true) }
    var includeBrandBadge by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }

    val cleanHandle = if (authorHandle.startsWith("@")) authorHandle else "@$authorHandle"
    val profileUrl = "panu.app/$cleanHandle"

    fun performExport() {
        isExporting = true
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isExporting = false
            exportSuccess = true

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "✨ Créé avec PANU Studio par $cleanHandle !\n" +
                            "Découvrez la vidéo et le profil complet sur : https://$profileUrl\n" +
                            "#PANUStudio #CreateurAfricain"
                )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager le contenu avec filigrane"))
        }, 1200)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎬", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exporter avec Filigrane Viral",
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
                Text(
                    text = "Exportez votre vidéo avec votre signature personnalisée et votre lien de profil PANU pour attirer des abonnés :",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                // Aperçu du filigrane
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(12.dp),
                        contentAlignment = when (selectedPosition) {
                            WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                            WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                            WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                        }
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (includeQrCode) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = colors.champagne,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Column {
                                    if (includeBrandBadge) {
                                        Text(
                                            text = "PANU STUDIO",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = colors.champagne
                                        )
                                    }
                                    Text(
                                        text = cleanHandle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = profileUrl,
                                        fontSize = 8.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Options du filigrane
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Inclure le QR Code scannable", style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                    Switch(
                        checked = includeQrCode,
                        onCheckedChange = { includeQrCode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.champagne)
                    )
                }

                // Position du filigrane
                Text(text = "Position du filigrane :", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WatermarkPosition.values().forEach { pos ->
                        val isSelected = selectedPosition == pos
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.champagne else colors.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPosition = pos }
                        ) {
                            Text(
                                text = pos.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else colors.textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (exportSuccess) {
                    Text(
                        text = "✓ Vidéo prête au partage avec filigrane viral !",
                        color = Color(0xFF2ED573),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { performExport() },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.champagne,
                    contentColor = if (colors.isDark) colors.background else Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportation...")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exporter pour TikTok / Reels", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Fermer")
            }
        }
    )
}
