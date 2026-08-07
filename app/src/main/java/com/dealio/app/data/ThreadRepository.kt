package com.dealio.app.data

import com.dealio.app.data.api.ApiClient
import com.dealio.app.data.api.ApiEnvelope
import com.dealio.app.data.api.NudgeResult
import com.dealio.app.data.api.ThreadReadRequest
import com.dealio.app.data.api.ThreadRef
import com.dealio.app.data.api.ThreadSummary
import com.dealio.app.data.api.ThreadSummaryRequest
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/**
 * Unread counts per deal thread, and the read markers that clear them.
 *
 * Role-agnostic — the backend resolves the caller from the token and authorizes
 * each (dealId, threadKey) pair itself, so all three portals share this.
 */
class ThreadRepository {
    private val api = ApiClient.threadApi
    private val gson = Gson()

    /** Unread counts for the given threads. Unauthorized pairs are simply absent. */
    suspend fun summaries(threads: List<ThreadRef>): ApiResult<List<ThreadSummary>> {
        if (threads.isEmpty()) return ApiResult.Success(emptyList())
        return call { api.getSummaries(ThreadSummaryRequest(threads)) }
    }

    /**
     * Mark one thread read up to now.
     *
     * Best-effort by design: a failure here costs a stale badge, never a lost
     * message, so callers fire it without surfacing errors.
     */
    suspend fun markRead(dealId: Long, threadKey: String): ApiResult<Any> =
        call { api.markRead(ThreadReadRequest(dealId, threadKey)) }

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
