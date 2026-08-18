package com.dealio.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dealio.app.ui.auth.DealioRole
import com.dealio.app.ui.auth.RoleBuilder
import com.dealio.app.ui.auth.RoleCp
import com.dealio.app.ui.auth.RoleCustomer
import com.dealio.app.ui.auth.onNavy
import com.dealio.app.ui.theme.Teal
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

/** Buyer gold — [RoleCustomer]'s colour, lifted for legibility on navy. */
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

/**
 * The portal's accent for components sitting on a *light* surface — spinners,
 * rules, small marks in shared widgets.
 *
 * [LocalHeroAccent] cannot be reused for this. It is a colour lifted for
 * legibility on deep navy, so on white it is washed out to the point of looking
 * disabled. Portals that carry a light-surface accent of their own provide it
 * here; everything else keeps the brand teal.
 */
val LocalSurfaceAccent = compositionLocalOf { Teal }

/**
 * The tint a role's *sign-in* hero glows with.
 *
 * Sign-in and the portal have to agree, or the promise above breaks at the
 * moment it is made: a builder picked a teal card and landed on a blue home.
 * Roles with a portal therefore answer with the portal's accent, and the rest
 * — bank, NRI — keep their own colour lifted for navy, as before.
 *
 * The role *pills* deliberately still use [DealioRole.color]: the builder's blue
 * hero comes from the bank's colour, so painting the pill with it too would put
 * two identical blues in a picker whose whole job is telling roles apart.
 */
fun heroAccentFor(role: DealioRole): Color = when (role.value) {
    RoleCustomer.value -> CustomerHeroAccent
    RoleCp.value -> CpHeroAccent
    RoleBuilder.value -> BuilderHeroAccent
    else -> role.color.onNavy()
}
