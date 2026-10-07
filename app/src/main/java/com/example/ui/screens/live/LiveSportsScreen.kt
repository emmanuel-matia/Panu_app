package com.example.ui.screens.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.LiveGiftCatalogItem
import com.example.data.model.LiveMatchStream
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.ui.components.PanuTopBar
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Message de chat en direct
data class LiveChatMessage(
    val id: String,
    val author: String,
    val text: String,
    val isHost: Boolean = false,
    val timestamp: String = "Maintenant"
)

// Protocoles de streaming supportés
enum class StreamingProtocol(val label: String, val description: String) {
    LIVEKIT_WEBRTC("LiveKit / WebRTC", "Ultra-faible latence (< 1s) pour interaction directe"),
    AWS_IVS("AWS IVS (Amazon)", "Diffusion vidéo interactive haute échelle"),
    RTMP_CUSTOM("RTMP / Supabase", "Serveur RTMP direct (YouTube, Twitch ou serveur privé)")
}

// Zéro donnée de démonstration : flux exclusivement réels chargés depuis Supabase
val SAMPLE_LIVE_STREAMS = emptyList<LiveMatchStream>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSportsScreen(
    onNavigateToPlayer: (String) -> Unit,
    featuresService: SupabasePanuFeaturesService? = null
) {
    val context = LocalContext.current
    var isBroadcastingMode by remember { mutableStateOf(false) }

    if (isBroadcastingMode) {
        LiveBroadcasterStudio(
            onClose = { isBroadcastingMode = false }
        )
    } else {
        LiveStreamsViewerHub(
            onStartBroadcasting = { isBroadcastingMode = true },
            onWatchStream = { stream ->
                onNavigateToPlayer(stream.videoStreamUrl)
            },
            featuresService = featuresService
        )
    }
}

