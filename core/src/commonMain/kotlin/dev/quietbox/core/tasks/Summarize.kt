package dev.quietbox.core.tasks

import dev.quietbox.core.ai.AiTask
import dev.quietbox.core.ai.Effort
import dev.quietbox.core.ai.Privacy
import dev.quietbox.core.ai.Scored
import dev.quietbox.core.ai.TaskSpec
import dev.quietbox.core.ai.Trigger
import dev.quietbox.core.ai.objectSchema
import dev.quietbox.core.ai.stringSchema
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.local.Sensitivity
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Summary(val tldr: String)

data class SummaryInput(val message: Message, val context: List<Message>)

object SummarizeTask : AiTask<SummaryInput, Summary> {

    private const val MIN_WORDS = 60

    override val spec = TaskSpec(
        name = "summarize",
        promptVersion = 2,
        trigger = Trigger.ON_OPEN,
        privacy = Privacy.CLOUD_OK,
        latencyBudget = 6.seconds,
        maxOutputTokens = 160,
        cloudEffort = Effort.MEDIUM,
        escalateBelow = 0.6,
        showAbove = 0.5,
    )

    override val serializer = Summary.serializer()

    override val instructions = """
        Write a one-sentence TL;DR (max 25 words) of the MESSAGE for its recipient.
        Lead with what the recipient needs to know or do. Use CONTEXT only to resolve references
        like "as discussed"; never summarize the context itself. No greeting, no "This email".
    """.trimIndent()

    override val outputSchema = objectSchema("tldr" to stringSchema("Max 25 words"))

    fun worthIt(message: Message) = message.body.split(Regex("\\s+")).size >= MIN_WORDS

    override fun id(input: SummaryInput) = input.message.id

    override fun render(input: SummaryInput) = buildString {
        if (input.context.isNotEmpty()) {
            appendLine("CONTEXT:")
            input.context.forEach { appendLine("- ${it.subject}: ${it.body.take(280)}") }
            appendLine()
        }
        appendLine("MESSAGE from ${input.message.from}, subject: ${input.message.subject}")
        append(input.message.body)
    }

    override fun privacy(input: SummaryInput) = Sensitivity.of(input.message)

    override fun validate(input: SummaryInput, output: Summary): List<String> = buildList {
        val words = output.tldr.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) add("tldr is empty")
        if (words.size > 30) add("tldr has ${words.size} words, max 25")
        if (output.tldr.lowercase().startsWith("this email")) add("tldr starts with 'This email'")
    }

    override fun onDevice(input: SummaryInput): Scored<Summary> {
        val first = input.message.body
            .split(Regex("(?<=[.!?])\\s+"))
            .firstOrNull { sentence -> sentence.split(" ").size > 5 && GREETING.none { sentence.lowercase().startsWith(it) } }
            ?: input.message.subject
        return Scored(Summary(first.split(" ").take(25).joinToString(" ")), 0.45)
    }

    private val GREETING = listOf("hi", "hello", "dear", "hey", "hallo", "i hope")
}
