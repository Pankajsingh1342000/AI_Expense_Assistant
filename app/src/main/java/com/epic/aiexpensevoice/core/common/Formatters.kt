package com.epic.aiexpensevoice.core.common

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun Double.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(this)

fun nowLabel(): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

fun String.toDisplayDateLabel(): String {
    val dateTime = toLocalDateTimeOrNull()
    val date = dateTime?.toLocalDate() ?: toLocalDateOrNull()
    val today = LocalDate.now()

    return when {
        dateTime != null && date == today -> dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        date != null && date == today -> "Today"
        date != null && date == today.minusDays(1) -> "Yesterday"
        date != null && date.year == today.year -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
        date != null -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        else -> this.take(16)
    }
}

fun String.toLocalDateOrNull(): LocalDate? = toLocalDateTimeOrNull()?.toLocalDate() ?: runCatching {
    if (length >= 10) LocalDate.parse(take(10)) else null
}.getOrNull()

fun String.toLocalDateTimeOrNull(): LocalDateTime? = runCatching {
    when {
        contains("T") && (contains("+") || contains("Z")) -> OffsetDateTime.parse(this)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        contains("T") -> LocalDateTime.parse(this)
            .atZone(java.time.ZoneId.of("UTC"))
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        else -> null
    }
}.getOrNull()
