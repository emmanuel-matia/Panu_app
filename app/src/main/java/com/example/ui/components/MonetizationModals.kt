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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CreditPackOffer
import com.example.data.model.PaymentProvider
import com.example.data.model.VisibilityBoostPlan
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 1. SYSTÈME DE PUBLICITÉS VIDÉO DE 5 SECONDES (GOOGLE ADMOB / AUDIENCE NETWORK)
 * S'affiche lorsque l'utilisateur clique sur "Générer la vidéo" ou "Télécharger la création".
 */
@Composable
fun FiveSecondVideoAdDialog(
    isOpen: Boolean,
    triggerActionLabel: String, // ex: "Générer la vidéo IA" ou "Télécharger la création"
    featuresService: SupabasePanuFeaturesService?,
    onAdCompleted: () -> Unit,
    onOpenCreditsStore: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val scope = rememberCoroutineScope()
    val ads = featuresService?.monetizedAdSequences ?: emptyList()
    val currentAd = remember { ads.firstOrNull() }

    var remainingSeconds by remember(isOpen) { mutableIntStateOf(5) }
    var adFinished by remember(isOpen) { mutableStateOf(false) }

    LaunchedEffect(isOpen) {
        remainingSeconds = 5
        adFinished = false
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }
        adFinished = true
        if (currentAd != null) {
            featuresService?.recordAdImpression("studio_action_5s_ad", currentAd)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (adFinished) onDismiss()
        },
        containerColor = colors.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📺 Publicité Vidéo 5s (AdMob / Audience Network)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = colors.champagne
                    )
                    Text(
                        text = "Action : $triggerActionLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                Surface(
                    color = if (adFinished) Color(0xFF2ED573) else Color(0xFFFF4757),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (adFinished) "PRÊT ✓" else "${remainingSeconds}s",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, colors.champagne.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    if (currentAd != null) {
                        AsyncImage(
                            model = currentAd.bannerUrl,
                            contentDescription = currentAd.campaignTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = colors.champagne,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = currentAd?.adNetwork ?: "Google AdMob • Audience Network",
                            color = colors.champagne,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentAd?.campaignTitle ?: "Sponsor Officiel PANU Studio 8K",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Recette reversée au Fonds pour les Créateurs PANU (+${currentAd?.rewardFcfa?.toInt() ?: 65} FCFA)",
                            color = Color(0xFF2ED573),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (5 - remainingSeconds) / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colors.champagne,
                    trackColor = colors.surfaceElevated
                )

                Surface(
                    color = colors.champagneSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onOpenCreditsStore()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Astuce Pro : Passez au Pack Crédits (Mobile Money / Carte) pour générer en HD 8K sans filigrane et sans publicité !",
                            fontSize = 11.sp,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onAdCompleted()
                },
                enabled = adFinished,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (adFinished) colors.champagne else colors.surfaceElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_confirm_after_5s_ad")
            ) {
                Text(
                    text = if (adFinished) "Continuer : $triggerActionLabel →" else "Veuillez patienter (${remainingSeconds}s)...",
                    fontWeight = FontWeight.Bold,
                    color = if (adFinished) Color.Black else colors.textSecondary
                )
            }
        }
    )
}

/**
 * 2. BOUTIQUE DE PACKS DE CRÉDITS (MODÈLE 60 CRÉDITS + MOBILE MONEY M-PESA, ORANGE MONEY, AIRTEL MONEY & CARTE)
 */
