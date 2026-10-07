package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SocialLinks
import com.example.ui.theme.PanuTheme

/**
 * Composant de présentation des liens vérifiés d'un créateur vers ses chaînes
 * externes officielles (TikTok, YouTube, Facebook, Instagram).
 */
@Composable
fun VerifiedSocialLinksCard(
    socialLinks: SocialLinks?,
    modifier: Modifier = Modifier,
    onEditLinksClick: (() -> Unit)? = null
) {
    val colors = PanuTheme.colors
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2ED573),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chaînes & Comptes Vérifiés",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }

                if (onEditLinksClick != null) {
                    Text(
                        text = "Modifier",
                        color = colors.champagne,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onEditLinksClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val hasAny = socialLinks != null && socialLinks.hasAnyLink()
            if (!hasAny) {
                Text(
                    text = "Aucun compte externe vérifié lié pour le moment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    socialLinks?.tiktok?.takeIf { it.isNotBlank() }?.let { tt ->
                        VerifiedLinkRow(
                            platformIcon = "🎵",
                            platformName = "TikTok",
                            handle = tt,
                            onClick = {
                                val url = if (tt.startsWith("http")) tt else "https://tiktok.com/@${tt.removePrefix("@")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }

                    socialLinks?.youtube?.takeIf { it.isNotBlank() }?.let { yt ->
                        VerifiedLinkRow(
                            platformIcon = "▶️",
                            platformName = "YouTube",
                            handle = yt,
                            onClick = {
                                val url = if (yt.startsWith("http")) yt else "https://youtube.com/@${yt.removePrefix("@")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }

                    socialLinks?.facebook?.takeIf { it.isNotBlank() }?.let { fb ->
                        VerifiedLinkRow(
                            platformIcon = "👥",
                            platformName = "Facebook",
                            handle = fb,
                            onClick = {
                                val url = if (fb.startsWith("http")) fb else "https://facebook.com/${fb.removePrefix("@")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }

                    socialLinks?.instagram?.takeIf { it.isNotBlank() }?.let { ig ->
                        VerifiedLinkRow(
                            platformIcon = "📷",
                            platformName = "Instagram",
                            handle = ig,
                            onClick = {
                                val url = if (ig.startsWith("http")) ig else "https://instagram.com/${ig.removePrefix("@")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }

                    socialLinks?.website?.takeIf { it.isNotBlank() }?.let { web ->
                        VerifiedLinkRow(
                            platformIcon = "🌐",
                            platformName = "Site Officiel",
                            handle = web,
                            onClick = {
                                val url = if (web.startsWith("http")) web else "https://$web"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifiedLinkRow(
    platformIcon: String,
    platformName: String,
    handle: String,
    onClick: () -> Unit
) {
    val colors = PanuTheme.colors

    Surface(
        color = colors.surfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = platformIcon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = platformName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Vérifié",
                            tint = Color(0xFF2ED573),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = handle,
                        fontSize = 11.sp,
                        color = colors.champagne
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
