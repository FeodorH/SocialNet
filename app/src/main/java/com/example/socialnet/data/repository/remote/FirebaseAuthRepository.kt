package com.example.socialnet.data.repository.remote

import com.example.socialnet.core.common.AppError
import com.example.socialnet.core.common.AppResult
import com.example.socialnet.core.model.User
import com.example.socialnet.core.utils.AppDispatchers
import com.example.socialnet.data.dto.UserResponse
import com.example.socialnet.data.mapper.toDomain
import com.example.socialnet.domain.repository.remote.AuthRepository
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val dispatchers: AppDispatchers
) : AuthRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)

        awaitClose { auth.removeAuthStateListener(listener) }
    }.flowOn(dispatchers.io)

    override suspend fun register(email: String, password: String, name: String): AppResult<User> =
        withContext(dispatchers.io) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                    ?: return@withContext AppResult.Failure(AppError.Unknown())

                runCatching { firebaseUser.sendEmailVerification().await() }

                val uid = firebaseUser.uid
                val userRef = firestore.collection("users").document(uid)

                runCatching {
                    userRef.set(
                        mapOf(
                            "email" to email,
                            "name" to name,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        )
                    ).await()
                }

                // Возвращаем User даже если Firestore-запись упала
                val userResponse = UserResponse(
                    id = uid,
                    email = email,
                    name = name,
                    avatarUrl = null,
                    avatarPublicId = null,
                    bio = null,
                    createdAt = Timestamp.now(),
                )

                AppResult.Success(userResponse.toDomain())
            } catch (e: Exception) {
                AppResult.Failure(mapAuthException(e))
            }
        }

    override suspend fun login(email: String, password: String): AppResult<User> =
        withContext(dispatchers.io) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val uid =
                    authResult.user?.uid ?: return@withContext AppResult.Failure(AppError.Unknown())

                val snapshot = firestore.collection("users").document(uid).get().await()
                val userResponse = snapshot.toObject(UserResponse::class.java)

                if (userResponse != null) {
                    AppResult.Success(userResponse.toDomain())
                } else {
                    AppResult.Failure(AppError.NotFound)
                }
            } catch (e: Exception) {
                AppResult.Failure(mapAuthException(e))
            }
        }

    override suspend fun logout(): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            auth.signOut()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e))
        }
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            try {
                auth.sendPasswordResetEmail(email).await()
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Failure(mapAuthException(e))
            }
        }

    private fun mapAuthException(e: Exception): AppError {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> AppError.Validation(AppError.Validation.Field.PASSWORD, "Слишком слабый пароль")
            is FirebaseAuthUserCollisionException -> AppError.Validation(AppError.Validation.Field.EMAIL, "Пользователь с таким email уже существует")
            is FirebaseAuthInvalidCredentialsException -> AppError.Unauthorized
            is IOException -> AppError.Network
            else -> AppError.Unknown(e)
        }
    }

    override fun observeEmailVerified(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.IdTokenListener { auth ->
            trySend(auth.currentUser?.isEmailVerified ?: false)
        }
        auth.addIdTokenListener(listener)
        awaitClose { auth.removeIdTokenListener(listener) }
    }.flowOn(dispatchers.io)

    override suspend fun reloadCurrentUser(): AppResult<Boolean> =
        withContext(dispatchers.io) {
            try {
                val user = auth.currentUser
                    ?: return@withContext AppResult.Failure(AppError.Unauthorized)
                user.reload().await()
                // Принудительное обновление токена — иначе Firestore
                // продолжит использовать старый токен с email_verified=false
                user.getIdToken(true).await()
                AppResult.Success(user.isEmailVerified)
            } catch (e: Exception) {
                AppResult.Failure(AppError.Unknown(e))
            }
        }

    override suspend fun sendEmailVerification(): AppResult<Unit> =
        withContext(dispatchers.io) {
            try {
                val user = auth.currentUser
                    ?: return@withContext AppResult.Failure(AppError.Unauthorized)
                user.sendEmailVerification().await()
                AppResult.Success(Unit)
            } catch (e: FirebaseAuthException) {
                if (e.errorCode == "ERROR_TOO_MANY_REQUESTS") {
                    AppResult.Failure(
                        AppError.Server("RATE_LIMITED", "Слишком часто. Подождите минуту.")
                    )
                } else {
                    AppResult.Failure(AppError.Unknown(e))
                }
            } catch (e: Exception) {
                AppResult.Failure(AppError.Unknown(e))
            }
        }
}