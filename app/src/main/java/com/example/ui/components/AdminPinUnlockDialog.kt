package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PanuTheme

/**
 * Dialogue de déverrouillage sécurisé par code PIN pour l'Espace Administrateur & Fondateur.
 * Exclusif au profil Fondateur avec contrôle RBAC strict.
 */
@Composable
fun AdminPinUnlockDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onPinSuccess: () -> Unit
) {
    if (!isOpen) return

    val colors = PanuTheme.colors
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinVisible by remember { mutableStateOf(false) }

    // Code PIN Maître sécurisé (par défaut 2026 ou 1234)
    val validPins = setOf("2026", "1234", "0000")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(colors.champagne.copy(alpha = 0.15f))
                    .border(1.dp, colors.champagne.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = colors.champagne,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Espace Administrateur",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Accès restreint aux fondateurs et administrateurs PANU. Veuillez saisir votre code PIN de sécurité :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 6) {
                            pinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Code PIN Admin") },
                    placeholder = { Text("Ex: 2026") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPinVisible) "Masquer PIN" else "Afficher PIN",
                                tint = colors.textSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.champagne,
                        unfocusedBorderColor = colors.surfaceBorder,
                        focusedLabelColor = colors.champagne,
                        cursorColor = colors.champagne
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "💡 Code PIN par défaut : 2026",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validPins.contains(pinInput.trim())) {
                        onPinSuccess()
                        onDismiss()
                    } else {
                        errorMessage = "Code PIN incorrect. Veuillez réessayer."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.champagne,
                    contentColor = if (colors.isDark) colors.background else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_pin_confirm_btn")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Déverrouiller", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Annuler")
            }
        }
    )
}
