package com.example.socialnet.domain.repository.remote

import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.model.ImageRef
import com.example.socialnet.core.model.PageCursor
import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.PostPage
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun createPost(
        userId: String,
        text: String,
        images: List<ImageRef.Remote>): AppResult<Post>

    suspend fun updatePost(
        userId: String,
        postId: String,
        text: String,
        addedImages: List<ImageRef.Remote>,
    ): AppResult<Post>

    suspend fun deletePost(postId: String): AppResult<Unit>
    suspend fun getPost(postId: String): AppResult<Post>

    suspend fun getUserPosts(
        userId: String,
        cursor: PageCursor?,
        pageSize: Int,
    ): AppResult<PostPage>

    fun observeUserPostsRealtime(userId: String, windowSize: Int): Flow<List<Post>>

}