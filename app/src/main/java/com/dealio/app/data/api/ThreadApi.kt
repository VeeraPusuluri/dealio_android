package com.dealio.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Messaging, for all three portals.
 *
 * A conversation is between *people*. It used to be a room inside a deal, keyed
 * (dealId, threadKey) — so a buyer talking to one builder about two towers had
 * two "Builder" rows in their inbox, same person behind both. Nothing about a
 * chat was ever per-project, so nothing about it is keyed on one now.
 *
 * Role-agnostic: the caller is resolved from the token and the backend decides
 * which conversations they may hold, so one API serves the builder, the CP and
 * the buyer without any of them passing an id for themselves.
 */
interface ThreadApi {
    /** Conversations that have actually been opened — the inbox. */
    @GET("threads")
    suspend fun list(): Response<ApiEnvelope<List<Conversation>>>

    /**
     * Everyone this user could talk to, whether or not a conversation exists —
     * what the "+" on the conversations screen offers. Entries already open
     * carry their `id`, so tapping one resumes rather than duplicating.
     */
    @GET("threads/candidates")
    suspend fun candidates(): Response<ApiEnvelope<List<Conversation>>>

    /** Open (or reopen) a conversation by key. Idempotent. */
    @POST("threads/open")
    suspend fun open(@Body body: OpenConversationRequest): Response<ApiEnvelope<Conversation>>

    @GET("threads/{id}/messages")
    suspend fun messages(@Path("id") id: Long): Response<ApiEnvelope<ConversationTranscript>>

    @POST("threads/{id}/messages")
    suspend fun send(
        @Path("id") id: Long,
        @Body body: SendConversationMessageRequest,
    ): Response<ApiEnvelope<ConversationMessage>>

    @POST("threads/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<ApiEnvelope<Any>>

    /**
     * Poke whoever the deal is waiting on. The target is derived server-side from
     * the deal's own baton, so there is nothing to send but the deal id.
     *
     * Still deal-shaped, and rightly so: a nudge is about a transaction stalling,
     * which is the one thing that genuinely belongs to a deal rather than to the
     * people on it.
     */
    @POST("deals/{dealId}/nudge")
    suspend fun nudge(@Path("dealId") dealId: Long): Response<ApiEnvelope<NudgeResult>>
}

data class NudgeResult(
    val nudged: List<String> = emptyList(),
    val action: String = "",
)

data class OpenConversationRequest(val key: String)

data class SendConversationMessageRequest(val message: String)

data class ConversationLastMessage(
    val message: String = "",
    val senderRole: String = "",
    val senderName: String = "",
    val createdAt: String = "",
)

/**
 * One conversation, as both the inbox and the "+" picker see it.
 *
 * [id] is null for a candidate nobody has opened yet; [key] is always present
 * and is what `open` takes, so a screen can hand either to the same call site.
 */
data class Conversation(
    val id: Long? = null,
    val key: String = "",
    /** "cp-customer" | "builder-cp" | "group" | "builder-customer" */
    val kind: String = "",
    /** Who the viewer is talking to, already phrased from their side. */
    val title: String = "",
    /** The other participants by name — the second line on a group row. */
    val participants: List<String> = emptyList(),
    val isGroup: Boolean = false,
    val lastMessage: ConversationLastMessage? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
)

data class ConversationMessage(
    val id: Long = 0,
    val conversationId: Long = 0,
    val senderId: Long = 0,
    val senderName: String = "",
    val senderRole: String = "",
    val message: String = "",
    val createdAt: String = "",
)

data class ConversationTranscript(
    val conversation: Conversation = Conversation(),
    val messages: List<ConversationMessage> = emptyList(),
)
