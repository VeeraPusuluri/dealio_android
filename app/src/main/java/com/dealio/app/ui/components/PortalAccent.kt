package com.dealio.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dealio.app.ui.auth.RoleCp
import com.dealio.app.ui.auth.RoleCustomer
import com.dealio.app.ui.auth.onNavy
import com.dealio.app.ui.theme.TealBright

/**
 * The colour a portal's hero is lit with.
 *
 * Every portal opened on the same navy band, so a buyer's home, a partner's home
 * and a builder's home were the same screen with different words on it. Sign-in
 * had already solved this: the role picker tints its hero with the role's own
 * colour, which is why picking "Partner" turns the whole surface warm. These are
 * those same tints, carried past the sign-in card into the portal behind it — so
 * the colour a user chooses to sign in with is the colour they then work in.
 *
 * Kept as one accent per portal rather than a colour per screen: the tint is an
 * identity, and a tab that invented its own would read as a different app again.
 */

/** Buyer green — [RoleCustomer]'s colour, lifted for legibility on navy. */
val CustomerHeroAccent = RoleCustomer.color.onNavy()

/** Partner orange — [RoleCp]'s colour, same treatment. */
val CpHeroAccent = RoleCp.color.onNavy()

/**
 * Builder blue.
 *
 * Not the builder's own teal: against the buyer's green it read as the same
 * portal at a glance, and telling the portals apart is the entire point of
 * tinting them. This is the blue the bank role already carries on sign-in, which
 * is the one cool hue in the palette that is nobody else's.
 */
val BuilderHeroAccent = Color(0xFF2E5D8E).onNavy()

/**
 * Accent for the hero of whichever portal is on screen.
 *
 * Provided once per portal root, so a screen paints the right tint without
 * having to be told which portal it belongs to. The default is the sign-in
 * screen's own default, for anything that renders outside a portal.
 */
val LocalHeroAccent = compositionLocalOf { TealBright }