@Composable
fun MobileMoneyCreditsStoreDialog(
    isOpen: Boolean,
    featuresService: SupabasePanuFeaturesService?,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val creditsBalance = featuresService?.panuCreditsBalance?.collectAsState()?.value ?: 60
    val hasNoWatermarkHd = featuresService?.hasNoWatermarkHdPass?.collectAsState()?.value ?: false
    val packs = featuresService?.creditPacksCatalog ?: emptyList()

    var selectedPack by remember { mutableStateOf<CreditPackOffer?>(packs.getOrNull(1) ?: packs.firstOrNull()) }
    var selectedProvider by remember { mutableStateOf(PaymentProvider.ORANGE_MONEY) }
    var phoneOrCardInput by remember { mutableStateOf("+225 07 00 00 00 00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Column {
                Text(
                    text = "🪙 Crédits PANU & Mode HD Sans Filigrane",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = colors.textPrimary
                )
                Text(
                    text = "Solde actuel : $creditsBalance Crédits (60 Crédits offerts à l'inscription)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.champagne
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasNoWatermarkHd) {
                    Surface(
                        color = Color(0xFF2ED573).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2ED573))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2ED573))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pass HD 8K Sans Filigrane & Rendu Rapide ACTIF sur votre compte !",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "1. Choisissez votre Pack de Crédits :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.champagne
                )

                packs.forEach { pack ->
                    val isSelected = selectedPack?.id == pack.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.champagne else colors.surfaceBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPack = pack }
                            .testTag("credit_pack_${pack.id}"),
                        color = if (isSelected) colors.champagneSubtle else colors.surfaceElevated
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pack.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${pack.priceFcfa} FCFA",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = colors.champagne
                                )
                            }
                            Text(
                                text = pack.badge,
                                fontSize = 10.sp,
                                color = colors.champagne,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            pack.perks.forEach { perk ->
                                Text(
                                    text = "✓ $perk",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "2. Moyen de paiement (Mobile Money ou Carte) :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.champagne
                )

                PaymentProvider.values().forEach { provider ->
                    val isSelected = selectedProvider == provider
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.champagne else colors.surfaceBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedProvider = provider },
                        color = if (isSelected) colors.champagneSubtle else colors.surfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = provider.badgeEmoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.labelFr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = provider.feeNote,
                                    fontSize = 10.sp,
                                    color = colors.textSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phoneOrCardInput,
                    onValueChange = { phoneOrCardInput = it },
                    label = {
                        Text(
                            if (selectedProvider == PaymentProvider.BANK_CARD)
                                "Numéro de Carte Visa / Mastercard"
                            else
                                "Numéro ${selectedProvider.labelFr}"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pack = selectedPack ?: return@Button
                    scope.launch {
                        featuresService?.purchaseCreditPack(
                            pack = pack,
                            provider = selectedProvider,
                            phoneOrCardNumber = phoneOrCardInput
                        )
                        Toast.makeText(
                            context,
                            "✅ +${pack.creditsAmount} Crédits activés via ${selectedProvider.labelFr} (Mode HD Sans Filigrane débloqué) !",
                            Toast.LENGTH_LONG
                        ).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_confirm_credit_purchase")
            ) {
                Text(
                    text = "Payer ${selectedPack?.priceFcfa ?: 1500} FCFA (${selectedProvider.labelFr})",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = colors.textSecondary)
            }
        }
    )
}

/**
 * 3. OPTION "BOOSTER DE VISIBILITÉ" POUR CRÉATEURS (DÉCOUVRIR : +VUES & +FOLLOWERS)
 */
@Composable
fun VisibilityBoosterDialog(
    isOpen: Boolean,
    videoId: String,
    videoTitle: String,
    featuresService: SupabasePanuFeaturesService?,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val plans = featuresService?.visibilityBoostPlans ?: emptyList()
    val creditsBalance = featuresService?.panuCreditsBalance?.collectAsState()?.value ?: 60

    var selectedPlan by remember { mutableStateOf<VisibilityBoostPlan?>(plans.firstOrNull()) }
    var useMobileMoneyDirect by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf(PaymentProvider.ORANGE_MONEY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = colors.champagne)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "🚀 Booster de Visibilité Créateur",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Propulsez « $videoTitle » dans l'onglet Découvrir",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.champagne
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Obtenez plus de vues, plus de followers et augmentez vos commissions du Fonds pour les Créateurs :",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                plans.forEach { plan ->
                    val isSelected = selectedPlan?.id == plan.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.champagne else colors.surfaceBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPlan = plan }
                            .testTag("boost_plan_${plan.id}"),
                        color = if (isSelected) colors.champagneSubtle else colors.surfaceElevated
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = plan.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${plan.creditsCost} Crédits / ${plan.priceFcfa} FCFA",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = colors.champagne
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📈 +${plan.estimatedExtraViews} vues garanties • 👥 +${plan.estimatedNewFollowers} followers",
                                fontSize = 12.sp,
                                color = Color(0xFF2ED573),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⏱️ ${plan.durationLabel}",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { useMobileMoneyDirect = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!useMobileMoneyDirect) colors.champagneSubtle else Color.Transparent
                        )
                    ) {
                        Text("Solde ($creditsBalance cr.)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { useMobileMoneyDirect = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (useMobileMoneyDirect) colors.champagneSubtle else Color.Transparent
                        )
                    ) {
                        Text("Mobile Money / Carte", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (useMobileMoneyDirect) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentProvider.values().forEach { prov ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (selectedProvider == prov) colors.champagne else colors.surfaceBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedProvider = prov },
                                color = if (selectedProvider == prov) colors.champagneSubtle else colors.surfaceElevated
                            ) {
                                Text(
                                    text = "${prov.badgeEmoji} ${prov.labelFr.substringBefore(" ")}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val plan = selectedPlan ?: return@Button
                    scope.launch {
                        val res = featuresService?.boostCreatorVideo(
                            videoId = videoId,
                            videoTitle = videoTitle,
                            plan = plan,
                            payWithMobileMoney = if (useMobileMoneyDirect) selectedProvider else null
                        )
                        if (res?.isSuccess == true) {
                            Toast.makeText(
                                context,
                                "🚀 Vidéo boostée en tête de Découvrir (+${plan.estimatedExtraViews} vues) !",
                                Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        } else {
                            Toast.makeText(
                                context,
                                res?.exceptionOrNull()?.localizedMessage ?: "Crédits insuffisants",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_confirm_boost_video")
            ) {
                Text(
                    text = if (useMobileMoneyDirect)
                        "Booster avec ${selectedProvider.labelFr} (${selectedPlan?.priceFcfa ?: 500} FCFA)"
                    else
                        "Booster maintenant (${selectedPlan?.creditsCost ?: 30} Crédits)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = colors.textSecondary)
            }
        }
    )
}

/**
 * 4. FONDS POUR LES CRÉATEURS & VIREMENT D'ARGENT RÉEL (MOBILE MONEY / CARTE)
 */
@Composable
fun CreatorFundPayoutDialog(
    isOpen: Boolean,
    featuresService: SupabasePanuFeaturesService?,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats = featuresService?.creatorEarnings?.collectAsState()?.value

    var selectedProvider by remember { mutableStateOf(PaymentProvider.ORANGE_MONEY) }
    var accountNumber by remember { mutableStateOf("+225 07 00 00 00 00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = colors.champagne)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "💰 Fonds pour les Créateurs PANU",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Partage des recettes publicitaires (Commission ${stats?.creatorFundCommissionPercent ?: 55}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.champagne
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = colors.champagneSubtle,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Solde Disponible à Retirer en Argent Réel :",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${stats?.totalAvailableFcfa?.toInt() ?: 95100} FCFA",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = colors.champagne
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Vues cumulées : ${stats?.totalViews ?: 18450} vues (+${stats?.boostedViewsGained ?: 12500} vues boostées)\n" +
                                    "• Commission Fonds Créateurs (55% Pubs AdMob/Shorts) : +${stats?.creatorFundShareFcfa?.toInt() ?: 28400} FCFA\n" +
                                    "• Bonus Vues & Partages Viraux : +${stats?.viewsAndSharesBonusFcfa?.toInt() ?: 45200} FCFA",
                            fontSize = 11.sp,
                            color = colors.textPrimary
                        )
                    }
                }

                Text(
                    text = "Choisissez votre compte de réception :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )

                PaymentProvider.values().forEach { prov ->
                    val isSelected = selectedProvider == prov
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (isSelected) colors.champagne else colors.surfaceBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedProvider = prov },
                        color = if (isSelected) colors.champagneSubtle else colors.surfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prov.badgeEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = prov.labelFr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.champagne, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Numéro ${selectedProvider.labelFr} ou IBAN/Carte") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = stats?.totalAvailableFcfa ?: 0.0
                    if (amount <= 0) {
                        Toast.makeText(context, "Aucun solde disponible à retirer.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        featuresService?.withdrawCreatorEarningsToRealMoney(
                            amountFcfa = amount,
                            provider = selectedProvider,
                            accountIdentifier = accountNumber
                        )
                        Toast.makeText(
                            context,
                            "💸 Virement de ${amount.toInt()} FCFA envoyé vers ${selectedProvider.labelFr} !",
                            Toast.LENGTH_LONG
                        ).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ED573)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_withdraw_creator_fund")
            ) {
                Text(
                    text = "Retirer ${stats?.totalAvailableFcfa?.toInt() ?: 95100} FCFA vers ${selectedProvider.labelFr}",
                    color = Color.Black,
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = colors.textSecondary)
            }
        }
    )
}

// =============================================================================
// 5. PUBLICITÉ IN-VIDEO DYNAMIQUE (FORMAT SHORTS / REELS — ADMOB & AUDIENCE NETWORK)
// Insérée toutes les 3 vidéos lors du défilement dans l'onglet Découvrir
// =============================================================================
@Composable
fun TikTokInVideoAdSlide(
    adCampaignTitle: String,
    advertiserName: String,
    bannerUrl: String,
    rewardFcfa: Int,
    onClaimAdReward: () -> Unit,
    onOpenCreatorFund: () -> Unit
) {
    val colors = PanuTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .testTag("tiktok_in_video_ad_slide")
    ) {
        AsyncImage(
            model = bannerUrl,
            contentDescription = adCampaignTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Badge supérieur : Publicité In-Video Sponsorisée (Shorts / Reels)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = colors.champagne,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SPONSORISÉ • PUBLICITÉ IN-VIDEO",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Surface(
                color = Color(0xFF2ED573).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2ED573))
            ) {
                Text(
                    text = "+$rewardFcfa FCFA Fonds Créateurs",
                    color = Color(0xFF2ED573),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        // Contenu central et inférieur de l'interstitiel Shorts/Reels
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = advertiserName.uppercase(),
                color = colors.champagne,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
            Text(
                text = adCampaignTitle,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                lineHeight = 26.sp
            )
            Text(
                text = "Interstitiel dynamique inséré toutes les 3 vidéos dans Découvrir. 55% des recettes publicitaires (AdMob / Audience Network) sont reversées au Fonds pour les Créateurs PANU.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onClaimAdReward,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_claim_in_video_ad_reward"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Découvrir l'Offre (+$rewardFcfa FCFA)",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenCreatorFund,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne)
                ) {
                    Text(
                        text = "💰 Fonds (55%)",
                        color = colors.champagne,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "⬆️ Glissez vers le haut pour continuer à regarder les vidéos virales",
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
}
