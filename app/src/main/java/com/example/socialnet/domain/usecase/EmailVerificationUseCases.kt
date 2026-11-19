package com.example.socialnet.domain.usecase

import com.example.socialnet.core.common.AppResult
import com.example.socialnet.domain.repository.common.OfflineFirstAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEmailVerifiedUseCase @Inject constructor(
    private val auth: OfflineFirstAuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = auth.observeEmailVerified()
}

class ReloadCurrentUserUseCase @Inject constructor(
    private val auth: OfflineFirstAuthRepository,
) {
    suspend operator fun invoke(): AppResult<Boolean> = auth.reloadCurrentUser()
}

class SendEmailVerificationUseCase @Inject constructor(
    private val auth: OfflineFirstAuthRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = auth.sendEmailVerification()
}