package com.example.socialnet.data.mapper

import com.google.firebase.Timestamp
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Конвертация из Firebase Timestamp в доменный kotlin.time.Instant.
 * Если пришел null (например, сервер еще не обработал serverTimestamp),
 * берем текущее время (fallback).
 */
fun Timestamp?.toDomainInstant(): Instant {
    return this?.let { Instant.fromEpochMilliseconds(it.toDate().time) }
        ?: Clock.System.now()
}
