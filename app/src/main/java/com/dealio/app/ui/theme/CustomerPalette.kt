package com.dealio.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Buyer portal palette ────────────────────────────────────────────────────
//
// The buyer portal ran on the shared brand teal, the same accent the builder and
// partner portals use, so the one portal a member of the public ever sees looked
// like the internal tooling it sits next to. It is now warm: gold on a sand
// field, which is the register property marketing is written in, and which
// nothing else in the product uses.
//
// Only the buyer portal reads these. The teal tokens stay exactly as they were
// for every other portal — this is an identity for one audience, not a reskin of
// the product.

/**
 * The workhorse accent: link text, icons, selected states, thin rules.
 *
 * Deliberately *not* the brand gold below. [CustomerAccentBright] on white
 * carries about 2.3:1 against the page, which fails legibility for anything
 * text-sized — gold's problem is that the colour people picture is a fill
 * colour, not an ink. This is that gold taken down until it can be read, and it
 * is the value to reach for by default.
 */
val CustomerAccent = Color(0xFFA9761F)

/**
 * Brand gold — fills, large surfaces, and anything sitting on navy.
 *
 * This is the colour the portal is *recognised* by: the sign-in role pill, the
 * hero wash, filled buttons. Safe wherever it is the background rather than the
 * mark on top of one.
 */
val CustomerAccentBright = Color(0xFFC9A227)

/** Pressed and depth states under [CustomerAccent]. */
val CustomerAccentDeep = Color(0xFF7C5510)

/**
 * The page field behind the white cards.
 *
 * A warm off-white rather than [Mist]'s cool one. It is a small shift on its own
 * and a decisive one in aggregate: every card in the portal is white, so the
 * field is most of what a buyer actually sees.
 */
val CustomerSurface = Color(0xFFFAF8F3)
