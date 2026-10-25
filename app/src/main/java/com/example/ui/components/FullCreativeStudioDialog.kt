package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Post
import com.example.data.repository.PostRepository
import com.example.ui.theme.PanuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Interface de création complète PANU :
 * - Accès direct à la caméra (filmer / prendre photos en direct)
 * - Bouton "Importer" pour vidéos, photos ou musiques dans la galerie
 * - Sélection de la durée : 15s, 60s, 3 min
 * - Outils de retouche et miniatures (Filtres, Ratios, Couverture, Audio)
 */
@Composable
fun FullCreativeStudioDialog(
    isOpen: Boolean,
    userId: String,
    authorName: String,
    postRepository: PostRepository?,
    onDismiss: () -> Unit,
    onPostCreated: (Post) -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Mode : Caméra directe vs Média Importé
    var captureMode by remember { mutableStateOf("video") } // "video" ou "photo"
    var isCameraFacingFront by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }

    // Enregistrement caméra simulé / timer
    var isRecording by remember { mutableStateOf(false) }
    var recordedSeconds by remember { mutableIntStateOf(0) }

    // Durée sélectionnée : 15s, 60s, 3 min
    var selectedDuration by remember { mutableStateOf("15s") }
    val durationOptions = listOf("15s", "60s", "3 min")

    // Média importé
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf<String?>("video") }
    var selectedAudioTitle by remember { mutableStateOf("Morceau original - PANU Studio") }

    // Outils de retouche
    var selectedFilter by remember { mutableStateOf("Normal") }
    val filterOptions = listOf("Normal", "Vibrant", "Cinéma", "Rétro", "Noir & Blanc")

    var selectedRatio by remember { mutableStateOf("9:16") }
    val ratioOptions = listOf("9:16", "1:1", "16:9")

    var selectedThumbnailIndex by remember { mutableIntStateOf(0) }
    val thumbnailPlaceholders = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80"
    )

    // Titre et Description
    var videoTitle by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Launcher pour Importer des médias (vidéo, image, son)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            selectedMediaType = "video"
            Toast.makeText(context, "Média sélectionné depuis la galerie !", Toast.LENGTH_SHORT).show()
        }
    }

    // Minuteur d'enregistrement
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val maxSeconds = when (selectedDuration) {
                "15s" -> 15
                "60s" -> 60
                else -> 180
            }
            while (isRecording && recordedSeconds < maxSeconds) {
                delay(1000)
                recordedSeconds++
            }
            if (recordedSeconds >= maxSeconds) {
                isRecording = false
                Toast.makeText(context, "Enregistrement terminé ($selectedDuration)", Toast.LENGTH_SHORT).show()
            }
        } else {
            recordedSeconds = 0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_creative_studio_dialog"),
            color = Color(0xFF0C0E14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // =============================================================
                // 1. EN-TÊTE DU STUDIO DE CRÉATION
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }

                    // Sélection de la musique / son en haut
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E2230),
                        border = BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Bande son : $selectedAudioTitle", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedAudioTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Bouton flash
                    IconButton(onClick = { isFlashOn = !isFlashOn }) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) colors.champagne else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // =============================================================
                // 2. APERÇU CAMÉRA DIRECT OU MÉDIA IMPORTÉ
                // =============================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(
                            when (selectedRatio) {
                                "1:1" -> 1f
                                "16:9" -> 16f / 9f
                                else -> 9f / 14f // Format vertical immersif
                            }
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF151824))
                        .border(1.dp, colors.champagne.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedMediaUri != null) {
                        // Média importé depuis la galerie
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = colors.champagne,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Média Galerie Prêt",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Filtre appliqué : $selectedFilter",
                                color = colors.champagne,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { selectedMediaUri = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                            ) {
                                Text("Changer de média", color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Viseur Caméra Live
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF222838),
                                            when (selectedFilter) {
                                                "Vibrant" -> Color(0xFF2B3A4A)
                                                "Cinéma" -> Color(0xFF1E2B38)
                                                "Rétro" -> Color(0xFF382E20)
                                                "Noir & Blanc" -> Color(0xFF1F1F1F)
                                                else -> Color(0xFF161A24)
                                            }
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (captureMode == "video") Icons.Default.Videocam else Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = if (isRecording) Color.Red else colors.champagne,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isRecording) "🔴 Enregistrement... $recordedSeconds s" else "Viseur Caméra en Direct",
                                    color = if (isRecording) Color.Red else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Objectif : ${if (isCameraFacingFront) "Avant (Selfie)" else "Arrière"}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }

                            // Bouton bascule avant/arrière
                            IconButton(
                                onClick = { isCameraFacingFront = !isCameraFacingFront },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Basculer caméra",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =============================================================
                // 3. SÉLECTION DE LA DURÉE (15s, 60s, 3 min)
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Durée :",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    durationOptions.forEach { dur ->
                        val isSelected = selectedDuration == dur
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colors.champagne else Color(0xFF1E2230),
                            border = BorderStroke(1.dp, if (isSelected) colors.champagne else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { selectedDuration = dur }
                                .testTag("duration_chip_$dur")
                        ) {
                            Text(
                                text = dur,
                                color = if (isSelected) (if (colors.isDark) Color.Black else Color.White) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =============================================================
                // 4. CONTRÔLES PRINCIPAUX : FILMER EN DIRECT & BOUTON IMPORTER
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Photo / Vidéo toggle
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1A1D2B),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                captureMode = if (captureMode == "video") "photo" else "video"
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (captureMode == "video") Icons.Default.CameraAlt else Icons.Default.Videocam,
                                contentDescription = "Mode",
                                tint = colors.champagne,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Bouton Enregistrer / Déclencher (Grand bouton central rouge/champagne)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red.copy(alpha = 0.3f) else colors.champagne.copy(alpha = 0.2f))
                            .clickable {
                                if (captureMode == "photo") {
                                    Toast.makeText(context, "📸 Photo capturée en direct !", Toast.LENGTH_SHORT).show()
                                } else {
                                    isRecording = !isRecording
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isRecording) Color.Red else colors.champagne,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Déclencher",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Bouton "IMPORTER" (Galerie téléphone : Vidéo, Photo, Musique)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            mediaPickerLauncher.launch("video/*,image/*")
                        }.testTag("btn_import_media")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E2230),
                            border = BorderStroke(1.dp, colors.champagne.copy(alpha = 0.5f)),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Importer",
                                    tint = colors.champagne,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Importer",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // =============================================================
                // 5. OUTILS DE RETOUCHE & MINIATURES
                // =============================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "🎨 Outils de retouche & Miniatures",
                        color = colors.champagne,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ligne des filtres
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterOptions.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.champagne,
                                    selectedLabelColor = if (colors.isDark) Color.Black else Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Choix du format / ratio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Format :",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        ratioOptions.forEach { ratio ->
                            val isSel = selectedRatio == ratio
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) colors.champagne.copy(alpha = 0.2f) else Color(0xFF1E2230),
                                border = BorderStroke(1.dp, if (isSel) colors.champagne else Color.Transparent),
                                modifier = Modifier.clickable { selectedRatio = ratio }
                            ) {
                                Text(
                                    text = ratio,
                                    color = if (isSel) colors.champagne else Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sélecteur de miniature / couverture
                    Text(
                        text = "Sélection de la couverture / miniature :",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        thumbnailPlaceholders.forEachIndexed { idx, url ->
                            val isSelected = selectedThumbnailIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(width = 64.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        2.dp,
                                        if (isSelected) colors.champagne else Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedThumbnailIndex = idx }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Miniature $idx",
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(16.dp)
                                            .background(colors.champagne, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Titre / Description de la vidéo
                    OutlinedTextField(
                        value = videoTitle,
                        onValueChange = { videoTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creative_studio_title_input"),
                        label = { Text("Titre / Description de la création", fontSize = 12.sp) },
                        placeholder = { Text("Ex : Freestyle Afrobeat session #PANU", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.champagne,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================
                    // 6. BOUTON PUBLIER LA CRÉATION
                    // =========================================================
                    Button(
                        onClick = {
                            val title = videoTitle.trim().ifEmpty { "Création originale Studio PANU" }
                            isSubmitting = true
                            scope.launch {
                                val post = Post(
                                    id = UUID.randomUUID().toString(),
                                    authorId = userId,
                                    authorName = authorName,
                                    authorUsername = authorName.lowercase().replace(" ", "_"),
                                    authorAvatarUrl = thumbnailPlaceholders[selectedThumbnailIndex],
                                    title = title,
                                    content = "$title — Filtre : $selectedFilter, Durée : $selectedDuration. Remixe cette vidéo ou booste ton contenu ! 🚀",
                                    mediaUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                                    mediaType = "video",
                                    status = "published",
                                    visibility = "public",
                                    likesCount = 1
                                )
                                postRepository?.savePost(post)
                                onPostCreated(post)
                                isSubmitting = false
                                Toast.makeText(context, "🎉 Votre création a été publiée avec succès !", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("creative_studio_publish_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.champagne),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = if (colors.isDark) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Publier la création",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (colors.isDark) Color.Black else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
