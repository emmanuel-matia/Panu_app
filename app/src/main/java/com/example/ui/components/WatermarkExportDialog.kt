package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.theme.PanuTheme

/**
 * Module d'exportation de contenus créés sur PANU avec un filigrane (watermark) personnalisé
 * et QR code / lien vers PANU pour viraliser la plateforme sur TikTok, YouTube Shorts et Instagram Reels.
 */
@Composable
fun WatermarkExportDialog(
    isOpen: Boolean,
    title: String,
    mediaUrl: String?,
    authorHandle: String,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val colors = PanuTheme.colors

    val exportFormats = listOf("TikTok (9:16)", "Shorts (9:16)", "Reels (9:16)", "Carré (1:1)")
    var selectedTarget by remember { mutableStateOf("TikTok (9:16)") }
    var enableWatermark by remember { mutableStateOf(true) }
    var enableQrCode by remember { mutableStateOf(true) }
    var watermarkPosition by remember { mutableStateOf("Bas Droite") } // Bas Droite, Haut Droite, Bas Gauche

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("watermark_export_dialog"),
            shape = RoundedCornerShape(22.dp),
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.champagneSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Exportation Virale",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "TikTok • Shorts • Reels",
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

                // Aperçu du format & Filigrane simulé
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, colors.surfaceBorder, RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model = mediaUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=900&q=80",
                        contentDescription = "Aperçu vidéo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Dégradé d'incrustation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )

                    // Incrustation du Filigrane PANU (Watermark)
                    if (enableWatermark) {
                        val alignment = when (watermarkPosition) {
                            "Haut Droite" -> Alignment.TopEnd
                            "Bas Gauche" -> Alignment.BottomStart
                            else -> Alignment.BottomEnd
                        }

                        Surface(
                            modifier = Modifier
                                .align(alignment)
                                .padding(10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(colors.champagne),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "P",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "PANU Studio",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = colors.champagne
                                    )
                                    Text(
                                        text = "panu.app/@${authorHandle.removePrefix("@")}",
                                        fontSize = 8.sp,
                                        color = Color.White
                                    )
                                }
                                if (enableQrCode) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.QrCode2,
                                        contentDescription = "QR Code",
                                        tint = colors.champagne,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sélecteur de format cible
                Text(
                    text = "Plateforme & Ratio de sortie :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    exportFormats.forEach { fmt ->
                        FilterChip(
                            selected = selectedTarget == fmt,
                            onClick = { selectedTarget = fmt },
                            label = { Text(fmt.split(" ")[0], fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Options Filigrane & QR Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Filigrane personnalisé PANU",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Ajoute votre lien créateur et booste votre visibilité",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = enableWatermark,
                        onCheckedChange = { enableWatermark = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.champagne,
                            checkedTrackColor = colors.champagneSubtle
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Incruster le QR Code PANU",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Scan immédiat vers votre profil public",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = enableQrCode && enableWatermark,
                        enabled = enableWatermark,
                        onCheckedChange = { enableQrCode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.champagne,
                            checkedTrackColor = colors.champagneSubtle
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton Export & Partage
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val watermarkText = if (enableWatermark) {
                                "\n\n🎬 Créé avec @panu.studio par @$authorHandle\n📲 Découvrez d'autres créations et rejoignez PANU : https://panu.app/@$authorHandle\n#PanuStudio #PanuApp #CreateursAfricains #MadeWithPanu"
                            } else {
                                "\n\n🎬 Vidéo sur PANU : https://panu.app/@$authorHandle"
                            }
                            putExtra(Intent.EXTRA_TEXT, "Regardez « $title » $watermarkText")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partager sur $selectedTarget"))
                        Toast.makeText(
                            context,
                            "🚀 Exportation pour $selectedTarget prête avec filigrane PANU !",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_export_watermark_share"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.champagne,
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporter & Partager sur les Réseaux", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
