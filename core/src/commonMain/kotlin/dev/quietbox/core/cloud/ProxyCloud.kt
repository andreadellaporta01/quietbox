package dev.quietbox.core.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ProxyCloud(
    private val baseUrl: String,
    private val token: String,
    private val http: HttpClient = defaultClient(),
) : CloudModel {

    override val name = "proxy"

    override suspend fun complete(request: CloudRequest): CloudReply {
        val response = try {
            http.post("$baseUrl/v1/task") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } catch (e: Exception) {
            throw CloudFailure(CloudFailure.Kind.OFFLINE, e.message ?: "network")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Unauthorized -> throw CloudFailure(CloudFailure.Kind.UNAUTHORIZED, "bad workshop token")
            HttpStatusCode.TooManyRequests -> throw CloudFailure(CloudFailure.Kind.RATE_LIMITED, "slow down")
            HttpStatusCode.UnprocessableEntity -> throw CloudFailure(CloudFailure.Kind.REFUSED, "model declined")
            else -> throw CloudFailure(CloudFailure.Kind.SERVER, "proxy ${response.status.value}")
        }
    }

    companion object {
        fun defaultClient() = HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 15_000 }
        }
    }
}
