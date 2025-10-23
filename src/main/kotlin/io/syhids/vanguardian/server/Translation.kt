package io.syhids.vanguardian.server

import org.nirmato.ollama.api.ChatRequest.Companion.chatRequest
import org.nirmato.ollama.api.Message
import org.nirmato.ollama.api.Role
import org.nirmato.ollama.client.ktor.OllamaClient

private const val OutputLanguage = "Spanish"
private const val TranslationLlm = "gemma3"

suspend fun OllamaClient.translateWithLlm(text: String): String {
    val request = chatRequest {
        model(TranslationLlm)
        messages(
            listOf(
                Message(
                    role = Role.SYSTEM,
                    content = "You are a translation assistant. Always translate from English to $OutputLanguage. Respond with ONLY the $OutputLanguage translation and nothing else: no explanations, no notes, no code fences, no extra whitespace, no metadata.",
                ),
                Message(
                    role = Role.USER,
                    content = listOf(
                        "Please translate to $OutputLanguage:",
                        text
                    ).joinToString(separator = "\n"),
                )
            )
        )
    }
    val response = chat(request)
    val responseContent = response.message!!.content
    return responseContent
}