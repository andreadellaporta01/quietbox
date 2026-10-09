package dev.quietbox.core.tasks

import dev.quietbox.core.ai.AiTask
import dev.quietbox.core.ai.Effort
import dev.quietbox.core.ai.Privacy
import dev.quietbox.core.ai.Scored
import dev.quietbox.core.ai.TaskSpec
import dev.quietbox.core.ai.Trigger
import dev.quietbox.core.ai.arraySchema
import dev.quietbox.core.ai.enumSchema
import dev.quietbox.core.ai.objectSchema
import dev.quietbox.core.ai.stringSchema
import dev.quietbox.core.inbox.Message
import dev.quietbox.core.local.Sensitivity
import dev.quietbox.core.local.Text
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlin.time.Duration.Companion.seconds

enum class ActionType { EVENT, PAYMENT, DEADLINE, DELIVERY }

@Serializable
data class ActionItem(
    val type: ActionType,
    val title: String,
    val date: String?,
    val time: String?,
    val amount: String?,
    val evidence: String,
)

@Serializable
data class Extraction(val actions: List<ActionItem>)

object ExtractTask : AiTask<Message, Extraction> {

    override val spec = TaskSpec(
        name = "extract",
        promptVersion = 5,
        trigger = Trigger.BACKGROUND,
        privacy = Privacy.CLOUD_OK,
        latencyBudget = 10.seconds,
        maxOutputTokens = 400,
        cloudEffort = Effort.LOW,
        escalateBelow = 0.8,
        showAbove = 0.6,
    )

    override val serializer = Extraction.serializer()

    override val instructions = """
        Extract at most 3 things the reader could put in a calendar, pay, or track.
        date: ISO yyyy-MM-dd, resolved against the received date; null if the message has no date.
        time: HH:mm 24h or null. amount: copy it exactly as written, or null.
        evidence: a short verbatim quote from the message that proves the item. Never paraphrase it.
        If there is nothing actionable, return an empty list. Do not invent dates.
    """.trimIndent()

    private val nullableString = buildJsonObject {
        putJsonArray("type") { add("string"); add("null") }
    }

    override val outputSchema = objectSchema(
        "actions" to arraySchema(
            objectSchema(
                "type" to enumSchema(ActionType.entries),
                "title" to stringSchema("At most 6 words"),
                "date" to nullableString,
                "time" to nullableString,
                "amount" to nullableString,
                "evidence" to stringSchema("Verbatim quote from the message"),
            ),
        ),
    )

    override fun id(input: Message) = input.id

    override fun render(input: Message) =
        "Received: ${input.receivedAt.date}\nFrom: ${input.from}\nSubject: ${input.subject}\n\n${input.body}"

    override fun privacy(input: Message) = Sensitivity.of(input)

    // LAB-3 · Guard. The schema guarantees the shape, not the truth. Return every reason this
    // extraction is unacceptable; an empty list means "ship it". The reasons are also sent back to
    // the model as a repair prompt, so make them specific, e.g. "actions[0].evidence is not a quote from the message".
    //
    // Rules (output.actions is a List<ActionItem>: type, title, date?, time?, amount?, evidence):
    //   1. at most 3 actions                                          rejectsMoreThanThreeItems
    //   2. date, if present, parses with LocalDate.parse  (yyyy-MM-dd)  rejectsDatesThatAreNotIso
    //      time, if present, parses with LocalTime.parse  (HH:mm)
    //   3. evidence is a verbatim quote from input.fullText            rejectsEvidenceThatIsNotAQuote
    //   4. amount, if present, literally appears in input.fullText     rejectsAnAmountThatIsNotInTheMessage
    //   (and a grounded item passes: acceptsAGroundedItem)
    //
    // Compare Text.normalize(input.fullText) with Text.normalize(...) of the quote, never raw strings:
    // whitespace and punctuation differ. runCatching { LocalDate.parse(it) }.isFailure is the date check.
    // Shape:  = buildList { if (output.actions.size > 3) add("more than 3 actions"); output.actions.forEachIndexed { i, a -> ... } }
    override fun validate(input: Message, output: Extraction): List<String> =
        TODO("LAB-3: reject anything the message does not literally support.")

    override fun onDevice(input: Message): Scored<Extraction> {
        val body = input.fullText
        val amount = AMOUNT.find(body)?.value
        val isoDate = ISO_DATE.find(body)?.value
        val euDate = EU_DATE.find(body)?.let { m ->
            val (d, mo, y) = m.destructured
            runCatching { LocalDate(y.toInt(), mo.toInt(), d.toInt()).toString() }.getOrNull()
        }
        val date = isoDate ?: euDate
        val actions = buildList {
            if (amount != null) {
                add(ActionItem(ActionType.PAYMENT, "Pay ${input.senderName}", date, null, amount, amount))
            } else if (date != null) {
                add(ActionItem(ActionType.EVENT, input.subject.take(40), date, null, null, isoDate ?: EU_DATE.find(body)!!.value))
            }
        }
        val confidence = when {
            actions.isEmpty() && !Text.containsAny(body, TEMPORAL) -> 0.85
            actions.isEmpty() -> 0.3
            amount != null && date != null -> 0.8
            else -> 0.5
        }
        return Scored(Extraction(actions), confidence)
    }

    private val AMOUNT = Regex("""(€\s?\d+(?:[.,]\d{2})?|\d+(?:[.,]\d{2})?\s?(?:EUR|€)|\$\d+(?:\.\d{2})?)""")
    private val ISO_DATE = Regex("""\b20\d{2}-\d{2}-\d{2}\b""")
    private val EU_DATE = Regex("""\b(\d{1,2})\.(\d{1,2})\.(20\d{2})\b""")
    private val TEMPORAL = listOf(
        "tomorrow", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "next week", "tonight", " at ", "o'clock", "pm", "am ", "deadline", "due", "january", "february", "march",
        "april", "may ", "june", "july", "august", "september", "october", "november", "december", "morgen",
    )
}
