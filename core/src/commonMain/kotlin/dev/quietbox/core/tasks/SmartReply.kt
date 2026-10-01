package dev.quietbox.core.tasks

import dev.quietbox.core.ai.AiTask
import dev.quietbox.core.ai.Effort
import dev.quietbox.core.ai.Privacy
import dev.quietbox.core.ai.Scored
import dev.quietbox.core.ai.TaskSpec
import dev.quietbox.core.ai.Trigger
import dev.quietbox.core.ai.arraySchema
import dev.quietbox.core.ai.objectSchema
import dev.quietbox.core.ai.stringSchema
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.local.Sensitivity
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Replies(val options: List<String>)

object SmartReplyTask : AiTask<Message, Replies> {

    override val spec = TaskSpec(
        name = "reply",
        promptVersion = 4,
        trigger = Trigger.ON_OPEN,
        privacy = Privacy.CLOUD_OK,
        latencyBudget = 3.seconds,
        maxOutputTokens = 120,
        cloudEffort = Effort.LOW,
        escalateBelow = 0.75,
        showAbove = 0.5,
    )

    override val serializer = Replies.serializer()

    override val instructions = """
        Suggest exactly 3 short replies (max 8 words each) the recipient could send as-is.
        Make them meaningfully different: one yes, one no or alternative, one asking for what is missing.
        Write in the language of the message. Never promise money, dates or commitments not in the message.
    """.trimIndent()

    override val outputSchema = objectSchema("options" to arraySchema(stringSchema("Max 8 words")))

    override fun id(input: Message) = input.id

    override fun render(input: Message) = "From: ${input.from}\nSubject: ${input.subject}\n\n${input.body}"

    override fun privacy(input: Message) = Sensitivity.of(input)

    override fun validate(input: Message, output: Replies): List<String> = buildList {
        if (output.options.size != 3) add("expected 3 options, got ${output.options.size}")
        output.options.forEachIndexed { i, option ->
            if (option.split(" ").size > 10) add("options[$i] is longer than 8 words")
        }
        if (output.options.map { it.lowercase().trim() }.toSet().size < output.options.size) add("options are duplicated")
    }

    override fun onDevice(input: Message): Scored<Replies>? {
        if (input.isAutomated) return null
        val german = GERMAN.count { input.body.lowercase().contains(it) } >= 2
        val options = if (german) {
            listOf("Ja, passt!", "Leider nicht, sorry.", "Kannst du mehr Details schicken?")
        } else {
            listOf("Sounds good, thanks!", "Sorry, I can't make it.", "Could you share more details?")
        }
        return Scored(Replies(options), 0.55)
    }

    private val GERMAN = listOf(" und ", " ich ", " wir ", " nicht ", " bitte", " danke", " ist ")
}
