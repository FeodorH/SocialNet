package com.example.socialnet.domain.usecase

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.model.ReactionSummary
import com.example.socialnet.core.model.ReactionType
import com.example.socialnet.domain.repository.common.OfflineFirstAuthRepository
import com.example.socialnet.domain.repository.common.OfflineFirstReactionRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ToggleReactionUseCase @Inject constructor(
    private val auth: OfflineFirstAuthRepository,
    private val reactionRepository: OfflineFirstReactionRepository
) {
    suspend operator fun invoke(
        postId: String,
        type: ReactionType,
        current: ReactionType?
    ): AppResult<ReactionSummary> {
        val userId = auth.currentUserId.first()
            ?: return AppResult.Failure(AppError.Unauthorized)

        return if (current == type) {
            reactionRepository.removeReaction(userId, postId)
        } else {
            reactionRepository.setReaction(userId, postId, type)
        }
    }
}

/**
 * Реакция текущего пользователя на пост.
 * Используется на экране поста и в карточке поста.
 */
class ObserveMyReactionUseCase @Inject constructor(
    private val reactions: OfflineFirstReactionRepository
) {
    operator fun invoke(postId: String): Flow<ReactionType?> =
        reactions.observeMyReaction(postId)
}