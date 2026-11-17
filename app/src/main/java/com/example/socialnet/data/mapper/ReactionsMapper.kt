package com.example.socialnet.data.mapper

import com.example.socialnet.core.model.ReactionSummary
import com.example.socialnet.core.model.ReactionType

/**
 * Конвертация Map из Firestore в доменную модель ReactionSummary.
 * Безопасно парсит ключи Enum, игнорируя неизвестные значения.
 */
fun Map<String, Long>?.toReactionSummary(total: Long): ReactionSummary {
    if (this == null) return ReactionSummary.EMPTY

    val byType = this.mapNotNull { (key, value) ->
        // Пытаемся найти Enum по имени (например, "LIKE" -> ReactionType.LIKE)
        val type = runCatching { ReactionType.valueOf(key.uppercase()) }.getOrNull()
        if (type != null) type to value.toInt() else null
    }.toMap()

    return ReactionSummary(
        total = total.toInt(),
        byType = byType
    )
}
