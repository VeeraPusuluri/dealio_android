package com.dealio.app.ui.builder.pipeline

import com.dealio.app.ui.flow.DEAL_STAGES
import com.dealio.app.ui.flow.canonicalStage
import com.dealio.app.ui.flow.isLeadStage

/**
 * The columns on the lead board — the ladder up to the conversion point, not the
 * whole of it.
 *
 * Derived from the canonical ladder rather than hand-listed. These were once two
 * hand-maintained lists that had already drifted: the pipeline filed a
 * loan-sanctioned deal under "Closed" while the deal screen called the same row
 * "Booked", because booking is when money moved and the loan stages sit after
 * it. Deriving means they cannot disagree again.
 *
 * Everything from Negotiation on is a deal and lives on the Deals screen; the
 * server no longer returns those rows from `/leads`, so keeping their columns
 * here would render five that can never fill.
 */
val LEAD_STAGES = DEAL_STAGES.filter { isLeadStage(it) }

/** Allowed forward transitions from each stage. */
val NEXT_STAGES = mapOf(
    "New Lead" to listOf("Profile Created", "Meeting Requested", "Negotiation", "Closed"),
    "Profile Created" to listOf("Meeting Requested", "Negotiation", "Closed"),
    "Meeting Requested" to listOf("Meeting Confirmed", "Meeting Done", "Negotiation", "Closed"),
    "Meeting Confirmed" to listOf("Meeting Done", "Negotiation", "Closed"),
    "Meeting Done" to listOf("Negotiation", "Agreement", "Booked", "Closed"),
    "Negotiation" to listOf("Agreement", "Booked", "Closed"),
    "Agreement" to listOf("Pending Booking", "Booked", "Closed"),
    "Pending Booking" to listOf("Booked", "Closed"),
    "Booked" to listOf("Closed"),
    "Closed" to emptyList(),
)

/**
 * A raw `stage` off the wire, folded onto the canonical ten.
 *
 * The enum spellings arrive underscored (`MEETING_REQUESTED`), which the shared
 * alias table does not carry, so they are unpicked to words first. Anything the
 * ladder still doesn't recognise is returned untouched — the baton grouping
 * gathers those into their own section rather than guessing a stage for them.
 */
fun stageLabel(raw: String): String = canonicalStage(raw.replace('_', ' ')) ?: raw

/**
 * The value `PATCH /builder/:builderId/leads/:dealId/stage` expects — the
 * canonical spaced label, unchanged.
 *
 * This used to translate the label to SCREAMING_CASE ("Meeting Requested" →
 * "MEETING_REQUESTED"), a vocabulary the API has never accepted: it keys on the
 * spaced labels, so four of the ten stages came back
 * `400 Unknown stage "MEETING_REQUESTED"` and the move silently failed. New
 * Lead, Meeting Requested, Meeting Confirmed and Meeting Done were all
 * unreachable from the pipeline board; the six single-word stages worked only
 * because lower-casing "NEGOTIATION" happens to land on a real key. The web
 * board carried the identical bug and dropped the map for the same reason.
 */
fun stageEnum(label: String): String = canonicalStage(label) ?: label
