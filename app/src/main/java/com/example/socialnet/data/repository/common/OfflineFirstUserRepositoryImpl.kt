package com.example.socialnet.data.repository.common

import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.model.User
import com.example.socialnet.core.utils.AppDispatchers
import com.example.socialnet.data.local.dao.PostDao
import com.example.socialnet.data.local.dao.UserDao
import com.example.socialnet.data.mapper.toDomain
import com.example.socialnet.data.mapper.toEntity
import com.example.socialnet.domain.repository.remote.UserRepository
import com.example.socialnet.domain.repository.common.OfflineFirstUserRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OfflineFirstUserRepositoryImpl @Inject constructor(
    private val firebaseUserRepository: UserRepository,
    private val userDao: UserDao,
    private val postDao: PostDao,
    private val dispatchers: AppDispatchers,
) : OfflineFirstUserRepository {

    /**
     * Реактивный поток из Room. Эмитит мгновенно то, что есть в кэше.
     * Свежесть обеспечивается фоновым вызовом syncUser() из ViewModel.
     */
    override fun observeUser(userId: String): Flow<User?> = flow {
        coroutineScope {
            launch {
                firebaseUserRepository.observeUser(userId).collect { user ->
                    if (user != null) userDao.upsertUser(user.toEntity())
                }
            }
            emitAll(userDao.observeUser(userId).map { it?.toDomain() })
        }
    }

    /**
     * Сначала сеть. Если сеть доступна — обновляем Room и возвращаем свежие данные.
     * Если сети нет (или Firebase вернул сетевую ошибку) — отдаём кэш.
     * Если кэша нет — возвращаем ошибку.
     */
    override suspend fun getUser(userId: String): AppResult<User> =
        withContext(dispatchers.io) {
            when (val networkResult = firebaseUserRepository.getUser(userId)) {
                is AppResult.Success -> {
                    userDao.upsertUser(networkResult.data.toEntity())
                    networkResult
                }
                is AppResult.Failure -> {
                    val cached = userDao.getUser(userId)
                    if (cached != null) {
                        AppResult.Success(cached.toDomain())
                    } else {
                        networkResult
                    }
                }
            }
        }

    override suspend fun updateProfile(
        userId: String,
        name: String?,
        bio: String?,
        avatarUrl: String?,
        avatarPublicId: String?,
        removeAvatar: Boolean
    ): AppResult<User> = withContext(dispatchers.io) {
        val result = firebaseUserRepository.updateProfile(
            userId = userId,
            name = name,
            bio = bio,
            avatarUrl = avatarUrl,
            avatarPublicId = avatarPublicId,
            removeAvatar = removeAvatar
        )

        when (result) {
            is AppResult.Success -> {
                val updatedUser = result.data

                userDao.upsertUser(updatedUser.toEntity())

                val nameChanged = name != null
                val avatarChanged = avatarUrl != null || removeAvatar
                if (nameChanged || avatarChanged) {
                    postDao.updateAuthorInPosts(
                        authorId = userId,
                        name = updatedUser.name,
                        avatarUrl = updatedUser.avatarUrl
                    )
                }

                result
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun syncUser(userId: String): AppResult<User> =
        withContext(dispatchers.io) {
            when (val result = firebaseUserRepository.getUser(userId)) {
                is AppResult.Success -> {
                    userDao.upsertUser(result.data.toEntity())
                    result
                }
                is AppResult.Failure -> {
                    val cached = userDao.getUser(userId)
                    if (cached != null) {
                        AppResult.Success(cached.toDomain())
                    } else {
                        result
                    }
                }
            }
        }
}