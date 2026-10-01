package dev.quietbox.core.inbox

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val from: String,
    val subject: String,
    val body: String,
    val receivedAt: LocalDateTime,
) {
    val senderName: String get() = from.substringBefore(" <").trim()
    val senderAddress: String get() = from.substringAfter("<", from).removeSuffix(">").trim()
    val isAutomated: Boolean
        get() = AUTOMATED_HINTS.any { senderAddress.lowercase().contains(it) }
    val fullText: String get() = "$subject\n$body"

    private companion object {
        val AUTOMATED_HINTS = listOf("noreply", "no-reply", "notification", "newsletter", "promo", "automated", "info@", "billing@", "jira@", "shipment", "buchung")
    }
}
