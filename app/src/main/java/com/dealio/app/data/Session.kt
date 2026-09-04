package com.dealio.app.data

import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * The one place that knows the signed-in session has ended.
 *
 * A Dealio access token no longer expires on a clock — it carries no `exp`
 * claim, and `ApiClient` renews it from the refresh token when the server
 * rejects one. So a session ends only when something ends it: the device signed
 * out from another one, the account suspended, or a dev server restarted with a
 * fresh JWT_SECRET.
 *
 * The app used to sit on a dead token forever, because "logged in" only meant
 * "a token string is in SharedPreferences". Every authed screen then rendered
 * the backend's raw 401 — "Invalid or expired token" — over a Try again button
 * that re-fired the same doomed request, with no route back to sign-in short of
 * clearing the app's data. [end] is the signal the nav host listens on to bounce
 * a session that dies mid-use, and [isExpired] still reads the claim for the
 * tokens issued while they lasted a week.
 */
object Session {

    private val _ended = MutableStateFlow(false)

    /** Flips to true once a session ends; the nav host resets it via [acknowledge]. */
    val ended: StateFlow<Boolean> = _ended.asStateFlow()

    fun end() {
        _ended.value = true
    }

    /** Called once the user has been sent back to sign-in. */
    fun acknowledge() {
        _ended.value = false
    }

    /**
     * True when [token]'s `exp` claim is already in the past.
     *
     * Tokens minted now have no `exp` at all, which reads here as "not expired"
     * — the same answer as for a token this cannot parse. That is deliberate:
     * this is only a shortcut so a launch holding one of the old week-long
     * tokens can skip a doomed request; the server stays the authority, and
     * anything unreadable takes the network path and gets a 401 like before.
     */
    fun isExpired(token: String): Boolean {
        val exp = expiryEpochSeconds(token) ?: return false
        return exp <= System.currentTimeMillis() / 1000
    }

    private fun expiryEpochSeconds(token: String): Long? = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return null
        val flags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        val json = JSONObject(String(Base64.decode(payload, flags)))
        json.optLong("exp").takeIf { it > 0 }
    }.getOrNull()
}
