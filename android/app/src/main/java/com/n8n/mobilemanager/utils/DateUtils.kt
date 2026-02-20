package com.n8n.mobilemanager.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.Duration

object DateUtils {
    private val formatterHHmm = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    private val formatterFull = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss").withZone(ZoneId.systemDefault())

    fun parseInstant(dateString: String?): Instant? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            Instant.parse(if (dateString.endsWith("Z") || dateString.contains("+")) dateString else "${dateString}Z")
        } catch (e: Exception) {
            null
        }
    }

    fun formatTime(dateString: String?): String {
        val instant = parseInstant(dateString) ?: return ""
        return formatterHHmm.format(instant)
    }

    fun formatFullDate(dateString: String?): String {
        val instant = parseInstant(dateString) ?: return ""
        return formatterFull.format(instant)
    }

    fun calculateDuration(start: String?, end: String?): String {
        val startInstant = parseInstant(start) ?: return ""
        val endInstant = parseInstant(end) ?: return ""

        val duration = Duration.between(startInstant, endInstant)
        val millis = duration.toMillis()

        return when {
            millis < 1000 -> "${millis}ms"
            millis < 60000 -> "${millis / 1000}s"
            else -> "${millis / 60000}m ${(millis % 60000) / 1000}s"
        }
    }
}
