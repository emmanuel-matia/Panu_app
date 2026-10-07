package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.PanuTheme
import com.example.ui.util.MediaPickerUtils

/**
 * Dialogue multimodal et adaptatif pour la sélection de médias :
 * - Photo de profil (Avatar)
 * - Photo de couverture
 * - Logos et visuels de marque
 * - Vidéos et clips
 *
 * Accès direct et prioritaire à la galerie locale du téléphone (sans forcer Google Photos),
 * ainsi qu'à l'appareil photo physique et aux fichiers de la mémoire interne.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionDialog(
    isOpen: Boolean,
    title: String = "Choisir une photo",
    description: String = "Sélectionnez votre média depuis votre téléphone pour mettre à jour votre contenu :",
    isVideo: Boolean = false,
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraPermissionNotice by remember { mutableStateOf(false) }

    // 1. Lanceur prioritaire Galerie Locale native du téléphone (Samsung Gallery, Xiaomi Gallery, etc.)
    val localGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            onImageSelected(uri)
            onDismiss()
        }
    }

    // 2. Sélecteur Android Photo Picker
    val visualMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
            onDismiss()
        }
    }

    // 3. Sélecteur Fichiers / Mémoire interne
    val getContentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
            onDismiss()
        }
    }

    // 4. Capture caméra photo physique
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            onImageSelected(tempCameraUri!!)
            onDismiss()
        }
    }

    // 5. Capture caméra vidéo physique
    val recordVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            onImageSelected(tempCameraUri!!)
            onDismiss()
        }
    }

    // Gestion de la permission caméra
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                if (isVideo) {
                    val uri = MediaPickerUtils.createTempFileUri(context, "panu_video_camera_", ".mp4")
                    tempCameraUri = uri
                    recordVideoLauncher.launch(uri)
                } else {
                    val uri = MediaPickerUtils.createTempFileUri(context, "panu_photo_camera_", ".jpg")
                    tempCameraUri = uri
                    takePhotoLauncher.launch(uri)
                }
            } catch (e: Exception) {
                showCameraPermissionNotice = true
            }
        } else {
            showCameraPermissionNotice = true
        }
    }

    fun launchCamera() {
        val hasCamPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCamPermission) {
            try {
                if (isVideo) {
                    val uri = MediaPickerUtils.createTempFileUri(context, "panu_video_camera_", ".mp4")
                    tempCameraUri = uri
                    recordVideoLauncher.launch(uri)
                } else {
                    val uri = MediaPickerUtils.createTempFileUri(context, "panu_photo_camera_", ".jpg")
                    tempCameraUri = uri
                    takePhotoLauncher.launch(uri)
                }
            } catch (e: Exception) {
                showCameraPermissionNotice = true
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchLocalGallery() {
        try {
            // Ouvre directement l'application Galerie locale du téléphone
            val galleryIntent = if (isVideo) {
                Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "video/*"
                }
            } else {
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    type = "image/*"
                }
            }
            localGalleryLauncher.launch(galleryIntent)
        } catch (_: Exception) {
            // Fallback 1: Photo Picker moderne
            try {
                if (isVideo) {
                    visualMediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                } else {
                    visualMediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            } catch (_: Exception) {
                // Fallback 2: Content Picker
                getContentPickerLauncher.launch(if (isVideo) "video/*" else "image/*")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PanuTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PanuTheme.colors.champagne.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PanuTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = PanuTheme.colors.textSecondary)
                }
            }

            if (showCameraPermissionNotice) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Permission appareil photo requise. Veuillez l'activer dans les paramètres système de votre appareil.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = PanuTheme.colors.textSecondary
            )

            // Source 1: Galerie native locale du téléphone (Prioritaire & sans forcer Google Photos)
            SourceActionCard(
                icon = Icons.Default.PhotoLibrary,
                title = if (isVideo) "Galerie Vidéos locale du téléphone" else "Galerie Photos locale du téléphone",
                description = "Ouvre directement l'application Galerie locale de votre téléphone",
                testTag = "picker_btn_gallery",
                onClick = { launchLocalGallery() }
            )

            // Source 2: Appareil photo / Caméra physique
            SourceActionCard(
                icon = if (isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                title = if (isVideo) "Caméra (Enregistrer une vidéo)" else "Appareil photo (Prendre une photo)",
                description = if (isVideo) "Capturer une vidéo directement avec la caméra" else "Capturer une nouvelle photo avec la caméra du smartphone",
                testTag = "picker_btn_camera",
                onClick = { launchCamera() }
            )

            // Source 3: Parcourir les fichiers & stockage interne du téléphone
            SourceActionCard(
                icon = Icons.Default.FolderOpen,
                title = "Mémoire du téléphone (Fichiers & Téléchargements)",
                description = "Accéder à l'explorateur de fichiers ou Téléchargements",
                testTag = "picker_btn_storage",
                onClick = {
                    getContentPickerLauncher.launch(if (isVideo) "video/*" else "image/*")
                }
            )
        }
    }
}

@Composable
private fun SourceActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    testTag: String = "",
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = PanuTheme.colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, PanuTheme.colors.surfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PanuTheme.colors.champagne.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PanuTheme.colors.champagne, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PanuTheme.colors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PanuTheme.colors.textSecondary
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PanuTheme.colors.textSecondary)
        }
    }
}
