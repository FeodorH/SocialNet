package com.example.socialnet.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.socialnet.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import androidx.compose.ui.platform.LocalLocale

@Composable
fun formatRelativeTime(instant: Instant): String {
    val now = kotlin.time.Clock.System.now()
    val diffSeconds = (now - instant).inWholeSeconds.coerceAtLeast(0)

    return when {
        diffSeconds < 60 ->
            stringResource(R.string.right_now)

        diffSeconds < 3_600 -> {
            val minutes = (diffSeconds / 60).toInt()
            pluralStringResource(
                R.plurals.minutes_ago,
                minutes,
                minutes
            )
        }

        diffSeconds < 86_400 -> {
            val hours = (diffSeconds / 3_600).toInt()
            pluralStringResource(
                R.plurals.hours_ago,
                hours,
                hours
            )
        }

        diffSeconds < 172_800 ->
            stringResource(R.string.yesterday)

        diffSeconds < 604_800 -> {
            val days = (diffSeconds / 86_400).toInt()
            pluralStringResource(
                R.plurals.days_ago,
                days,
                days
            )
        }

        else -> {
            val date = instant.toJavaInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            date.format(
                DateTimeFormatter.ofPattern(
                    "d MMM",
                    LocalLocale.current.platformLocale
                )
            )
        }
    }
}