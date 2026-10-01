package dev.quietbox.proxy

import dev.quietbox.core.cloud.CloudFailure
import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.CloudRequest
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val log = LoggerFactory.getLogger("quietbox")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8787
    val token = System.getenv("QUIETBOX_TOKEN").orEmpty().ifBlank { error("set QUIETBOX_TOKEN (the code you put on the slide)") }
    val model = System.getenv("QUIETBOX_MODEL") ?: "claude-opus-5-5"
    val fallbacks = System.getenv("QUIETBOX_FALLBACKS")?.toBoolean() ?: true
    log.info("QuietBox proxy on :$port · model=$model · fallbacks=$fallbacks")
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module(Claude(model, fallbacks), token) }.start(wait = true)
}

fun Application.module(cloud: CloudModel, token: String, perMinute: Int = 120, roomPerMinute: Int = 2_000) {
    install(ContentNegotiation) { json() }
    install(CallLogging)
    val windows = ConcurrentHashMap<String, Pair<Long, AtomicLong>>()
    val attendees = ConcurrentHashMap.newKeySet<String>()
    val tokensServed = AtomicLong()

    routing {
        get("/health") { call.respondText("ok · ${cloud.name} · ${attendees.size} attendees · ${tokensServed.get()} tokens served") }

        post("/v1/task") {
            if (call.request.header("Authorization") != "Bearer $token") {
                return@post call.respond(HttpStatusCode.Unauthorized, "bad token")
            }
            val attendee = call.request.header("X-Attendee") ?: call.request.local.remoteAddress
            val minute = System.currentTimeMillis() / 60_000
            fun hit(key: String) = windows.compute(key) { _, old -> if (old?.first == minute) old else minute to AtomicLong() }!!.second.incrementAndGet()
            if (hit("attendee:$attendee") > perMinute || hit("room") > roomPerMinute) {
                return@post call.respond(HttpStatusCode.TooManyRequests, "slow down")
            }
            attendees += attendee
            val request = call.receive<CloudRequest>()
            try {
                val reply = cloud.complete(request)
                tokensServed.addAndGet((reply.inputTokens + reply.outputTokens).toLong())
                log.info("{}@v{} {}ms in={} out={}", request.task, request.promptVersion, reply.latencyMs, reply.inputTokens, reply.outputTokens)
                call.respond(reply)
            } catch (e: CloudFailure) {
                log.warn("{}@v{} failed: {} {}", request.task, request.promptVersion, e.kind, e.message)
                val status = when (e.kind) {
                    CloudFailure.Kind.REFUSED -> HttpStatusCode.UnprocessableEntity
                    CloudFailure.Kind.RATE_LIMITED -> HttpStatusCode.TooManyRequests
                    else -> HttpStatusCode.BadGateway
                }
                call.respond(status, e.message ?: e.kind.name)
            }
        }
    }
}
