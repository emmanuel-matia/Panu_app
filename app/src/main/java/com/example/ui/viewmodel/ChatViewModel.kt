package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.repository.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                role = "model",
                text = "👋 Bonjour ! Je suis votre Assistant IA et Directeur Artistique PANU (Propulsé par Gemini). Posez-moi vos questions, demandez des scripts, des analyses de projets ou des conseils d'afrofuturisme !"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3.8-flash") // Par défaut general tasks
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun sendMessage(userText: String) {
        val cleanText = userText.trim()
        if (cleanText.isBlank()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = cleanText
        )

        // Ajouter le message utilisateur à l'historique
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Construire l'historique multi-turn (rôle, texte)
                val history = _messages.value.dropLast(1).map { msg ->
                    val r = if (msg.role == "user") "user" else "model"
                    r to msg.text
                }

                val modelResponse = geminiRepository.chatWithGemini(
                    modelName = _selectedModel.value,
                    history = history,
                    userPrompt = cleanText,
                    systemInstruction = "Tu es l'Assistant IA expert et Directeur Artistique de l'application PANU. Tu aides les créateurs africains à concevoir des scripts, des vidéos, des idées de monétisation et des œuvres afrofuturistes. Ton ton est professionnel, inspirant, créatif et chaleureux."
                )

                val modelMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "model",
                    text = modelResponse
                )

                _messages.value = _messages.value + modelMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "model",
                    text = "⚠️ Erreur de communication avec Gemini (${e.localizedMessage ?: "Vérifiez votre connexion"})."
                )
                _messages.value = _messages.value + errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                id = "welcome_reset",
                role = "model",
                text = "✨ Conversation réinitialisée. Comment puis-je vous aider aujourd'hui ?"
            )
        )
    }
}
