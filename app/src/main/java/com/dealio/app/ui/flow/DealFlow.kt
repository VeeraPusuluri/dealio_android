package com.dealio.app.ui.flow

/**
 * The deal flow, as all three parties see it.
 *
 * This is the Android mirror of `Dealio_Backend/src/utils/dealStage.ts`. The two
 * must agree: the backend validates writes against this ladder, and the app
 * renders against it. Change one, change the other.
 *
 * Three things live here, and nothing else should re-derive them:
 *   1. the canonical ten stages Deal.status may hold
 *   2. the five phases a buyer sees, and the words they read
 *   3. the baton — who owes the next move
 */

// ─── The canonical ladder ────────────────────────────────────────────────────

val DEAL_STAGES = listOf(
    "New Lead",
    "Profile Created",
    "Meeting Requested",
    "Meeting Confirmed",
    "Meeting Done",
    "Negotiation",
    "Agreement",
    "Pending Booking",
    "Booked",
    "Closed",
)

/**
 * Every legacy or coined spelling ever written to Deal.status, folded onto the
 * canonical ten.
 *
 * Rows written before the backend started validating are still in the database,
 * so the app has to absorb them. Loan sub-stages land on Booked (booking is when
 * money moved); registration and both spellings of possession land on Closed.
 */
private val STAGE_ALIASES: Map<String, String> =
    DEAL_STAGES.associateBy { it.lowercase() } + mapOf(
        "enquiry" to "New Lead",
        "lead" to "New Lead",
        "interested loan required" to "Negotiation",
        "interested - loan required" to "Negotiation",
        "site visit scheduled" to "Meeting Confirmed",
        "site visit done" to "Meeting Done",
        "meeting completed" to "Meeting Done",
        "loan application created" to "Booked",
        "loan applied" to "Booked",
        "loan processing" to "Booked",
        "loan sanctioned" to "Booked",
        "loan disbursed" to "Booked",
        "registration done" to "Closed",
        "possession" to "Closed",
        "possession given" to "Closed",
        "won" to "Closed",
    )

/** Fold a raw status onto the canonical ten, or null when unrecognised. */
fun canonicalStage(raw: String?): String? =
    raw?.trim()?.lowercase()?.let { STAGE_ALIASES[it] }

/** Position on the ladder; -1 when unrecognised. */
fun stageIndex(raw: String?): Int = canonicalStage(raw)?.let { DEAL_STAGES.indexOf(it) } ?: -1

// ─── The lead / deal line ────────────────────────────────────────────────────

/**
 * A lead and a deal are the same row at different points on the ladder, and the
 * line between them is Negotiation — the point money enters the conversation.
 *
 * This mirrors `CONVERSION_STAGE` in dealStage.ts. The server already partitions
 * `/leads` and `/deals` on it, so filtering again here is belt-and-braces rather
 * than the primary defence — but it is what keeps a screen honest when it holds
 * a mixed list from some other endpoint (the CP has one `/leads` call that still
 * returns both sides of the line).
 *
 * Note the phase table above already flips VISIT → DEAL at exactly this stage.
 * This names that flip so screens stop re-deriving it with ad-hoc `when` blocks.
 */
const val CONVERSION_STAGE = "Negotiation"

private val CONVERSION_INDEX = DEAL_STAGES.indexOf(CONVERSION_STAGE)

/** True once the row has crossed into deal territory. */
fun isDealStage(raw: String?): Boolean = stageIndex(raw).let { it >= 0 && it >= CONVERSION_INDEX }

/**
 * True while the row is still a lead.
 *
 * The complement of [isDealStage], not a range check, so an unrecognised status
 * counts as a lead. A row matching neither would vanish from both screens, which
 * is worse than the duplication this replaced.
 */
fun isLeadStage(raw: String?): Boolean = !isDealStage(raw)

// ─── The buyer register ──────────────────────────────────────────────────────

enum class JourneyPhase(val label: String) {
    ENQUIRY("Enquiry"),
    VISIT("Visit"),
    DEAL("Deal"),
    BOOKING("Booking"),
    HANDOVER("Handover"),
}

private val PHASE_OF = mapOf(
    "New Lead" to JourneyPhase.ENQUIRY,
    "Profile Created" to JourneyPhase.ENQUIRY,
    "Meeting Requested" to JourneyPhase.VISIT,
    "Meeting Confirmed" to JourneyPhase.VISIT,
    "Meeting Done" to JourneyPhase.VISIT,
    "Negotiation" to JourneyPhase.DEAL,
    "Agreement" to JourneyPhase.DEAL,
    "Pending Booking" to JourneyPhase.BOOKING,
    "Booked" to JourneyPhase.BOOKING,
    "Closed" to JourneyPhase.HANDOVER,
)

