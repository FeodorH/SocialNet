package com.example.socialnet.presentation.common

import android.content.Context
import com.example.socialnet.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiStrings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun profileUpdated(): String = context.getString(R.string.profile_updated)
    fun postDeleted(): String = context.getString(R.string.post_deleted)
    fun avatarUpdated(): String = context.getString(R.string.avatar_updated)
    fun avatarDeleted(): String = context.getString(R.string.avatar_deleted)
    fun emailVerificationPending(): String = context.getString(R.string.email_verification_pending)
}