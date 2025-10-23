package io.syhids.vanguardian.server

import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.encodeBase64
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.nirmato.ollama.api.ChatRequest
import org.nirmato.ollama.api.ChatRequest.Companion.chatRequest
import org.nirmato.ollama.api.Format
import org.nirmato.ollama.api.Message
import org.nirmato.ollama.api.Role
import org.nirmato.ollama.client.ktor.OllamaClient

private const val OllamaUrl = "http://localhost:11434/api/"

// Used to describe the image and output a json
private const val MultimodalLlm = "llava"
private const val EnableAlertTranslation = true

private const val DebugPrints = false

private val ollamaClient = OllamaClient(CIO) {
    httpClient {
        defaultRequest {
            url(OllamaUrl)
        }
    }
}

fun Application.configureRouting() {
    routing {
        post("/analyze") {
            debugPrintln("POST /analyze")

            val base64Image = call.receiveChannel().toByteArray().encodeBase64()

            debugPrintln("Received image: ${base64Image.take(16)}...${base64Image.takeLast(16)}")

            val request = buildLLMChatRequest(base64Image)
            val response = ollamaClient.chat(request)
            val jsonResponse = response.message!!.content

            try {
                val ollamaResponse = Json.decodeFromString<ExpectedOllamaResponse>(jsonResponse)

                @Suppress("SimplifyBooleanWithConstants")
                if (EnableAlertTranslation && ollamaResponse.alert) {
                    val translatedResponse = try {
                        Json.encodeToString(
                            ollamaResponse.copy(reason = ollamaClient.translateWithLlm(ollamaResponse.reason))
                        )
                    } catch (e: Exception) {
                        println("Could not translate alert")
                        println(e)
                        jsonResponse
                    }

                    call.respondText(translatedResponse, contentType = ContentType.Application.Json)
                } else {
                    call.respondText(jsonResponse, contentType = ContentType.Application.Json)
                }
            } catch (e: Exception) {
                println("Could not parse response: $jsonResponse")
                println(e)
                call.respond(HttpStatusCode.UnprocessableEntity)
            }
        }
    }
}

private fun buildLLMChatRequest(base64Image: String): ChatRequest = chatRequest {
    format(Format.FormatType.JSON)
    model(MultimodalLlm)
    messages(
        listOf(
            Message(
                role = Role.USER,
                content = listOf(
                    "You are a concise safety analyzer for a camper van interior.",
                    "Output: a JSON object exactly like {\"alert\": true|false, \"reason\": \"short reason\", \"severity\": \"low|medium|high\"}.",
                    "",
                    "Decide if this frame indicates theft, intrusion, fire/smoke, or dangerous situation. Be conservative with false positives.",
                ).joinToString(separator = "\n"),
                images = listOf(base64Image)
            )
        )
    )
}

@Serializable
data class ExpectedOllamaResponse(val alert: Boolean, val reason: String, val severity: Severity? = null) {
    @Suppress("unused", "EnumEntryName")
    enum class Severity {
        medium,
        low,
        high,
    }

}

private fun debugPrintln(text: String) {
    if (DebugPrints) {
        println(text)
    }
}