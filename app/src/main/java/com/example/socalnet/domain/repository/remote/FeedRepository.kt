package com.example.socalnet.domain.repository.remote

import com.example.socalnet.core.common.AppResult
import com.example.socalnet.core.model.FeedType
import com.example.socalnet.core.model.PageCursor
import com.example.socalnet.core.model.Post
import com.example.socalnet.core.model.PostPage
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    suspend fun loadFeed(
        type: FeedType,
        cursor: PageCursor?,
        pageSize: Int,
    ): AppResult<PostPage>

    /**
     * Realtime-поток постов из Firestore.
     * Слушатель на query с тем же orderBy/limit, что и loadFeed.
     * Закрывается автоматически при отмене Flow.
     */
    fun observeFeedRealtime(type: FeedType, windowSize: Int): Flow<List<Post>>
}