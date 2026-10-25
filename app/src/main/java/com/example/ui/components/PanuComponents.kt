package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserRole
import com.example.data.remote.SupabasePanuFeaturesService
import com.example.ui.navigation.PanuScreen
import com.example.ui.theme.PanuEmerald
import com.example.ui.theme.PanuTheme

/**
 * BARRE SUPÉRIEURE ÉPURÉE PANU (DESIGN STRICT)
 * - Logo à gauche
 * - Strictement 2 icônes à droite (Recherche et Menu)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanuTopBar(
    onSearchClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    onMessagesClick: (() -> Unit)? = null,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    featuresService: SupabasePanuFeaturesService? = null,
    unreadMessagesCount: Int = 0
) {
    val colors = PanuTheme.colors

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo stylisé PANU
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.champagne),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = colors.background
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PANU",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = colors.textPrimary
                    )
                }
            }
        },
        actions = {
            // BOUTON MESSAGERIE & NOTIFICATIONS (Remplace le compteur "60" et le petit bouton "+")
            IconButton(
                onClick = { onMessagesClick?.invoke() },
                modifier = Modifier.testTag("header_messages_btn")
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Messages et notifications",
                        tint = colors.champagne,
                        modifier = Modifier.size(24.dp)
                    )
                    if (unreadMessagesCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2E4C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadMessagesCount > 9) "9+" else "$unreadMessagesCount",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // RECHERCHE
            if (onSearchClick != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Recherche",
                        tint = colors.textPrimary
                    )
                }
            }
            // MENU HAMBURGER
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu Principal",
                        tint = colors.textPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            titleContentColor = colors.textPrimary
        )
    )
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun PanuBottomNav(
    currentRoute: String?,
    isFounder: Boolean = false,
    onCreateClick: (() -> Unit)? = null,
    onNavigate: (String) -> Unit
) {
    val colors = PanuTheme.colors

    NavigationBar(
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(width = 1.dp, color = colors.surfaceBorder)
            .testTag("panu_bottom_nav")
    ) {
        // 1. Accueil
        val isHomeSelected = currentRoute == PanuScreen.Home.route
        NavigationBarItem(
            selected = isHomeSelected,
            onClick = { onNavigate(PanuScreen.Home.route) },
            icon = {
                Icon(
                    imageVector = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Accueil",
                    tint = if (isHomeSelected) colors.champagne else colors.textSecondary
                )
            },
            label = {
                Text(
                    text = "Accueil",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isHomeSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isHomeSelected) colors.champagne else colors.textSecondary,
                    maxLines = 1
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = colors.champagneSubtle,
                selectedIconColor = colors.champagne,
                selectedTextColor = colors.champagne,
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary
            ),
            modifier = Modifier.testTag("nav_home")
        )

        // 2. Studio IA / Templates
        val isStudioSelected = currentRoute == PanuScreen.Studio.route
        NavigationBarItem(
            selected = isStudioSelected,
            onClick = { onNavigate(PanuScreen.Studio.route) },
            icon = {
                Icon(
                    imageVector = if (isStudioSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = "Studio IA",
                    tint = if (isStudioSelected) colors.champagne else colors.textSecondary
                )
            },
            label = {
                Text(
                    text = "Studio IA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isStudioSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isStudioSelected) colors.champagne else colors.textSecondary,
                    maxLines = 1
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = colors.champagneSubtle,
                selectedIconColor = colors.champagne,
                selectedTextColor = colors.champagne,
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary
            ),
            modifier = Modifier.testTag("nav_studio")
        )

        // 3. GRAND BOUTON CENTRAL "+" (CRÉER)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = {
                    if (onCreateClick != null) {
                        onCreateClick()
                    } else {
                        onNavigate(PanuScreen.Create.route)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = colors.champagne,
                border = BorderStroke(2.dp, Color(0xFF202330)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(width = 44.dp, height = 34.dp)
                    .testTag("nav_create_grand_btn")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Créer",
                        tint = if (colors.isDark) Color.Black else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. Créations
        val isCreationsSelected = currentRoute == PanuScreen.Creations.route || currentRoute == PanuScreen.Activity.route
        NavigationBarItem(
            selected = isCreationsSelected,
            onClick = { onNavigate(PanuScreen.Creations.route) },
            icon = {
                Icon(
                    imageVector = if (isCreationsSelected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                    contentDescription = "Créations",
                    tint = if (isCreationsSelected) colors.champagne else colors.textSecondary
                )
            },
            label = {
                Text(
                    text = "Créations",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isCreationsSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isCreationsSelected) colors.champagne else colors.textSecondary,
                    maxLines = 1
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = colors.champagneSubtle,
                selectedIconColor = colors.champagne,
                selectedTextColor = colors.champagne,
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary
            ),
            modifier = Modifier.testTag("nav_creations")
        )

        // 5. Compte
        val isProfileSelected = currentRoute == PanuScreen.Profile.route
        NavigationBarItem(
            selected = isProfileSelected,
            onClick = { onNavigate(PanuScreen.Profile.route) },
            icon = {
                Icon(
                    imageVector = if (isProfileSelected) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Compte",
                    tint = if (isProfileSelected) colors.champagne else colors.textSecondary
                )
            },
            label = {
                Text(
                    text = "Compte",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isProfileSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isProfileSelected) colors.champagne else colors.textSecondary,
                    maxLines = 1
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = colors.champagneSubtle,
                selectedIconColor = colors.champagne,
                selectedTextColor = colors.champagne,
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}

@Composable
fun PanuEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.DynamicFeed,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = PanuTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.champagneSubtle)
                .border(1.dp, colors.surfaceBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.champagne,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.champagne,
                    contentColor = if (colors.isDark) colors.background else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_state_action_btn")
            ) {
                Text(actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PanuAvatar(
    avatarUrl: String?,
    fullName: String? = null,
    name: String? = fullName,
    size: Dp = 48.dp,
    borderWidth: Dp = 1.5.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = PanuTheme.colors
    val displayName = name ?: fullName
    val initial = displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier

    Box(
        modifier = clickableModifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surfaceElevated)
            .border(borderWidth, colors.champagne.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Photo de profil",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.42).sp
                ),
                color = colors.champagne
            )
        }
    }
}

@Composable
fun PanuRoleBadge(roleString: String) {
    PanuRoleBadge(UserRole.fromString(roleString))
}

@Composable
fun PanuRoleBadge(role: UserRole) {
    val colors = PanuTheme.colors
    val (bgColor, textColor, label) = when (role) {
        UserRole.FOUNDER -> Triple(colors.champagneSubtle, colors.champagne, "Fondateur")
        UserRole.ADMIN -> Triple(colors.emeraldSubtle, colors.emerald, "Admin")
        UserRole.BUSINESS -> Triple(colors.champagneSubtle, colors.champagne, "Entreprise")
        UserRole.CREATOR -> Triple(colors.champagneSubtle, colors.champagne, "Créateur")
        UserRole.USER -> Triple(colors.surfaceElevated, colors.textSecondary, "Membre")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