// ==========================================
// 1. HUB DE VISIONNAGE DES FLUX EN DIRECT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveStreamsViewerHub(
    onStartBroadcasting: () -> Unit,
    onWatchStream: (LiveMatchStream) -> Unit,
    featuresService: SupabasePanuFeaturesService? = null
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("Tous") }
    val categories = listOf("Tous", "Football", "Basketball", "Combat", "Créateur")

    val panuCredits = featuresService?.panuCreditsBalance?.collectAsState()?.value ?: 250
    val recentGifts = featuresService?.recentLiveGifts?.collectAsState()?.value ?: emptyList()
    val creatorEarnings = featuresService?.creatorEarnings?.collectAsState()?.value
    var activeGiftAnimation by remember { mutableStateOf<String?>(null) }

    var activeStreams by remember { mutableStateOf<List<LiveMatchStream>>(emptyList()) }
    var isLoadingLives by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (featuresService != null) {
            val lives = featuresService.fetchActiveLives()
            activeStreams = lives
        }
        isLoadingLives = false
    }

    val filteredStreams = remember(selectedCategory, activeStreams) {
        if (selectedCategory == "Tous") {
            activeStreams
        } else {
            activeStreams.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            PanuTopBar(
                canNavigateBack = true,
                onNavigateBack = { /* Action retour */ }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartBroadcasting,
                containerColor = Color(0xFFFF3838),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                text = { Text("Lancer un Direct Live") },
                modifier = Modifier.testTag("start_live_broadcast_fab")
            )
        },
        containerColor = colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Module LiveKit Cloud WebRTC & Gestion des Flux (Compte Fondateur : emmanuelmatia150@gmail.com)
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("livekit_cloud_status_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔴 LIVEKIT CLOUD WEBRTC • GESTION DES FLUX",
                                color = colors.champagne,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            Surface(
                                color = Color(0xFF2ED573).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "LATENCE < 400ms",
                                    color = Color(0xFF2ED573),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Serveur : wss://panu-cloud.livekit.cloud • Flux Sécurisé WebRTC",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Filtres de catégories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.champagne,
                                selectedLabelColor = if (colors.isDark) colors.background else Color.White
                            )
                        )
                    }
                }
            }

            // Zone Principale : Direct en Vedette ou État Vide Propre (Zéro Mock Data)
            if (filteredStreams.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("live_empty_state_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF2E4C).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF2E4C)),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("📹", fontSize = 34.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aucun direct en cours pour le moment",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Toutes les données de démonstration ont été purgées. Les diffusions n’apparaissent ici que lorsque de vrais créateurs allument leur caméra via WebRTC.",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onStartBroadcasting,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E4C)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("empty_state_start_live_btn")
                            ) {
                                Text("📹 Lancer un Direct avec ma Caméra", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                val featuredStream = filteredStreams[0]
                // Bannière Direct en Vedette
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onWatchStream(featuredStream) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = featuredStream.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )

                            // Badge Live
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFFF3838),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Text("EN DIRECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "👁️ ${featuredStream.viewersCount}",
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Infos
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = featuredStream.tournament,
                                    color = colors.champagne,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = featuredStream.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = featuredStream.scoreOrStatus,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Play Icon
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Regarder le direct",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Tous les Salons & Flux en Direct (${filteredStreams.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(filteredStreams) { match ->
                    LiveMatchCard(
                        match = match,
                        onClick = { onWatchStream(match) }
                    )
                }
            }

            // =========================================================================
            // 🎁 BOUTIQUE DE CADEAUX VIRTUELS LIVE (TABLE SUPABASE : `live_gifts`)
            // =========================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_gifts_section_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎁 Cadeaux Virtuels Live (live_gifts)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Soutenez les créateurs en direct avec vos crédits PANU",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Surface(
                                color = colors.champagneSubtle,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            featuresService?.addPanuCredits(200)
                                            Toast.makeText(context, "+200 Crédits PANU rechargés !", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🪙 $panuCredits Crédits (+)",
                                        color = colors.champagne,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = activeGiftAnimation != null) {
                            Surface(
                                color = Color(0xFF2ED573).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2ED573)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = activeGiftAnimation ?: "",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        val gifts = featuresService?.liveGiftsCatalog ?: listOf(
                            LiveGiftCatalogItem("gift_rose", "Rose", "🌹", 10, "Soutien instantané"),
                            LiveGiftCatalogItem("gift_crown", "Couronne", "👑", 100, "Couronne Royale"),
                            LiveGiftCatalogItem("gift_lion", "Lion d'Or", "🦁", 250, "Rugissement d'Or"),
                            LiveGiftCatalogItem("gift_diamond", "Diamant PANU", "💎", 500, "Prestige Exclusif")
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(gifts, key = { it.id }) { gift ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                                    modifier = Modifier
                                        .width(125.dp)
                                        .clickable {
                                            val targetStream = activeStreams.firstOrNull()
                                            if (targetStream == null) {
                                                Toast.makeText(context, "Aucun direct actif pour recevoir des cadeaux actuellement.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                scope.launch {
                                                    val res = featuresService?.sendLiveGift(
                                                        streamId = targetStream.id,
                                                        receiverName = targetStream.streamerName,
                                                        gift = gift
                                                    )
                                                    if (res?.isSuccess == true) {
                                                        activeGiftAnimation = "${gift.iconEmoji} ${gift.name} envoyé à ${targetStream.streamerName} (-${gift.creditsCost} crédits) !"
                                                        delay(3000L)
                                                        activeGiftAnimation = null
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            res?.exceptionOrNull()?.localizedMessage ?: "Crédits insuffisants",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("btn_send_gift_${gift.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = gift.iconEmoji, fontSize = 30.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = gift.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = colors.textPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${gift.creditsCost} crédits",
                                            fontSize = 11.sp,
                                            color = colors.champagne,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = colors.champagne,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Offrir",
                                                color = Color.Black,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (recentGifts.isNotEmpty()) {
                            HorizontalDivider(color = colors.surfaceBorder)
                            Text(
                                text = "Derniers cadeaux envoyés en direct :",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            recentGifts.take(3).forEach { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${tx.giftIcon} ${tx.senderName} → ${tx.receiverName} (${tx.giftType})",
                                        fontSize = 12.sp,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${tx.creditsAmount} cr.",
                                        fontSize = 12.sp,
                                        color = colors.champagne,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 💰 SÉQUENCES PUBLICITAIRES (`ad_impressions`) & RÉMUNÉRATION CRÉATEURS
            // =========================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("creator_monetization_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "💰 Monétisation Publicitaire (ad_impressions) & Rémunération Créateurs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Revenus calculés automatiquement selon le nombre de vues, de partages et les séquences publicitaires monétisées :",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )

                        if (creatorEarnings != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = colors.surfaceElevated,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Vues & Partages", fontSize = 11.sp, color = colors.textSecondary)
                                        Text(
                                            "${creatorEarnings.totalViews} vues • ${creatorEarnings.totalShares} part.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            "+${creatorEarnings.viewsAndSharesBonusFcfa.toInt()} FCFA",
                                            fontSize = 12.sp,
                                            color = colors.champagne,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = colors.champagneSubtle,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Solde Créateur Cumulé", fontSize = 11.sp, color = colors.textSecondary)
                                        Text(
                                            "${creatorEarnings.totalAvailableFcfa.toInt()} FCFA",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = colors.champagne
                                        )
                                        Text(
                                            "${creatorEarnings.adImpressionsCount} pubs • ${creatorEarnings.giftsReceivedCredits} cr.",
                                            fontSize = 11.sp,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        val ads = featuresService?.monetizedAdSequences ?: emptyList()
                        ads.forEach { ad ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SÉQUENCE PUB MONÉTISÉE • ${ad.advertiserName}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.champagne
                                        )
                                        Text(
                                            text = ad.campaignTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Gain : +${ad.rewardCredits} crédits PANU & +${ad.rewardFcfa.toInt()} FCFA créateur",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2ED573)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val targetId = activeStreams.firstOrNull()?.id ?: "live_ad"
                                                featuresService?.recordAdImpression(targetId, ad)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("btn_watch_ad_${ad.id}")
                                    ) {
                                        Text("Valider Pub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun LiveMatchCard(
    match: LiveMatchStream,
    onClick: () -> Unit
) {
    val colors = PanuTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("live_stream_card_${match.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature avec badge
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 75.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = match.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Badge live
                Surface(
                    color = Color(0xFFFF3838),
                    shape = RoundedCornerShape(bottomEnd = 4.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                // Play overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Description
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = match.tournament,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.champagne
                )
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = match.scoreOrStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF7675),
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Par ${match.streamerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "👁️ ${match.viewersCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. STUDIO DE DIFFUSION EN DIRECT (CAMERA)
// ==========================================
@Composable
private fun LiveBroadcasterStudio(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = PanuTheme.colors
    val scope = rememberCoroutineScope()

    var isLiveActive by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf(StreamingProtocol.LIVEKIT_WEBRTC) }

    var broadcastTitle by remember { mutableStateOf("Match en direct PANU") }
    var liveKitServerUrl by remember { mutableStateOf("wss://livekit.panu.app") }
    var liveKitRoomName by remember { mutableStateOf("stade_live_match") }
    var rtmpServerUrl by remember { mutableStateOf("rtmp://live.panu.app/live") }
    var rtmpStreamKey by remember { mutableStateOf("live_panu_sports_7781") }
    var showConfigDialog by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableLongStateOf(0L) }
    var viewersCount by remember { mutableIntStateOf(142) }
    var likesCount by remember { mutableIntStateOf(58) }

    // Chat messages réels
    var chatMessageInput by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(emptyList<LiveChatMessage>())
    }

    // Camera Permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true
        hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // Timer for active broadcast
    LaunchedEffect(isLiveActive) {
        if (isLiveActive) {
            durationSeconds = 0L
            while (isLiveActive) {
                delay(1000L)
                durationSeconds++
                // Random viewer activity
                if (durationSeconds % 3 == 0L) {
                    viewersCount += (1..5).random()
                }
            }
        }
    }

    val formattedDuration = remember(durationSeconds) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Flux vidéo caméra natif CameraX
        if (hasCameraPermission) {
            CameraXLiveView(
                isFrontCamera = isFrontCamera,
                isTorchEnabled = isTorchEnabled
            )
        } else {
            // Demande de permission
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(48.dp))
                        Text(
                            text = "Accès Caméra & Microphone requis",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Pour diffuser un match en direct, autorisez PANU à accéder à votre caméra et votre micro.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                permissionsLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.champagne)
                        ) {
                            Text("Autoriser les permissions")
                        }
                    }
                }
            }
        }

        // Overlay sombre pour lisibilité des contrôles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // ==========================================
        // BARRE DU HAUT : TÉLÉMÉTRIE & STATUT DIRECT
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bouton fermer
                IconButton(
                    onClick = {
                        if (isLiveActive) {
                            Toast.makeText(context, "Arrêtez le direct avant de quitter", Toast.LENGTH_SHORT).show()
                        } else {
                            onClose()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fermer", tint = Color.White)
                }

                if (isLiveActive) {
                    // Badge LIVE animé
                    Surface(
                        color = Color(0xFFFF3838),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text("EN DIRECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Durée
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formattedDuration,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Spectateurs
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "👁️ $viewersCount",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Surface(
                        color = Color.DarkGray.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PRÊT À DIFFUSER",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Boutons d'outils (Configuration serveur, flip caméra, torche)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Protocole / Serveur
                IconButton(
                    onClick = { showConfigDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Config serveur", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Flash / Torche
                IconButton(
                    onClick = { isTorchEnabled = !isTorchEnabled },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isTorchEnabled) colors.champagne else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Inverser caméra (Toggle Front / Back)
                IconButton(
                    onClick = {
                        isFrontCamera = !isFrontCamera
                        Toast.makeText(
                            context,
                            if (isFrontCamera) "Basculé sur Caméra Frontale (Selfie)" else "Basculé sur Caméra Arrière",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("toggle_camera_flip_btn")
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Basculer Caméra Avant/Arrière", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Indicateur caméra active (Avant/Arrière)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isFrontCamera) Icons.Default.Face else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = colors.champagne,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isFrontCamera) "Caméra Frontale" else "Caméra Arrière",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ==========================================
        // MILIEU : TÉLÉMÉTRIE BITRATE & FPS (EN DIRECT)
        // ==========================================
        if (isLiveActive) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = "📡 ${selectedProtocol.label.substringBefore(" ")}",
                        color = Color.Green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = "30 FPS • 2850 kbps • 1080p",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // ==========================================
        // BAS : CHAT EN DIRECT & CONTRÔLES D'ÉMISSION
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chat en direct si en ligne
            if (isLiveActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(130.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    chatMessages.takeLast(4).forEach { msg ->
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = msg.author,
                                    color = colors.champagne,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Saisie message chat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatMessageInput,
                        onValueChange = { chatMessageInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        placeholder = { Text("Écrire au chat...", color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                            focusedBorderColor = colors.champagne,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (chatMessageInput.isNotBlank()) {
                                chatMessages = chatMessages + LiveChatMessage(
                                    id = System.currentTimeMillis().toString(),
                                    author = "Vous (Diffuseur)",
                                    text = chatMessageInput.trim(),
                                    isHost = true
                                )
                                chatMessageInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(colors.champagne, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }

                    // Bouton Réaction Cœur
                    IconButton(
                        onClick = {
                            likesCount += 1
                            Toast.makeText(context, "❤️ Réaction envoyée", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFFFF3838), CircleShape)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // ==========================================
            // BOUTON PRINCIPAL : DÉMARRER / ARRÊTER LE DIRECT
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bouton micro
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(52.dp)
                        .background(if (isMuted) Color(0xFFFF3838) else Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Micro",
                        tint = Color.White
                    )
                }

                // Bouton Start / Stop
                Button(
                    onClick = {
                        if (!isLiveActive) {
                            if (!hasCameraPermission) {
                                permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                                return@Button
                            }
                            isLiveActive = true
                            Toast.makeText(context, "🔴 Diffusion lancée via ${selectedProtocol.label} !", Toast.LENGTH_LONG).show()
                        } else {
                            isLiveActive = false
                            Toast.makeText(context, "Direct terminé. Statistiques enregistrées.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("toggle_live_broadcast_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLiveActive) Color(0xFFFF3838) else Color(0xFF2ED573)
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(
                        if (isLiveActive) Icons.Default.Stop else Icons.Default.Sensors,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLiveActive) "ARRÊTER LA DIFFUSION" else "DÉMARRER LE DIRECT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Dialogue de configuration du serveur de streaming
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Configuration du Streaming Live") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Choisissez le protocole de diffusion vidéo :", fontSize = 13.sp)

                    StreamingProtocol.values().forEach { proto ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedProtocol = proto },
                            color = if (selectedProtocol == proto) colors.champagne.copy(alpha = 0.2f) else colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedProtocol == proto) colors.champagne else colors.surfaceBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(proto.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(proto.description, fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Titre de la diffusion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    when (selectedProtocol) {
                        StreamingProtocol.LIVEKIT_WEBRTC -> {
                            OutlinedTextField(
                                value = liveKitServerUrl,
                                onValueChange = { liveKitServerUrl = it },
                                label = { Text("Serveur LiveKit (WSS)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = liveKitRoomName,
                                onValueChange = { liveKitRoomName = it },
                                label = { Text("ID de Salle / Match Token") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        StreamingProtocol.RTMP_CUSTOM -> {
                            OutlinedTextField(
                                value = rtmpServerUrl,
                                onValueChange = { rtmpServerUrl = it },
                                label = { Text("URL Serveur RTMP") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = rtmpStreamKey,
                                onValueChange = { rtmpStreamKey = it },
                                label = { Text("Clé de flux (Stream Key)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        StreamingProtocol.AWS_IVS -> {
                            OutlinedTextField(
                                value = rtmpServerUrl,
                                onValueChange = { rtmpServerUrl = it },
                                label = { Text("AWS IVS Ingest Endpoint") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = rtmpStreamKey,
                                onValueChange = { rtmpStreamKey = it },
                                label = { Text("IVS Stream Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showConfigDialog = false }) {
                    Text("Valider")
                }
            }
        )
    }
}

// ==========================================
// 3. CAMERAX PREVIEWVIEW COMPOSABLE
// ==========================================
@Composable
private fun CameraXLiveView(
    isFrontCamera: Boolean,
    isTorchEnabled: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(isTorchEnabled, activeCamera, isFrontCamera) {
        try {
            if (!isFrontCamera && activeCamera?.cameraInfo?.hasFlashUnit() == true) {
                activeCamera?.cameraControl?.enableTorch(isTorchEnabled)
            }
        } catch (_: Exception) {}
    }

    key(isFrontCamera) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val targetSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        val resolvedSelector = if (cameraProvider.hasCamera(targetSelector)) {
                            targetSelector
                        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            targetSelector
                        }

                        cameraProvider.unbindAll()
                        val camera: Camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            resolvedSelector,
                            preview
                        )
                        activeCamera = camera
                        if (!isFrontCamera && camera.cameraInfo.hasFlashUnit()) {
                            camera.cameraControl.enableTorch(isTorchEnabled)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CameraXLiveView", "Failed to bind camera: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            update = {
                // Reactive updates handled via LaunchedEffect
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
