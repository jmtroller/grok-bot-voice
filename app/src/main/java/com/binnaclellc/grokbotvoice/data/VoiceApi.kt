package com.binnaclellc.grokbotvoice.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class VoiceApi(
    private val settings: SettingsStore,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    @Serializable
    data class Turn(
        val id: String? = null,
        val direction: String? = null,
        val text: String = "",
        val status: String? = null,
        @SerialName("audio_url") val audioUrl: String? = null,
        @SerialName("in_reply_to") val inReplyTo: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("error") val error: String? = null,
    )

    @Serializable
    data class TurnsResponse(
        val turns: List<Turn> = emptyList(),
        val count: Int = 0,
    )

    @Serializable
    data class ErrorBody(
        val error: String? = null,
        @SerialName("error_description") val errorDescription: String? = null,
    )

    fun postUtterance(audio: File): Turn {
        val token = requireToken()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                audio.name,
                audio.asRequestBody("audio/mp4".toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url("${settings.serverUrl}/voice/utterances")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        return execute(request) { json.decodeFromString(Turn.serializer(), it) }
    }

    fun getTurns(after: String?): TurnsResponse {
        val token = requireToken()
        val url = buildString {
            append(settings.serverUrl)
            append("/voice/turns")
            if (!after.isNullOrBlank()) {
                append("?after=")
                append(after)
            }
        }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return execute(request) { json.decodeFromString(TurnsResponse.serializer(), it) }
    }

    fun downloadAudio(turnId: String, dest: File): File {
        val token = requireToken()
        val request = Request.Builder()
            .url("${settings.serverUrl}/voice/turns/$turnId/audio")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(errorMessage(response.body?.string(), response.code))
            }
            val bytes = response.body?.bytes() ?: throw IOException("Empty audio")
            dest.writeBytes(bytes)
            return dest
        }
    }

    private fun requireToken(): String {
        val token = settings.deviceToken
        if (token.isBlank()) {
            throw IOException("Set the device token in Settings.")
        }
        return token
    }

    private fun <T> execute(request: Request, decode: (String) -> T): T {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(errorMessage(raw, response.code))
            }
            return decode(raw)
        }
    }

    private fun errorMessage(raw: String?, code: Int): String {
        val parsed = runCatching {
            json.decodeFromString(ErrorBody.serializer(), raw.orEmpty())
        }.getOrNull()
        val detail = parsed?.errorDescription ?: parsed?.error
        return when {
            code == 401 -> "Unauthorized (401). Check the device token."
            !detail.isNullOrBlank() -> detail
            else -> "Server error ($code)"
        }
    }
}
