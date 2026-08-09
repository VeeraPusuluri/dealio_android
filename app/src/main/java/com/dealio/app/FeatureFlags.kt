package com.dealio.app

/**
 * Flags for things that are built but deliberately not reachable yet.
 *
 * Prefer a flag over deleting the screen: the flows below still work, they are
 * just not offered, so turning one back on is a one-line change rather than a
 * revert.
 */

/**
 * Self-serve signup. Off for now — accounts are created for people rather than
 * by them, so the "Create an account" offer is hidden on the sign-in screen.
 * `SignupScreen` and its nav route are untouched behind this; flip to `true` to
 * put the entry point back. Mirrors `SIGNUP_ENABLED` in the web app's
 * `src/lib/featureFlags.ts`.
 */
const val SIGNUP_ENABLED = false
