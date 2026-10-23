package com.example.data.remote

import com.example.data.model.Post
import com.example.data.model.PostComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SupabasePostService(private val client: SupabaseClient) {

    suspend fun getPublishedPosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        // Query posts with author profiles joined or select fields
        val endpoint = "/rest/v1/posts?status=eq.published&visibility=eq.public&order=created_at.desc&select=id,author_id,title,content,media_url,media_type,status,visibility,likes_count,created_at,updated_at,profiles(full_name,username,avatar_url)"
        when (val response = client.execute(endpoint)) {
            is SupabaseResponse.Success -> {
                val array = response.asJsonArray() ?: JSONArray()
                val list = mutableListOf<Post>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parsePost(obj))
                }
                Result.success(list)
            }
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun getCanvasProjectsAsPosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        val endpoint = "/rest/v1/canvas_projects?order=updated_at.desc&select=id,user_id,title,export_format,exportFormat,preview_url,previewUrl,layers_json,updated_at,profiles(full_name,username,avatar_url)"
        when (val response = client.execute(endpoint)) {
            is SupabaseResponse.Success -> {
                val array = response.asJsonArray() ?: JSONArray()
                val list = mutableListOf<Post>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    val userId = obj.optString("user_id")
                    val title = obj.optString("title", "Création Studio PANU")
                    val exportFormat = obj.optString("export_format", "")
                        .ifBlank { obj.optString("exportFormat", "MP4") }
                    val layersJson = obj.optJSONArray("layers_json")
                    val layerFirstUrl = if (layersJson != null && layersJson.length() > 0) {
                        layersJson.optJSONObject(0)?.optString("content")?.takeIf { it.startsWith("http") }
                    } else null
                    val previewUrl = obj.optString("preview_url", "")
                        .ifBlank { obj.optString("previewUrl", "") }
                        .takeIf { it.isNotBlank() } ?: layerFirstUrl
                    val profilesObj = obj.optJSONObject("profiles")

                    val mediaType = if (exportFormat.equals("MP4", ignoreCase = true) || (previewUrl?.endsWith(".mp4", ignoreCase = true) == true)) "video" else "image"

                    list.add(
                        Post(
                            id = "canvas_$id",
                            authorId = userId,
                            authorName = profilesObj?.optString("full_name")?.takeIf { it.isNotBlank() } ?: "Créateur PANU",
                            authorUsername = profilesObj?.optString("username")?.takeIf { it.isNotBlank() } ?: "createur_panu",
                            authorAvatarUrl = profilesObj?.optString("avatar_url"),
                            title = title,
                            content = "Nouvelle création Studio PANU ($exportFormat)",
                            mediaUrl = previewUrl,
                            mediaType = mediaType,
                            status = "published",
                            visibility = "public",
                            likesCount = 0,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                Result.success(list)
            }
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun getUserPosts(authorId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        val endpoint = "/rest/v1/posts?author_id=eq.$authorId&order=created_at.desc&select=*"
        when (val response = client.execute(endpoint)) {
            is SupabaseResponse.Success -> {
                val array = response.asJsonArray() ?: JSONArray()
                val list = mutableListOf<Post>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parsePost(obj))
                }
                Result.success(list)
            }
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("author_id", post.authorId)
            post.title?.let { put("title", it) }
            put("content", post.content)
            post.mediaUrl?.let { put("media_url", it) }
            put("media_type", post.mediaType)
            put("status", post.status)
            put("visibility", post.visibility)
        }

        val endpoint = "/rest/v1/posts"
        val headers = mapOf("Prefer" to "return=representation")
        when (val response = client.execute(endpoint, method = "POST", jsonBody = payload.toString(), headers = headers)) {
            is SupabaseResponse.Success -> {
                val array = response.asJsonArray()
                if (array != null && array.length() > 0) {
                    Result.success(parsePost(array.getJSONObject(0)))
                } else {
                    Result.success(post)
                }
            }
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun updatePost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            post.title?.let { put("title", it) }
            put("content", post.content)
            post.mediaUrl?.let { put("media_url", it) }
            put("media_type", post.mediaType)
            put("status", post.status)
            put("visibility", post.visibility)
        }

        val endpoint = "/rest/v1/posts?id=eq.${post.id}"
        val headers = mapOf("Prefer" to "return=representation")
        when (val response = client.execute(endpoint, method = "PATCH", jsonBody = payload.toString(), headers = headers)) {
            is SupabaseResponse.Success -> {
                val array = response.asJsonArray()
                if (array != null && array.length() > 0) {
                    Result.success(parsePost(array.getJSONObject(0)))
                } else {
                    Result.success(post)
                }
            }
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun updatePostStatus(postId: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("status", status)
        }
        val endpoint = "/rest/v1/posts?id=eq.$postId"
        when (val response = client.execute(endpoint, method = "PATCH", jsonBody = payload.toString())) {
            is SupabaseResponse.Success -> Result.success(Unit)
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val endpoint = "/rest/v1/posts?id=eq.$postId"
        when (val response = client.execute(endpoint, method = "DELETE")) {
            is SupabaseResponse.Success -> Result.success(Unit)
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun updateLikesCount(postId: String, newCount: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("likes_count", newCount)
        }
        val endpoint = "/rest/v1/posts?id=eq.$postId"
        when (val response = client.execute(endpoint, method = "PATCH", jsonBody = payload.toString())) {
            is SupabaseResponse.Success -> Result.success(Unit)
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun getPostComments(postId: String): Result<List<PostComment>> = withContext(Dispatchers.IO) {
        val endpoint = "/rest/v1/post_comments?post_id=eq.$postId&order=created_at.asc&select=*"
        when (val response = client.execute(endpoint)) {
            is SupabaseResponse.Success -> {
                val array = response.asJsonArray() ?: JSONArray()
                val list = mutableListOf<PostComment>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PostComment(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            postId = obj.optString("post_id", postId),
                            authorId = obj.optString("author_id", "user"),
                            authorName = obj.optString("author_name", "Membre PANU"),
                            authorUsername = obj.optString("author_username", "membre"),
                            authorAvatarUrl = obj.optString("author_avatar_url").takeIf { it.isNotBlank() },
                            content = obj.optString("content", ""),
                            likesCount = obj.optInt("likes_count", 0),
                            createdAt = obj.optString("created_at", "Récemment").take(16).replace("T", " ")
                        )
                    )
                }
                Result.success(list)
            }
            is SupabaseResponse.Error -> Result.failure(Exception(response.message))
            is SupabaseResponse.NetworkError -> Result.failure(Exception(response.message))
        }
    }

    suspend fun addPostComment(comment: PostComment): Result<PostComment> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("id", comment.id)
            put("post_id", comment.postId)
            put("author_id", comment.authorId)
            put("author_name", comment.authorName)
            comment.authorUsername?.let { put("author_username", it) }
            comment.authorAvatarUrl?.let { put("author_avatar_url", it) }
            put("content", comment.content)
            put("likes_count", 0)
        }
        val endpoint = "/rest/v1/post_comments"
        val headers = mapOf("Prefer" to "return=representation")
        when (val response = client.execute(endpoint, method = "POST", jsonBody = payload.toString(), headers = headers)) {
            is SupabaseResponse.Success -> Result.success(comment)
            is SupabaseResponse.Error -> Result.success(comment) // optimistic fallback
            is SupabaseResponse.NetworkError -> Result.success(comment)
        }
    }

    private fun parsePost(obj: JSONObject): Post {
        val profilesObj = obj.optJSONObject("profiles")
        return Post(
            id = obj.optString("id"),
            authorId = obj.optString("author_id"),
            authorName = profilesObj?.optString("full_name")?.takeIf { it.isNotBlank() } ?: obj.optString("author_name").takeIf { it.isNotBlank() },
            authorUsername = profilesObj?.optString("username")?.takeIf { it.isNotBlank() } ?: obj.optString("author_username").takeIf { it.isNotBlank() },
            authorAvatarUrl = profilesObj?.optString("avatar_url")?.takeIf { it.isNotBlank() } ?: obj.optString("author_avatar_url").takeIf { it.isNotBlank() },
            title = obj.optString("title").takeIf { it.isNotBlank() },
            content = obj.optString("content", ""),
            mediaUrl = obj.optString("media_url").takeIf { it.isNotBlank() },
            mediaType = obj.optString("media_type", "none"),
            status = obj.optString("status", "draft"),
            visibility = obj.optString("visibility", "public"),
            likesCount = obj.optInt("likes_count", 0),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
