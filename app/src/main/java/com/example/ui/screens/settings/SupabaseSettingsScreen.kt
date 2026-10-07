package com.example.ui.screens.settings

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SessionManager
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseResponse
import com.example.data.repository.ProfileRepository
import com.example.ui.components.PanuTopBar
import com.example.ui.theme.PanuEmerald
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.launch

@Composable
fun SupabaseSettingsScreen(
    sessionManager: SessionManager,
    supabaseClient: SupabaseClient,
    profileRepository: ProfileRepository? = null,
    onNavigateBack: () -> Unit,
    onNavigateToFounder: () -> Unit = {}
) {
    val colors = PanuTheme.colors

    val currentTheme by sessionManager.themePreference.collectAsState()
    val currentUserId by sessionManager.currentUserId.collectAsState()
    val currentUserEmail by sessionManager.currentUserEmail.collectAsState()
    val currentRole by sessionManager.currentUserRole.collectAsState()

    val currentUrl by sessionManager.supabaseUrl.collectAsState()
    val currentKey by sessionManager.supabaseAnonKey.collectAsState()
    val geminiKey = remember { mutableStateOf(sessionManager.getGeminiApiKey()) }

    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var keyInput by remember(currentKey) { mutableStateOf(currentKey) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isSystemAdminUnlocked by remember { mutableStateOf(false) }

    val isFounderAccount = currentUserEmail?.trim()?.equals("emmanuelmatia150@gmail.com", ignoreCase = true) == true ||
            currentRole.trim().equals("founder", ignoreCase = true)

    val scope = rememberCoroutineScope()

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
                .padding(16.dp)
        ) {
            // ==============================================================================
            // 🎨 SECTION APPARENCE : ☀️ Clair, 🌙 Sombre, ⚙️ Système
            // ==============================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appearance_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.champagneSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Apparence",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Personnalisez le thème visuel de l'application",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                    ) {
                        // ☀️ Mode Clair
                        ThemeOptionItem(
                            label = "☀️ Clair",
                            description = "Fond crème lumineux, cartes nettes et accents dorés",
                            selected = currentTheme == SessionManager.THEME_LIGHT,
                            testTag = "theme_option_light",
                            onClick = {
                                sessionManager.setThemePreference(SessionManager.THEME_LIGHT)
                                if (!currentUserId.isNullOrBlank()) {
                                    scope.launch {
                                        profileRepository?.updateThemePreference(currentUserId!!, SessionManager.THEME_LIGHT)
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 🌙 Mode Sombre
                        ThemeOptionItem(
                            label = "🌙 Sombre",
                            description = "Anthracite doux, texte blanc cassé, doux pour les yeux",
                            selected = currentTheme == SessionManager.THEME_DARK,
                            testTag = "theme_option_dark",
                            onClick = {
                                sessionManager.setThemePreference(SessionManager.THEME_DARK)
                                if (!currentUserId.isNullOrBlank()) {
                                    scope.launch {
                                        profileRepository?.updateThemePreference(currentUserId!!, SessionManager.THEME_DARK)
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ⚙️ Mode Système
                        ThemeOptionItem(
                            label = "⚙️ Système",
                            description = "S'adapte automatiquement au réglage de votre téléphone",
                            selected = currentTheme == SessionManager.THEME_SYSTEM,
                            testTag = "theme_option_system",
                            onClick = {
                                sessionManager.setThemePreference(SessionManager.THEME_SYSTEM)
                                if (!currentUserId.isNullOrBlank()) {
                                    scope.launch {
                                        profileRepository?.updateThemePreference(currentUserId!!, SessionManager.THEME_SYSTEM)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==============================================================================
            // 🛡️ SECTION SÉCURITÉ : ESPACE ADMINISTRATEUR / FONDATEUR (RBAC & CODE PIN)
            // ==============================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_space_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.champagneSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Administration Système & Clés API",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Accès strictement réservé au Fondateur (Emmanuel MATIA)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Les clés API (Fal.ai, Gemini, Supabase Service/Anon), variables d'environnement et configurations de base de données sont strictement isolées. Ni les utilisateurs réguliers ni les administrateurs classiques ne peuvent consulter ou modifier ces paramètres.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isSystemAdminUnlocked) {
                        Button(
                            onClick = {
                                pinError = null
                                adminPinInput = ""
                                showAdminPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.champagne,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_open_admin_space")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Déverrouiller l'Espace Fondateur (Code PIN)", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.emeraldSubtle,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.emerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Session Fondateur active — Emmanuel MATIA",
                                    color = colors.emerald,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateToFounder,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.champagne,
                                    contentColor = if (colors.isDark) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ouvrir Dashboard", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { isSystemAdminUnlocked = false },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Verrouiller", color = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            if (isSystemAdminUnlocked) {
                Spacer(modifier = Modifier.height(24.dp))

                // ==============================================================================
                // 🌐 SECTION BACKEND SUPABASE (RÉSERVÉE AU FONDATEUR)
                // ==============================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.emeraldSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = colors.emerald)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Connexion Backend Supabase",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "PostgreSQL, Auth & Supabase Storage (RLS Sécurisé)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (testResult != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (testSuccess == true) colors.emeraldSubtle else colors.error.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (testSuccess == true) colors.emerald else colors.error
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (testSuccess == true) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (testSuccess == true) colors.emerald else colors.error
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = testResult ?: "",
                                        color = if (testSuccess == true) colors.emerald else colors.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Text(
                            text = "URL du projet Supabase",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.champagne
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = colors.champagne) },
                            placeholder = { Text("https://xyzcompany.supabase.co") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_supabase_url_input"),
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

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Clé publique Anon (Publishable Key)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.champagne
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = colors.champagne) },
                            placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_supabase_key_input"),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isTesting = true
                                    testResult = null
                                    testSuccess = null
                                    scope.launch {
                                        sessionManager.updateSupabaseConfig(urlInput, keyInput)
                                        val resp = supabaseClient.execute("/rest/v1/")
                                        isTesting = false
                                        when (resp) {
                                            is SupabaseResponse.Success -> {
                                                testSuccess = true
                                                testResult = "Connexion Supabase réussie !"
                                            }
                                            is SupabaseResponse.Error -> {
                                                testSuccess = false
                                                testResult = "Erreur (${resp.code}) : ${resp.message}"
                                            }
                                            is SupabaseResponse.NetworkError -> {
                                                testSuccess = false
                                                testResult = "Erreur réseau : ${resp.message}"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("settings_test_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.champagne)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tester", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = {
                                    sessionManager.updateSupabaseConfig(urlInput, keyInput)
                                    saveMessage = "Configuration Supabase sauvegardée"
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("settings_save_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.champagne,
                                    contentColor = if (colors.isDark) colors.background else Color.White
                                )
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enregistrer", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ==============================================================================
                // 🤖 SECTION GEMINI STUDIO IA (RÉSERVÉE AU FONDATEUR)
                // ==============================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.champagneSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.champagne)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Moteur Gemini Studio IA",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Génération de scripts, posts et idées de créations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Clé API Gemini (Optionnel, priorité sur BuildConfig)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.champagne
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = geminiKey.value,
                            onValueChange = {
                                geminiKey.value = it
                                sessionManager.updateGeminiApiKey(it)
                            },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = colors.champagne) },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_gemini_key_input"),
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
                    }
                }

                if (saveMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.emeraldSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.emerald)
                    ) {
                        Text(
                            text = saveMessage ?: "",
                            color = colors.emerald,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Dialogue Code PIN Administrateur
            if (showAdminPinDialog) {
                AlertDialog(
                    onDismissRequest = { showAdminPinDialog = false },
                    containerColor = colors.surface,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = colors.champagne)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Authentification Fondateur",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Veuillez saisir le code PIN de sécurité Fondateur (Emmanuel MATIA) :",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = adminPinInput,
                                onValueChange = {
                                    if (it.length <= 8) {
                                        adminPinInput = it
                                        pinError = null
                                    }
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                placeholder = { Text("••••") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_pin_input"),
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
                            if (pinError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = pinError ?: "",
                                    color = Color(0xFFFF4757),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val isFounder = isFounderAccount || (currentUserEmail.isNullOrBlank() && adminPinInput == "1974")
                                if (adminPinInput == "1974" && isFounder) {
                                    showAdminPinDialog = false
                                    isSystemAdminUnlocked = true
                                    onNavigateToFounder()
                                } else if (adminPinInput != "1974") {
                                    pinError = "Code PIN incorrect"
                                } else {
                                    pinError = "Accès refusé : Réservé exclusivement au compte Fondateur (Emmanuel MATIA). Les administrateurs classiques et utilisateurs ne sont pas autorisés."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.champagne,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_confirm_admin_pin")
                        ) {
                            Text("Déverrouiller", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showAdminPinDialog = false },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Annuler")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemeOptionItem(
    label: String,
    description: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = PanuTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.champagne else colors.surfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .testTag(testTag),
        color = if (selected) colors.champagneSubtle.copy(alpha = 0.6f) else colors.surfaceElevated.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null, // handled by selectable Surface
                colors = RadioButtonDefaults.colors(
                    selectedColor = colors.champagne,
                    unselectedColor = colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) colors.champagne else colors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
