package com.dealio.app.ui.meetups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Villa
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.IconOrange
import com.dealio.app.ui.components.IconPurple
import com.dealio.app.ui.components.IconRed
import com.dealio.app.ui.theme.Teal

/**
 * The words a meetup is described in, shared by the organiser's screens and the
 * customer's.
 *
 * Lives outside both `ui.cp` and `ui.customer` because a category means the same
 * thing on either side — the moment one of them owns the vocabulary, the other
 * ends up importing across the app's main seam, or quietly forking it.
 */

/**
 * What kind of gathering this is.
 *
 * Meetup.com leads with a topic because it is the first thing that tells you
 * whether an event is for you. The same holds here — "Open house" and "Loan
 * clinic" are different propositions, and someone scanning a list sorts on that
 * before they read the title.
 *
 * The wire value is the server's string; everything else is presentation. An
 * unknown value from a newer backend falls back to [OTHER] rather than crashing
 * a list, so the two can be deployed in either order.
 */
enum class MeetupCategory(
    val wire: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
) {
    SITE_VISIT("SITE_VISIT", "Site visit", Icons.Outlined.Apartment, Teal),
    OPEN_HOUSE("OPEN_HOUSE", "Open house", Icons.Outlined.Villa, IconBlue),
    INVESTOR_EVENING("INVESTOR_EVENING", "Investor evening", Icons.Outlined.TrendingUp, IconPurple),
    NRI_SESSION("NRI_SESSION", "NRI session", Icons.Outlined.Public, IconOrange),
    LOAN_CLINIC("LOAN_CLINIC", "Loan clinic", Icons.Outlined.AccountBalance, IconGreen),
    NETWORKING("NETWORKING", "Networking", Icons.Outlined.Handshake, IconRed),
    OTHER("OTHER", "Other", Icons.Outlined.Groups, Teal);

    /**
     * The header wash behind an event with no photograph.
     *
     * Meetups carry no cover image, and a grey block reads as a broken one. A
     * category-tinted gradient instead makes a list look deliberate, and tells
     * you what each one is from across the room.
     */
    val gradient: Brush
        get() = Brush.linearGradient(listOf(tint.copy(alpha = 0.92f), tint.copy(alpha = 0.55f)))

    companion object {
        fun from(wire: String?): MeetupCategory = entries.firstOrNull { it.wire == wire } ?: OTHER
    }
}

/** In person, online, or both. */
enum class MeetupMode(val wire: String, val label: String) {
    IN_PERSON("IN_PERSON", "In person"),
    ONLINE("ONLINE", "Online"),
    HYBRID("HYBRID", "Both");

    companion object {
        fun from(wire: String?): MeetupMode = entries.firstOrNull { it.wire == wire } ?: IN_PERSON
    }
}

/** How an answer reads, and the colour it carries, on both sides of the feature. */
enum class Rsvp(val wire: String, val label: String, val tint: Color) {
    GOING("GOING", "Going", IconGreen),
    MAYBE("MAYBE", "Maybe", IconOrange),
    DECLINED("DECLINED", "Can't go", IconRed),
    INVITED("INVITED", "No reply", Color(0xFF94A3B8));

    companion object {
        fun from(wire: String?): Rsvp = entries.firstOrNull { it.wire == wire } ?: INVITED
    }
}
