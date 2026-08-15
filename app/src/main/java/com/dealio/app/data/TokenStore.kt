package com.dealio.app.data

import android.content.Context
import androidx.core.content.edit
import com.dealio.app.data.api.AuthData
import com.dealio.app.data.api.AuthUser

/**
 * Persists the JWT pair and the signed-in user, mirroring the web app's
 * `dealio_access_token` / `dealio_refresh_token` localStorage keys.
 */
class TokenStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dealio_auth", Context.MODE_PRIVATE)

    /**
     * Whether there is still a session to resume: an access token inside its
     * lifetime, or an expired one we hold a refresh token for.
     *
     * Presence of a token alone used to be enough, which is how a launch with a
     * week-old token reached the dashboard and then failed on every request —
     * see [Session]. But an expired access token is not the end of a session
     * when a 60-day refresh token sits beside it; treating it as one would send
     * exactly the users this is meant to keep signed in back to the sign-in
     * screen. Those launches go through, and the first request renews the token.
     */
    val isLoggedIn: Boolean
        get() {
            val access = accessToken ?: return false
            return !Session.isExpired(access) || !refreshToken.isNullOrBlank()
        }

    /** Raw JWT access token, used to authorize builder/customer API calls. */
    val accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * The long-lived credential that renews [accessToken]. Opaque — it is not a
     * JWT and carries no expiry to read; the server is the only judge of whether
     * it is still good.
     */
    val refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)

    /** Store a renewed pair, leaving the cached user untouched. */
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    fun save(auth: AuthData) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, auth.accessToken)
            putString(KEY_REFRESH_TOKEN, auth.refreshToken)
            putLong(KEY_USER_ID, auth.user.id)
            putString(KEY_FULL_NAME, auth.user.fullName)
            putString(KEY_PHONE, auth.user.phone)
            putString(KEY_ROLE, auth.user.role)
            putString(KEY_EMAIL, auth.user.email)
            putString(KEY_AVATAR_URL, auth.user.avatarUrl)
        }
    }

    fun user(): AuthUser? {
        if (!isLoggedIn) return null
        return AuthUser(
            id = prefs.getLong(KEY_USER_ID, -1),
            fullName = prefs.getString(KEY_FULL_NAME, null),
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            role = prefs.getString(KEY_ROLE, "CUSTOMER") ?: "CUSTOMER",
            email = prefs.getString(KEY_EMAIL, null),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null),
        )
    }

    /**
     * Remembers a name or email the user just changed on their own profile.
     *
     * Same reason as [avatarUrl]: the session payload is only handed out at
     * sign-in, so an edited name would otherwise show on the screen that saved
     * it and nowhere else until the next login.
     */
    fun updateIdentity(fullName: String?, email: String?) {
        prefs.edit {
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_EMAIL, email)
        }
    }

    /**
     * Remembers a picture the user just set.
     *
     * The session payload only arrives at sign-in, so without this a new photo
     * would not appear anywhere else in the app until the next login.
     */
    var avatarUrl: String?
        get() = prefs.getString(KEY_AVATAR_URL, null)
        set(value) = prefs.edit { putString(KEY_AVATAR_URL, value) }

    /**
     * The customer's preferred city, remembered locally.
     *
     * The API can only be told the city (PATCH /customer/preferred-city) — there
     * is no endpoint that reads it back, so without this the settings screen
     * showed no city selected even right after the user picked one.
     */
    var preferredCity: String?
        get() = prefs.getString(KEY_PREFERRED_CITY, null)
        set(value) = prefs.edit { putString(KEY_PREFERRED_CITY, value) }

    /**
     * The channel partner's tier, remembered locally.
     *
     * Only so the credential card can be drawn from cache before
     * `GET /cp/:id/profile` answers. Without it the card would have to guess a
     * tier for that first frame, and printing "Silver" on a Gold partner's
     * credential is worse than waiting.
     */
    var cpTier: String?
        get() = prefs.getString(KEY_CP_TIER, null)
        set(value) = prefs.edit { putString(KEY_CP_TIER, value) }

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_PHONE = "phone"
        const val KEY_ROLE = "role"
        const val KEY_EMAIL = "email"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_PREFERRED_CITY = "preferred_city"
        const val KEY_CP_TIER = "cp_tier"
    }
}
