package com.dealio.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Per-thread unread counts and read markers.
 *
 * Role-agnostic: the caller is resolved from the token and the backend
 * authorizes every (dealId, threadKey) pair with the same rule that decides
 * whether the thread is readable at all. One API serves all three portals.
 */
interface ThreadApi {
    @POST("threads/summary")
    suspend fun getSummaries(@Body body: ThreadSummaryRequest): Response<ApiEnvelope<List<ThreadSummary>>>

    @POST("threads/read")
    suspend fun markRead(@Body body: ThreadReadRequest): Response<ApiEnvelope<Any>>

    /**
     * Poke whoever the deal is waiting on. The target is derived server-side from
     * the deal's own baton, so there is nothing to send but the deal id.
     */
    @POST("deals/{dealId}/nudge")
    suspend fun nudge(@Path("dealId") dealId: Long): Response<ApiEnvelope<NudgeResult>>
}

data class NudgeResult(
    val nudged: List<String> = emptyList(),
    val action: String = "",
)

data class ThreadRef(val dealId: Long, val threadKey: String)

data class ThreadSummaryRequest(val threads: List<ThreadRef>)

data class ThreadReadRequest(val dealId: Long, val threadKey: String)

data class ThreadLastMessage(
    val message: String = "",
    val senderRole: String = "",
    val senderName: String = "",
    val createdAt: String = "",
)

data class ThreadSummary(
    val dealId: Long = 0,
    val threadKey: String = "",
    val lastMessage: ThreadLastMessage? = null,
    val unreadCount: Int = 0,
)
