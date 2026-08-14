package com.dealio.app.data

import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * The one place that knows the signed-in session has ended.
 *
 * A Dealio access token lives seven days and there is nothing to renew it with:
 * the backend has no refresh route, and the `refreshToken` it hands back at
 * sign-in is the very same JWT. So every session eventually dies — on its own
 * expiry, when the device is signed out from another one, or when a dev server
 * restarts with a fresh JWT_SECRET.
 *
 * The app used to sit on the dead token forever, because "logged in" only meant
 * "a token string is in SharedPreferences". Every authed screen then rendered
 * the backend's raw 401 — "Invalid or expired token" — over a Try again button
 * that re-fired the same doomed request, with no route back to sign-in short of
 * clearing the app's data. Both halves of that are fixed here: [isExpired] lets
 * the launch path notice a stale token before it is ever used, and [end] is the
 * signal the nav host listens on to bounce a session that dies mid-use.
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
     * Unreadable tokens count as live on purpose. This is only a shortcut so a
     * launch with a known-dead token skips straight to sign-in; the server stays
     * the authority, and anything this can't parse just takes the network path
     * and gets a 401 like before.
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