/**
 * The phase a buyer sees.
 *
 * The previous implementation matched on a ten-entry `when` and fell through to
 * ENQUIRY, so a buyer whose loan was sanctioned — a status the backend writes and
 * the app did not know — was shown the very first phase of their purchase.
 */
fun phaseOf(rawStatus: String?): JourneyPhase =
    canonicalStage(rawStatus)?.let { PHASE_OF[it] } ?: JourneyPhase.ENQUIRY

/** What a buyer reads instead of the stage. Never contains pipeline vocabulary. */
fun buyerHeadline(rawStatus: String?): String = when (canonicalStage(rawStatus)) {
    "New Lead" -> "We have your enquiry"
    "Profile Created" -> "We have your requirement"
    "Meeting Requested" -> "We are arranging your site visit"
    "Meeting Confirmed" -> "Your site visit is confirmed"
    "Meeting Done" -> "Thanks for visiting"
    "Negotiation" -> "Your price quote is being prepared"
    "Agreement" -> "Your agreement is ready to review"
    "Pending Booking" -> "Your booking is being held"
    "Booked" -> "Your home is booked"
    "Closed" -> "Registration and handover"
    else -> "We have your enquiry"
}

// ─── The baton ───────────────────────────────────────────────────────────────

enum class DealRole { BUILDER, CP, CUSTOMER;
    val label: String get() = when (this) {
        BUILDER -> "Builder"; CP -> "your advisor"; CUSTOMER -> "Customer"
    }

    /**
     * What the backend calls this role — on a message's `senderRole`, and in the
     * hyphenated conversation kinds ("builder-cp", "cp-customer").
     *
     * Distinct from [label], which is prose and changes with the register the
     * reader is in. Comparing against the label is what this exists to prevent.
     */
    val wireName: String get() = when (this) {
        BUILDER -> "builder"; CP -> "cp"; CUSTOMER -> "customer"
    }
}

/**
 * Who owes the next move on a deal.
 *
 * Derived, never stored — a pure function of the status plus the two agreement
 * flags already on the row. That is what lets this ship without a schema change.
 */
data class Baton(
    /** Who owes the move. Empty once the deal is closed. */
    val holders: List<DealRole>,
    /** Imperative, addressed to whoever holds it. */
    val action: String,
    /** Buyer-safe phrasing of the same wait. */
    val buyerCopy: String,
) {
    fun heldBy(role: DealRole) = role in holders
    val isComplete get() = holders.isEmpty()
}

private data class BatonSpec(val holders: List<DealRole>, val action: String, val buyerCopy: String)

private val BATON_OF = mapOf(
    "New Lead" to BatonSpec(listOf(DealRole.CP), "Add the buyer's requirement", "Your advisor is setting up your search"),
    "Profile Created" to BatonSpec(listOf(DealRole.CP), "Request a site visit", "Your advisor is arranging a visit"),
    "Meeting Requested" to BatonSpec(listOf(DealRole.BUILDER), "Confirm a site visit slot", "The builder is confirming your slot"),
    "Meeting Confirmed" to BatonSpec(listOf(DealRole.CUSTOMER), "Attend the site visit", "Your site visit is booked"),
    "Meeting Done" to BatonSpec(listOf(DealRole.CP), "Capture feedback from the visit", "Your advisor is following up"),
    "Negotiation" to BatonSpec(listOf(DealRole.BUILDER), "Send a pricing quote", "The builder is preparing your quote"),
    "Agreement" to BatonSpec(listOf(DealRole.CP, DealRole.CUSTOMER), "Agree to the terms", "Review and accept your agreement"),
    "Pending Booking" to BatonSpec(listOf(DealRole.CUSTOMER), "Complete the booking payment", "Complete your booking payment"),
    "Booked" to BatonSpec(listOf(DealRole.BUILDER), "Registration and possession", "The builder is preparing registration"),
    "Closed" to BatonSpec(emptyList(), "Complete", "Complete"),
)

fun batonOf(
    rawStatus: String?,
    cpAgreed: Boolean = false,
    customerConfirmed: Boolean = false,
): Baton {
    val stage = canonicalStage(rawStatus) ?: "New Lead"
    val spec = BATON_OF[stage] ?: BATON_OF.getValue("New Lead")
    // At Agreement each side is owed independently, so drop whoever has agreed.
    val holders = if (stage != "Agreement") spec.holders else spec.holders.filterNot {
        if (it == DealRole.CP) cpAgreed else customerConfirmed
    }
    return Baton(holders, spec.action, spec.buyerCopy)
}
