package com.example.data.model

import java.util.UUID

data class PostComment(
    val id: String = UUID.randomUUID().toString(),
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String? = null,
    val authorAvatarUrl: String? = null,
    val content: String,
    val likesCount: Int = 0,
    val createdAt: String = "À l'instant"
)
