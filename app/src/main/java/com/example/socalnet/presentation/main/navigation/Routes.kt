package com.example.socalnet.presentation.main.navigation

object MainRoutes {
    const val FEED = "feed"
    const val PROFILE = "profile"
    const val SAVE_POST = "save_post?postId={postId}"
    const val SAVE_POST_ARG = "postId"
    const val USER_PROFILE = "user_profile/{userId}"
    const val USER_PROFILE_ARG = "userId"

    const val AUTH_GRAPH = "auth"
    const val AUTH_LOGIN = "auth/login"
    const val AUTH_REGISTER = "auth/register"
    const val AUTH_RESET = "auth/reset"

    fun savePost(postId: String? = null): String =
        if (postId == null) "save_post" else "save_post?postId=$postId"

    fun userProfile(userId: String) = "user_profile/$userId"
}