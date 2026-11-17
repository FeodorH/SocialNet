package com.example.socialnet.data.repository.remote

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.model.FeedType
import com.example.socialnet.core.model.PageCursor
import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.PostAuthor
import com.example.socialnet.core.model.PostPage
import com.example.socialnet.core.model.ReactionType
import com.example.socialnet.core.utils.AppDispatchers
import com.example.socialnet.core.utils.CursorCodec
import com.example.socialnet.data.dto.PostResponse
import com.example.socialnet.data.dto.UserResponse
import com.example.socialnet.data.mapper.toDomain
import com.example.socialnet.domain.repository.remote.AuthRepository
import com.example.socialnet.domain.repository.remote.FeedRepository
import com.example.socialnet.domain.repository.remote.SubscriptionRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class FirebaseFeedRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val dispatchers: AppDispatchers
) : FeedRepository {

    private val firestore = Firebase.firestore
    private val postsCollection = firestore.collection("posts")
    private val usersCollection = firestore.collection("users")

    companion object {
        // Лимит whereIn. В новых версиях Firestore — 30, в старых — 10.
        private const val MAX_WHERE_IN_SIZE = 30
    }

    override suspend fun loadFeed(
        type: FeedType,
        cursor: PageCursor?,
        pageSize: Int
    ): AppResult<PostPage> = withContext(dispatchers.io) {
        try {
            when (type) {
                FeedType.GENERAL -> loadGeneralFeed(cursor, pageSize)
                FeedType.SUBSCRIPTIONS -> loadSubscriptionsFeed(cursor, pageSize)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e))
        }
    }

    override fun observeFeedRealtime(
        type: FeedType,
        windowSize: Int,
    ): Flow<List<Post>> = callbackFlow {
        val query = buildFeedQuery(type, windowSize)

        if (query == null) {
            close()
            return@callbackFlow
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }
            if (snapshot != null) {
                launch {
                    val responses = snapshot.documents.mapNotNull {
                        it.toObject(PostResponse::class.java)
                    }
                    trySend(enrichPosts(responses))
                }
            }
        }

        awaitClose { listener.remove() }
    }.flowOn(dispatchers.io)

    private suspend fun loadGeneralFeed(
        cursor: PageCursor?,
        pageSize: Int
    ): AppResult<PostPage> {
        var query = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit((pageSize).toLong())

        if (cursor != null) {
            val (timestamp, postId) = CursorCodec.decode(cursor)
            query = query.startAfter(timestamp, postId)
        }

        val snapshot = query.get().await()
        val docs = snapshot.documents
        val hasMore = docs.size > pageSize
        val pageDocs = if (hasMore) docs.take(pageSize) else docs

        val postResponses = pageDocs.mapNotNull { it.toObject(PostResponse::class.java) }
        val posts = enrichPosts(postResponses)

        val lastDoc = pageDocs.lastOrNull()
        val nextCursor = if (hasMore && lastDoc != null) {
            lastDoc.getTimestamp("createdAt")?.let { CursorCodec.encode(it, lastDoc.id) }
        } else null

        return AppResult.Success(
            PostPage(posts = posts, nextCursor = nextCursor, hasMore = hasMore)
        )
    }

    private suspend fun loadSubscriptionsFeed(
        cursor: PageCursor?,
        pageSize: Int
    ): AppResult<PostPage> {
        val currentUserId = authRepository.currentUserId.first()
            ?: return AppResult.Failure(AppError.Unauthorized)

        /** получение id подписок */
        val followingIds = when (val r = subscriptionRepository.getSubscriptionIds(currentUserId)) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }

        if (followingIds.isEmpty()) {
            return AppResult.Success(PostPage(emptyList(), null, false))
        }

        /** разбение на чанки и запросы */
        val chunks = followingIds.chunked(MAX_WHERE_IN_SIZE)

        val allPostResponses = coroutineScope {
            chunks.map { chunk ->
                async {
                    var query = postsCollection
                        .whereIn("authorId", chunk)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit((pageSize).toLong())

                    if (cursor != null) {
                        val (timestamp, postId) = CursorCodec.decode(cursor)
                        query = query.startAfter(timestamp, postId)
                    }

                    val snapshot = query.get().await()
                    snapshot.documents.mapNotNull { it.toObject(PostResponse::class.java) }
                }
            }.awaitAll().flatten()
        }

        /** удаляем дубликаты и сортируем по времени */
        val uniqueResponses = allPostResponses.distinctBy { it.id }
        val sortedResponses = uniqueResponses
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            .take(pageSize)

        if (sortedResponses.isEmpty()) {
            return AppResult.Success(PostPage(emptyList(), null, false))
        }

        val posts = enrichPosts(sortedResponses)

        /** задаём курсор */
        val lastPost = sortedResponses.last()
        val nextCursor = if (uniqueResponses.size > pageSize) {
            lastPost.createdAt?.let { CursorCodec.encode(it, lastPost.id) }
        } else null

        return AppResult.Success(
            PostPage(posts = posts, nextCursor = nextCursor, hasMore = nextCursor != null)
        )
    }


    /**
     * Загружает авторов и реакции для набора постов через batch-запросы.
     * Вместо N+1 запросов делает 2 batch-запроса (getAll).
     */
    private suspend fun enrichPosts(postResponses: List<PostResponse>): List<Post> {
        if (postResponses.isEmpty()) return emptyList()

        val currentUserId = authRepository.currentUserId.first()
        val authorIds = postResponses.map { it.authorId }.distinct()
        val postIds = postResponses.map { it.id }

        // Параллельно грузим авторов и реакции
        return coroutineScope {
            val authorsDeferred = async { fetchAuthors(authorIds) }
            val reactionsDeferred = async {
                if (currentUserId != null) {
                    fetchMyReactions(postIds, currentUserId)
                } else {
                    emptyMap()
                }
            }

            val authorsMap = authorsDeferred.await()
            val reactionsMap = reactionsDeferred.await()

            postResponses.mapNotNull { response ->
                val author = authorsMap[response.authorId] ?: return@mapNotNull null
                response.toDomain(author, reactionsMap[response.id])
            }
        }
    }

    private suspend fun fetchAuthors(authorIds: List<String>): Map<String, PostAuthor> =
        coroutineScope {
            val authors = authorIds.map { id ->
                async {
                    val snapshot = usersCollection.document(id).get().await()
                    snapshot.toObject(UserResponse::class.java)
                }
            }.awaitAll().filterNotNull()

            authors.associate { user ->
                user.id to PostAuthor(user.id, user.name, user.avatarUrl)
            }
        }

    private suspend fun fetchMyReactions(
        postIds: List<String>,
        currentUserId: String
    ): Map<String, ReactionType?> = coroutineScope {
        if (postIds.isEmpty()) return@coroutineScope emptyMap()

        postIds.map { postId ->
            async {
                val snapshot = postsCollection.document(postId)
                    .collection("reactions").document(currentUserId)
                    .get().await()

                val typeName = snapshot.getString("type")
                val type = typeName?.let { runCatching { ReactionType.valueOf(it) }.getOrNull() }
                postId to type
            }
        }.awaitAll().toMap()
    }

    /**
     * Собирает query так же, как loadFeed, но без пагинации — только верхнее окно.
     * Возвращает null для SUBSCRIPTIONS без авторизации.
     */
    private suspend fun buildFeedQuery(
        type: FeedType,
        windowSize: Int
    ): Query? = when (type) {
        FeedType.GENERAL -> postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(windowSize.toLong())

        FeedType.SUBSCRIPTIONS -> {
            val currentUserId = authRepository.currentUserId.first()
                ?: return null
            val followingIds = (subscriptionRepository.getSubscriptionIds(currentUserId)
                    as? AppResult.Success)?.data ?: return null
            if (followingIds.isEmpty()) return null
            val chunk = followingIds.take(MAX_WHERE_IN_SIZE)
            postsCollection
                .whereIn("authorId", chunk)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(windowSize.toLong())
        }
    }
}