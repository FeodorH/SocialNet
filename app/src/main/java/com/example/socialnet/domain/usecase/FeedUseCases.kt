package com.example.socialnet.domain.usecase

import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.common.WorkingRules
import com.example.socialnet.core.model.FeedType
import com.example.socialnet.core.model.PageCursor
import com.example.socialnet.core.model.Post
import com.example.socialnet.domain.repository.common.OfflineFirstAuthRepository
import com.example.socialnet.domain.repository.common.OfflineFirstFeedRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * Наблюдение за лентой из Room. Мгновенно, без сети.
 * Подписка на смену пользователя — через flatMapLatest.
 */
class ObserveFeedUseCase @Inject constructor(
    private val authRepository: OfflineFirstAuthRepository,
    private val feeds: OfflineFirstFeedRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(type: FeedType): Flow<List<Post>> {
        return authRepository.currentUserId.flatMapLatest { userId ->
            feeds.observeFeed(type, userId)
        }
    }
}

/**
 * Загрузка страницы ленты из сети с записью в Room.
 * Возвращает курсор следующей страницы (null = конец).
 */
class SyncFeedUseCase @Inject constructor(
    private val feeds: OfflineFirstFeedRepository
) {
    suspend operator fun invoke(
        type: FeedType,
        cursor: PageCursor? = null
    ): AppResult<PageCursor?> = feeds.syncFeed(
        type = type,
        cursor = cursor,
        pageSize = WorkingRules.FEED_PAGE_SIZE
    )
}