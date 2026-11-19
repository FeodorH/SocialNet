package com.example.socialnet.data.mapper

import com.example.socialnet.core.model.ImageRef
import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.PostAuthor
import com.example.socialnet.core.model.ReactionSummary
import com.example.socialnet.core.model.ReactionType
import com.example.socialnet.data.local.entity.PostEntity
import kotlin.time.Instant

fun PostEntity.toDomain(): Post {
    val images = imageUrls.mapIndexed { index, url ->
        val publicId = imagePublicIds.getOrNull(index)
        ImageRef.Remote(url = url, publicId = publicId)
    }

    val reactions = ReactionSummary(
        total = reactionsTotal,
        byType = reactionsByType.mapNotNull { (key, value) ->
            runCatching { ReactionType.valueOf(key) }.getOrNull()?.let { it to value }
        }.toMap()
    )

    return Post(
        id = id,
        author = PostAuthor(
            id = authorId,
            name = authorName,
            avatarUrl = authorAvatarUrl
        ),
        text = text,
        images = images,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
        updatedAt = updatedAtEpochMillis?.let { Instant.fromEpochMilliseconds(it) },
        reactions = reactions,
        myReaction = myReactionType?.let { runCatching { ReactionType.valueOf(it) }.getOrNull() }
    )
}

fun Post.toEntity(): PostEntity {
    return PostEntity(
        id = id,
        authorId = author.id,
        authorName = author.name,
        authorAvatarUrl = author.avatarUrl,
        text = text,
        imageUrls = images.map { it.url },
        imagePublicIds = images.map { it.publicId ?: "" },
        createdAtEpochMillis = createdAt.toEpochMilliseconds(),
        updatedAtEpochMillis = updatedAt?.toEpochMilliseconds(),
        reactionsTotal = reactions.total,
        reactionsByType = reactions.byType.mapKeys { it.key.name }.mapValues { it.value },
        myReactionType = myReaction?.name,
        cachedAt = System.currentTimeMillis()
    )
}