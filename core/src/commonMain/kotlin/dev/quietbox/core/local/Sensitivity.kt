package dev.quietbox.core.local

import dev.quietbox.core.ai.Privacy
import dev.quietbox.core.inbox.Message

object Sensitivity {
    private val SENSITIVE = listOf(
        "diagnosis", "blood test", "lab results", "prescription", "therapy", "medical", "doctor",
        "password", "one-time code", "verification code", "otp", "iban", "pin ", "salary", "payslip",
        "lawyer", "custody", "divorce", "befund", "arzt",
    )

    fun of(message: Message): Privacy =
        if (Text.containsAny(message.fullText, SENSITIVE)) Privacy.LOCAL_ONLY else Privacy.CLOUD_OK
}
