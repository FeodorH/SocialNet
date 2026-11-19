package com.example.socialnet.data.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class PostResponse(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val text: String = "",
    val imageUrls: List<String> = emptyList(),
    val imagePublicIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val reactionsTotal: Long = 0L,
    val reactionsByType: Map<String, Long> = emptyMap()
)