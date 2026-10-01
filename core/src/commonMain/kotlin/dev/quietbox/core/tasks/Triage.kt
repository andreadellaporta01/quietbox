package dev.quietbox.core.tasks

import dev.quietbox.core.ai.AiTask
import dev.quietbox.core.ai.Effort
import dev.quietbox.core.ai.Privacy
import dev.quietbox.core.ai.Scored
import dev.quietbox.core.ai.TaskSpec
import dev.quietbox.core.ai.Trigger
import dev.quietbox.core.ai.enumSchema
import dev.quietbox.core.ai.objectSchema
import dev.quietbox.core.ai.stringSchema
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.local.Sensitivity
import dev.quietbox.core.local.Text
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration.Companion.seconds

enum class Category { ACTION, REPLY, FYI, NOISE }

@Serializable
data class Triage(val category: Category, val urgent: Boolean, val reason: String)

object TriageTask : AiTask<Message, Triage> {

    override val spec = TaskSpec(
        name = "triage",
        promptVersion = 4,
        trigger = Trigger.BACKGROUND,
        privacy = Privacy.CLOUD_OK,
        latencyBudget = 8.seconds,
        maxOutputTokens = 120,
        cloudEffort = Effort.LOW,
        escalateBelow = 0.7,
        showAbove = 0.5,
    )

    override val serializer = Triage.serializer()

    override val instructions = """
        You sort one inbox message into exactly one category:
        ACTION = the reader must do something concrete (pay, sign, attend, confirm, submit).
        REPLY = a person is waiting for an answer from the reader.
        FYI = useful to know, nothing to do. Receipts, confirmations and anything saying "no action is required" are FYI.
        NOISE = marketing, newsletters, automated chatter.
        urgent = true only if there is a deadline within 3 days of the received date.
        reason: at most 12 words, no preamble.
    """.trimIndent()

    override val outputSchema = objectSchema(
        "category" to enumSchema(Category.entries),
        "urgent" to buildJsonObject { put("type", JsonPrimitive("boolean")) },
        "reason" to stringSchema("At most 12 words"),
    )

    override fun id(input: Message) = input.id

    override fun render(input: Message) =
        "Received: ${input.receivedAt}\nFrom: ${input.from}\nSubject: ${input.subject}\n\n${input.body}"

    override fun privacy(input: Message) = Sensitivity.of(input)

    override fun validate(input: Message, output: Triage): List<String> = buildList {
        if (output.reason.isBlank()) add("reason is empty")
        if (output.reason.split(" ").size > 16) add("reason longer than 12 words")
    }

    override fun onDevice(input: Message): Scored<Triage> {
        val text = input.fullText
        return when {
            input.isAutomated && Text.containsAny(text, NOISE) ->
                Scored(Triage(Category.NOISE, false, "automated marketing"), 0.9)
            Text.containsAny(text, ACTION) ->
                Scored(Triage(Category.ACTION, Text.containsAny(text, URGENT), "contains a request to act"), 0.75)
            !input.isAutomated && text.contains('?') ->
                Scored(Triage(Category.REPLY, Text.containsAny(text, URGENT), "a person asked a question"), 0.65)
            input.isAutomated ->
                Scored(Triage(Category.FYI, false, "automated notification"), 0.6)
            else -> Scored(Triage(Category.FYI, false, "no clear signal"), 0.4)
        }
    }

    private val NOISE = listOf("% off", "sale", "unsubscribe", "newsletter", "deal", "offer", "discount", "webinar", "angebot")
    private val ACTION = listOf("please pay", "payment due", "sign ", "please confirm", "deadline", "submit", "renew", "please book", "bescheid", "fällig", "action required", "rsvp")
    private val URGENT = listOf("today", "tomorrow", "asap", "urgent", "by friday", "heute", "morgen", "final notice")
}
