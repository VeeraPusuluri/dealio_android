package com.dealio.app.data

import com.dealio.app.data.api.ApiClient
import com.dealio.app.data.api.ApiEnvelope
import com.dealio.app.data.api.Conversation
import com.dealio.app.data.api.ConversationMessage
import com.dealio.app.data.api.ConversationTranscript
import com.dealio.app.data.api.NudgeResult
import com.dealio.app.data.api.OpenConversationRequest
import com.dealio.app.data.api.SendConversationMessageRequest
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/**
 * Conversations, for whichever portal is asking.
 *
 * Role-agnostic by design — the backend resolves the caller from the token and
 * decides which conversations they may hold, so the builder, CP and customer
 * shells all share this one repository rather than each carrying a messaging
 * client of their own.
 */
class ThreadRepository {
    private val api = ApiClient.threadApi
    private val gson = Gson()

    /** The inbox: conversations that have actually been opened. */
    suspend fun list(): ApiResult<List<Conversation>> = call { api.list() }

    /** Everyone this user could talk to — what the "+" picker offers. */
    suspend fun candidates(): ApiResult<List<Conversation>> = call { api.candidates() }

    /** Open or resume a conversation. Safe to call twice; the key is the identity. */
    suspend fun open(key: String): ApiResult<Conversation> =
        call { api.open(OpenConversationRequest(key)) }

    suspend fun messages(conversationId: Long): ApiResult<ConversationTranscript> =
        call { api.messages(conversationId) }

    suspend fun send(conversationId: Long, message: String): ApiResult<ConversationMessage> =
        call { api.send(conversationId, SendConversationMessageRequest(message)) }

    /**
     * Mark one conversation read up to now.
     *
     * Best-effort by design: a failure here costs a stale badge, never a lost
     * message, so callers fire it without surfacing errors.
     */
    suspend fun markRead(conversationId: Long): ApiResult<Any> = call { api.markRead(conversationId) }

    /**
     * Nudge whoever the deal is waiting on.
     *
     * Unlike markRead this is worth surfacing: the caller wants to know it landed,
     * and a 429 carries the cooldown message the user should read.
     */
    suspend fun nudge(dealId: Long): ApiResult<NudgeResult> = call { api.nudge(dealId) }

    private suspend fun <T> call(block: suspend () -> Response<ApiEnvelope<T>>): ApiResult<T> {
        return try {
            val response = block()
            val envelope = if (response.isSuccessful) response.body()
            else response.errorBody()?.string()?.let { raw ->
                runCatching { gson.fromJson(raw, ApiEnvelope::class.java) }.getOrNull()
            }?.let { ApiEnvelope<T>(ok = it.ok, message = it.message) }
            when {
                envelope == null -> ApiResult.Error("Unexpected server response (HTTP ${response.code()})")
                !envelope.ok -> ApiResult.Error(envelope.message ?: "Request failed")
                envelope.data == null ->
                    @Suppress("UNCHECKED_CAST")
                    ApiResult.Success(Unit as T)
                else -> ApiResult.Success(envelope.data)
            }
        } catch (e: IOException) {
            ApiResult.Error("Can't reach the Dealio server. Check your connection and try again.")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Something went wrong")
        }
    }
}
