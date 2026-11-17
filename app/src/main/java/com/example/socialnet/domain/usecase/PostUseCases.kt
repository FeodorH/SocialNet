package com.example.socialnet.domain.usecase

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.model.ImageRef
import com.example.socialnet.core.model.PageCursor
import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.PostDraft
import com.example.socialnet.core.utils.IdGenerator
import com.example.socialnet.domain.repository.common.OfflineFirstAuthRepository
import com.example.socialnet.domain.repository.common.OfflineFirstPostRepository
import com.example.socialnet.domain.repository.remote.MediaRepository
import com.example.socialnet.domain.vaildation.PostValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val auth: OfflineFirstAuthRepository,
    private val posts: OfflineFirstPostRepository,
    private val media: MediaRepository
) {
    suspend operator fun invoke(draft: PostDraft): AppResult<Post> {
        PostValidator.validate(draft.text, draft.images.size)?.let { return AppResult.Failure(it) }

        val userId = auth.currentUserId.first() ?: return AppResult.Failure(AppError.Unauthorized)

        val uploadBatchId = IdGenerator.generate()
        val uploadedRefs = mutableListOf<ImageRef.Remote>()
        val uploadedTokens = mutableListOf<String>()

        for ((index, image) in draft.images.withIndex()) {
            val cloudinaryPath = "socialnet/posts/$uploadBatchId/$index"
            when (val r = media.upload(image, cloudinaryPath)) {
                is AppResult.Success -> {
                    uploadedRefs += r.data.imageRef
                    r.data.deleteToken?.let { uploadedTokens += it }
                }
                is AppResult.Failure -> {
                    media.deleteByTokens(uploadedTokens)
                    return r
                }
            }
        }

        val postResult = posts.createPost(userId, draft.text, uploadedRefs)
        if (postResult is AppResult.Failure) {
            media.deleteByTokens(uploadedTokens)
        }
        return postResult
    }
}

class UpdatePostUseCase @Inject constructor(
    private val auth: OfflineFirstAuthRepository,
    private val posts: OfflineFirstPostRepository,
    private val media: MediaRepository
) {
    suspend operator fun invoke(
        postId: String,
        text: String,
        existingImages: List<ImageRef.Remote>,
        addedImages: List<ImageRef.Local>
    ): AppResult<Post> {
        val userId = auth.currentUserId.first() ?: return AppResult.Failure(AppError.Unauthorized)

        // syncPost: сначала сеть, при ошибке — кэш
        val originalPost = when (val r = posts.syncPost(postId)) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }

        if (originalPost.author.id != userId) {
            return AppResult.Failure(AppError.Unauthorized)
        }

        val existingUrls = existingImages.map { it.url }.toSet()
        val removedImageUrls = originalPost.images
            .map { it.url }
            .filter { it !in existingUrls }

        PostValidator.validate(text, existingImages.size + addedImages.size)
            ?.let { return AppResult.Failure(it) }

        val uploadedRefs = mutableListOf<ImageRef.Remote>()
        val uploadedTokens = mutableListOf<String>()
        val uploadBatchId = IdGenerator.generate()

        for ((index, image) in addedImages.withIndex()) {
            val path = "socialnet/posts/$postId/${uploadBatchId}_$index"
            when (val r = media.upload(image, path)) {
                is AppResult.Success -> {
                    uploadedRefs += r.data.imageRef
                    r.data.deleteToken?.let { uploadedTokens += it }
                }
                is AppResult.Failure -> {
                    media.deleteByTokens(uploadedTokens)
                    return r
                }
            }
        }

        val finalImages = existingImages + uploadedRefs
        val updateResult = posts.updatePost(
            userId = userId,
            postId = postId,
            text = text,
            addedImages = finalImages,
            removedImageUrls = removedImageUrls
        )

        if (updateResult is AppResult.Failure) {
            media.deleteByTokens(uploadedTokens)
            return updateResult
        }
        return updateResult
    }
}

class DeletePostUseCase @Inject constructor(
    private val posts: OfflineFirstPostRepository
) {
    suspend operator fun invoke(postId: String): AppResult<Unit> = posts.deletePost(postId)
}

class GetPostUseCase @Inject constructor(
    private val posts: OfflineFirstPostRepository
) {
    suspend operator fun invoke(postId: String): AppResult<Post> = posts.syncPost(postId)
}

class ObserveUserPostsUseCase @Inject constructor(
    private val posts: OfflineFirstPostRepository
) {
    /** Реактивно из Room. */
    operator fun invoke(userId: String): Flow<List<Post>> = posts.observeUserPosts(userId)
}

class SyncUserPostsUseCase @Inject constructor(
    private val posts: OfflineFirstPostRepository
) {
    /** Сеть → Room. Возвращает курсор следующей страницы. */
    suspend operator fun invoke(
        userId: String,
        cursor: PageCursor? = null
    ): AppResult<PageCursor?> = posts.syncUserPosts(userId, cursor)
}