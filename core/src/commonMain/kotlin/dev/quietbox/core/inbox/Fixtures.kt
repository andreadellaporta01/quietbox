package dev.quietbox.core.inbox

import dev.quietbox.core.tasks.ActionItem
import dev.quietbox.core.tasks.ActionType
import dev.quietbox.core.tasks.Category
import kotlinx.datetime.LocalDateTime

data class Golden(
    val category: Category,
    val urgent: Boolean,
    val actions: List<ActionItem>,
    val tldr: String? = null,
    val replies: List<String>? = null,
    val reason: String,
    val alsoOk: List<Pair<ActionType, String>> = emptyList(),
)

data class Fixture(val message: Message, val golden: Golden)

object Fixtures {

    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime(2026, 10, day, hour, minute)

    private fun action(type: ActionType, title: String, date: String?, time: String?, amount: String?, evidence: String) =
        ActionItem(type, title, date, time, amount, evidence)

    val all: List<Fixture> = listOf(
        Fixture(
            Message(
                "m01", "Hausverwaltung Schmidt <buchhaltung@hv-schmidt.de>", "Nebenkostenabrechnung 2025",
                "Sehr geehrte Mieterin, sehr geehrter Mieter,\n\nanbei erhalten Sie die Nebenkostenabrechnung für 2025. " +
                    "Es ergibt sich eine Nachzahlung von 184,50 €. Bitte überweisen Sie den Betrag bis zum 15.10.2026 auf das bekannte Konto.\n\n" +
                    "Mit freundlichen Grüßen\nHausverwaltung Schmidt",
                at(5, 8, 12),
            ),
            Golden(
                Category.ACTION, false,
                listOf(action(ActionType.PAYMENT, "Pay utilities top-up", "2026-10-15", null, "184,50 €", "Nachzahlung von 184,50 €")),
                reason = "payment due by 15 October",
            ),
        ),
        Fixture(
            Message(
                "m02", "Lena Vogel <lena.vogel@kiezlabs.io>", "retro tomorrow?",
                "Hey! Are you joining the sprint retro tomorrow at 10:00? If not, can you drop your notes in the doc before? Thanks!",
                at(5, 17, 40),
            ),
            Golden(
                Category.REPLY, true,
                listOf(action(ActionType.EVENT, "Sprint retro", "2026-10-06", "10:00", null, "sprint retro tomorrow at 10:00")),
                replies = listOf("Yes, I'll be there!", "Can't make it, notes in doc.", "Is it in the usual room?"),
                reason = "Lena is waiting for a yes or no",
                alsoOk = listOf(ActionType.DEADLINE to "2026-10-06"),
            ),
        ),
        Fixture(
            Message(
                "m03", "Urban Sneakers <newsletter@urbansneakers.shop>", "🔥 30% off everything — 48h only",
                "Our biggest sale of the season is here. 30% off everything, 48 hours only. Unsubscribe here if you no longer want these emails.",
                at(5, 9, 0),
            ),
            Golden(Category.NOISE, false, emptyList(), reason = "marketing newsletter"),
        ),
        Fixture(
            Message(
                "m04", "Praxis Dr. Keller <termine@praxis-keller.de>", "Your blood test results",
                "Hello, your blood test results from 29 September are ready. Dr. Keller would like to discuss them with you. " +
                    "Please book a follow-up appointment within the next two weeks via our online calendar or by phone.",
                at(5, 11, 5),
            ),
            Golden(Category.ACTION, false, emptyList(), reason = "doctor asks to book a follow-up"),
        ),
        Fixture(
            Message(
                "m05", "Jira <jira@kiezlabs.atlassian.net>", "[QB-412] Status changed: In Review → Done",
                "Marco Rossi changed the status of QB-412 'Offline banner flickers on resume' from In Review to Done.",
                at(5, 14, 22),
            ),
            Golden(Category.FYI, false, emptyList(), reason = "automated ticket update"),
        ),
        Fixture(
            Message(
                "m06", "DHL Paket <noreply@dhl.de>", "Ihre Sendung kommt morgen",
                "Gute Nachrichten: Ihre Sendung 00340434161094042557 wird morgen zwischen 10:00 und 14:00 zugestellt. " +
                    "Sie müssen nichts weiter tun.",
                at(5, 19, 3),
            ),
            Golden(
                Category.FYI, false,
                listOf(action(ActionType.DELIVERY, "DHL parcel arrives", "2026-10-06", "10:00", null, "morgen zwischen 10:00 und 14:00 zugestellt")),
                reason = "delivery notice, nothing to do",
            ),
        ),
        Fixture(
            Message(
                "m07", "next.app devcon <speakers@nextapp-devcon.berlin>", "Please confirm your workshop room setup",
                "Hi Andrea, hi Alessandro, please confirm your workshop room setup (projector, HDMI, power strips) by Friday 9 October. " +
                    "If we don't hear back we'll assume the standard setup.",
                at(6, 9, 30),
            ),
            Golden(
                Category.ACTION, true,
                listOf(action(ActionType.DEADLINE, "Confirm workshop room setup", "2026-10-09", null, null, "by Friday 9 October")),
                reason = "confirm room setup by Friday",
            ),
        ),
        Fixture(
            Message(
                "m08", "Giulia Marino <giulia.marino@gmail.com>", "cena sabato?",
                "Hey, we're doing pizza at ours on Saturday around 8pm, Marta and Paul are coming too. You in? Bring nothing, just yourself 🙂",
                at(6, 12, 10),
            ),
            Golden(
                Category.REPLY, false,
                listOf(action(ActionType.EVENT, "Pizza at Giulia's", "2026-10-10", "20:00", null, "on Saturday around 8pm")),
                replies = listOf("I'm in, see you Saturday!", "Can't this time, next one?", "What time should I come?"),
                reason = "friend invites you to dinner",
            ),
        ),
        Fixture(
            Message(
                "m09", "Sofia Brandt <sofia.brandt@kiezlabs.io>", "Re: 2.0 release plan — where we landed",
                "Hi all, quick wrap-up after yesterday's call so we're aligned before the board update. We agreed to move the 2.0 release " +
                    "from 14 October to 21 October because the payment SDK migration is not certified yet. Marco owns the certification and " +
                    "will report on Thursday. Design freeze stays on 12 October, so please don't open new UI tickets after that date. " +
                    "QA needs the release candidate by 16 October at the latest. Andrea, as discussed, can you send the updated timeline " +
                    "to the client? Nothing else changes for now. Thanks everyone, Sofia",
                at(6, 16, 45),
            ),
            Golden(
                Category.ACTION, false,
                listOf(action(ActionType.DEADLINE, "Release candidate to QA", "2026-10-16", null, null, "release candidate by 16 October")),
                tldr = "Release slips to 21 October; you need to send the client the updated timeline. Design freeze 12 Oct, RC due 16 Oct.",
                reason = "Sofia asks you to update the client",
                alsoOk = listOf(ActionType.DEADLINE to "2026-10-12", ActionType.DEADLINE to "2026-10-21", ActionType.EVENT to "2026-10-21"),
            ),
        ),
        Fixture(
            Message(
                "m10", "N26 <no-reply@n26.com>", "Your verification code",
                "Your one-time code is 482 913. It expires in 5 minutes. Never share this code with anyone, including N26 staff.",
                at(6, 18, 2),
            ),
            Golden(Category.FYI, false, emptyList(), reason = "one-time code"),
        ),
        Fixture(
            Message(
                "m11", "Hetzner Online <billing@hetzner.com>", "Invoice R0024518 — paid",
                "Dear customer, your invoice R0024518 for October amounts to €23.80 and has been charged to your credit card. " +
                    "No action is required.",
                at(7, 7, 15),
            ),
            Golden(Category.FYI, false, emptyList(), reason = "invoice already paid"),
        ),
        Fixture(
            Message(
                "m12", "Jonas Weber <jonas@talentbridge.de>", "Senior KMP role — 15 min chat?",
                "Hi Andrea, I came across your KMP talks and I'm working on a senior mobile role in Berlin. Would you be open to a 15 minute chat " +
                    "sometime next week? No pressure at all.",
                at(7, 10, 0),
            ),
            Golden(
                Category.REPLY, false, emptyList(),
                replies = listOf("Sure, happy to chat next week.", "Thanks, not looking right now.", "Can you share the role details?"),
                reason = "recruiter asks for a chat",
            ),
        ),
        Fixture(
            Message(
                "m13", "Deutsche Bahn <buchung@bahn.de>", "Buchungsbestätigung ICE 1006",
                "Ihre Fahrt: Berlin Hbf → München Hbf, ICE 1006, am 2026-10-12, Abfahrt 08:34, Wagen 9, Platz 74. Gute Reise!",
                at(7, 12, 30),
            ),
            Golden(
                Category.FYI, false,
                listOf(action(ActionType.EVENT, "ICE 1006 to Munich", "2026-10-12", "08:34", null, "am 2026-10-12, Abfahrt 08:34")),
                reason = "train booking confirmation",
            ),
        ),
        Fixture(
            Message(
                "m14", "DevTools Weekly <info@devtoolsweekly.io>", "Webinar: AI agents for mobile (free seat)",
                "Join our free webinar on AI agents for mobile next Thursday. Seats are limited, reserve yours now! Unsubscribe anytime.",
                at(7, 13, 0),
            ),
            Golden(Category.NOISE, false, emptyList(), reason = "promotional webinar"),
        ),
        Fixture(
            Message(
                "m15", "Thomas Richter <t.richter@nordwind-logistik.de>", "Estimate for the driver app offline mode",
                "Hi Andrea, the board meets tomorrow morning. Could you send me a rough estimate for the offline mode of the driver app " +
                    "by end of day today? A range in weeks is fine.",
                at(7, 15, 20),
            ),
            Golden(
                Category.REPLY, true,
                listOf(action(ActionType.DEADLINE, "Send offline-mode estimate", "2026-10-07", null, null, "by end of day today")),
                replies = listOf("Sure, sending a range tonight.", "Can it wait until tomorrow 9am?", "Which features are in scope?"),
                reason = "client needs an estimate today",
                alsoOk = listOf(ActionType.EVENT to "2026-10-08"),
            ),
        ),
        Fixture(
            Message(
                "m16", "Kita Sonnenblume <info@kita-sonnenblume.de>", "Elternabend am 09.10.2026",
                "Liebe Eltern, wir laden euch herzlich zum Elternabend am 09.10.2026 um 19:00 Uhr ein. Bitte gebt bis Mittwoch Bescheid, " +
                    "ob ihr kommt.",
                at(7, 16, 0),
            ),
            Golden(
                Category.ACTION, false,
                listOf(action(ActionType.EVENT, "Kita parents' evening", "2026-10-09", "19:00", null, "Elternabend am 09.10.2026 um 19:00")),
                reason = "RSVP for parents' evening",
                alsoOk = listOf(ActionType.DEADLINE to "2026-10-14", ActionType.DEADLINE to "2026-10-08"),
            ),
        ),
    )

    val inbox: List<Message> = all.map { it.message }.sortedByDescending { it.receivedAt }

    val threadContext: List<Message> = listOf(
        Message(
            "c01", "Sofia Brandt <sofia.brandt@kiezlabs.io>", "2.0 release plan",
            "Agenda for tomorrow's call: payment SDK certification status, release date, design freeze, QA window for the 2.0 release.",
            at(5, 10, 0),
        ),
    )

    fun goldenFor(message: Message): Golden? = all.firstOrNull { it.message.id == message.id }?.golden

    fun bySubject(subject: String): Fixture? = all.firstOrNull { it.message.subject == subject }
}
