package com.example.socialnet.presentation.feed.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.socialnet.core.common.UiState
import com.example.socialnet.core.model.FeedType
import com.example.socialnet.core.model.ImageRef
import com.example.socialnet.core.model.Post
import com.example.socialnet.core.model.PostAuthor
import com.example.socialnet.core.model.ReactionSummary
import com.example.socialnet.core.model.ReactionType
import com.example.socialnet.presentation.feed.FeedScreen
import com.example.socialnet.presentation.feed.model.FeedUiState
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private val now = Clock.System.now()

private val alice = PostAuthor("1", "Alice Wonder", null)
private val bob = PostAuthor("2", "Bob Builder", null)
private val charlie = PostAuthor("3", "Charlie C", null)

private val fakePosts = listOf(
    Post(
        id = "p1",
        author = alice,
        text = "Сегодня отличный день для нового поста. Пишу из своего любимого кафе ☕",
        images = emptyList(),
        createdAt = now - 5.minutes,
        reactions = ReactionSummary(
            total = 12,
            byType = mapOf(ReactionType.LIKE to 8, ReactionType.LOVE to 4),
        ),
        myReaction = ReactionType.LIKE,
    ),
    Post(
        id = "p2",
        author = bob,
        text = "Посмотрите на этот закат!",
        images = listOf(ImageRef.Remote("https://example.com/1.jpg", null)),
        createdAt = now - 2.hours,
        reactions = ReactionSummary(
            total = 42,
            byType = mapOf(ReactionType.LIKE to 30, ReactionType.FIRE to 12),
        ),
        myReaction = null,
    ),
    Post(
        id = "p3",
        author = charlie,
        text = "Свежие фото с прогулки 🌳",
        images = listOf(
            ImageRef.Remote("https://example.com/2.jpg", null),
            ImageRef.Remote("https://example.com/3.jpg", null),
        ),
        createdAt = now - 1.days,
        reactions = ReactionSummary(
            total = 5,
            byType = mapOf(ReactionType.LOVE to 5),
        ),
        myReaction = ReactionType.LOVE,
    ),
    Post(
        id = "p4",
        author = alice,
        text = "Много фото с поездки",
        images = (1..4).map { ImageRef.Remote("https://example.com/4_$it.jpg", null) },
        createdAt = now - 3.days,
        reactions = ReactionSummary.EMPTY,
        myReaction = null,
    ),
)

private val feedContentState = UiState.Content(
    FeedUiState(
        posts = fakePosts,
        hasMore = true,
    )
)

@Preview(showBackground = true, heightDp = 1200, name = "Feed · User")
@Composable
private fun FeedUserPreview() {
    MaterialTheme {
        FeedScreen(
            state = feedContentState,
            selectedTab = FeedType.GENERAL,
            role = true,
            onTabSelected = {}, onReactionClick = { _, _ -> },
            onSubscribeClick = {_, _ -> },
            onAuthorClick = {_ ->},
            onRefresh = {}, onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, name = "Feed · Guest")
@Composable
private fun FeedGuestPreview() {
    MaterialTheme {
        FeedScreen(
            state = feedContentState,
            selectedTab = FeedType.GENERAL,
            role = false,
            onTabSelected = {}, onReactionClick = { _, _ -> },
            onSubscribeClick = {_, _ -> },
            onAuthorClick = {_ ->},
            onRefresh = {}, onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 600, name = "Feed · Empty")
@Composable
private fun FeedEmptyPreview() {
    MaterialTheme {
        FeedScreen(
            state = UiState.Empty,
            selectedTab = FeedType.GENERAL,
            role = true,
            onTabSelected = {}, onReactionClick = { _, _ -> },
            onSubscribeClick = {_, _ -> },
            onAuthorClick = {_ ->},
            onRefresh = {}, onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 600, name = "Feed · Error")
@Composable
private fun FeedErrorPreview() {
    MaterialTheme {
        FeedScreen(
            state = UiState.Error("Не удалось загрузить ленту"),
            selectedTab = FeedType.GENERAL,
            role = true,
            onTabSelected = {}, onReactionClick = { _, _ -> },
            onSubscribeClick = {_, _ -> },
            onAuthorClick = {_ ->},
            onRefresh = {}, onLoadMore = {},
        )
    }
}