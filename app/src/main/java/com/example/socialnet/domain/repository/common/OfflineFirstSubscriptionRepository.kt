package com.example.socialnet.domain.repository.common

import com.example.socialnet.core.common.AppResult
import kotlinx.coroutines.flow.Flow

interface OfflineFirstSubscriptionRepository {

    /** Мгновенный статус подписки из Room */
    fun observeIsSubscribed(userId: String, authorId: String): Flow<Boolean>

    /** Список ID авторов из Room (для ленты) */
    suspend fun getSubscriptionIds(userId: String): AppResult<List<String>>

    /** Подписка: сеть -> при успехе обновляем Room */
    suspend fun subscribe(userId: String, authorId: String): AppResult<Unit>

    /** Отписка: сеть -> при успехе удаляем из Room */
    suspend fun unsubscribe(userId: String, authorId: String): AppResult<Unit>

    /** Синхронизация всех подписок пользователя из сети в Room */
    suspend fun syncSubscriptions(userId: String): AppResult<Unit>

    /** Реактивный список авторов, на которых подписан пользователь. */
    fun observeSubscriptionIds(userId: String): Flow<List<String>>
}