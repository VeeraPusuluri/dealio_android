package com.dealio.app.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary

/**
 * What each role may do at each stage.
 *
 * The Android twin of `STAGE_ACTIONS` in `Dealio_frontend/src/lib/dealStages.ts`,
 * which the web renders as the role-specific action card in `DealRoom.tsx`. The
 * stage × role gating and the copy are the website's, verbatim; only the CTA
 * targets are Android's, because the two apps navigate differently.
 *
 * Before this existed each Android deal screen re-derived its own gate, and the
 * CP screen's was `stageIndex(status) in 0..3` — a range that includes *Meeting
 * Requested* and *Meeting Confirmed*, so a CP was offered "Schedule site visit"
 * on a visit they had already booked and the builder had already confirmed. The
 * website offers no action to a CP at either stage; it tells them who they are
 * waiting on. That is the whole point of holding this in one table.
 *
 * Add or change an action HERE and all three portals pick it up.
 */

/**
 * Where a stage CTA leads.
 *
 * The web deep-links everything, including back to the page the viewer is
 * already on (`/cp/leads?deal=<id>`). Android screens own their own navigation
 * and their own sheets, so a target names the *intent* and each screen decides
 * how to serve it — opening a bottom sheet, calling the view model, or
 * navigating. A screen handles the targets its role can receive and ignores the
 * rest.
 */
enum class StageTarget {
    // ── Served in place, on the deal already open ──
    /** CP: open the site-visit booking sheet. */
    REQUEST_VISIT,
    /** CP: open the follow-up dialog. */
    LOG_FOLLOW_UP,
    /** CP: agree to the deal terms. */
    AGREE,
    /** Customer: pick and submit the signed agreement. */
    UPLOAD_SIGNED_AGREEMENT,
    /** Customer: choose an actual unit off the project's matrix and shortlist it. */
    PICK_UNIT,

    // ── Elsewhere in the app ──
    BUILDER_MEETINGS,
    BUILDER_SHORTLISTS,
    BUILDER_COMMISSIONS,
    /** Builder: countersign the buyer's signed agreement, in place. */
    BUILDER_ACCEPT_AGREEMENT,
    CP_COMMISSIONS,
    CUSTOMER_VISITS,
    /** The buyer shortlists a unit on the deal's project page. */
    CUSTOMER_PROJECT,
    CUSTOMER_LOAN,
}

/** The one action a role is offered at a stage. */
data class StageCta(val label: String, val target: StageTarget)

/**
 * @param headline what is happening at this stage, in this role's register.
 * @param cta the single next action, or null when this role has nothing to do —
 *        which is itself the answer, and the reason this table exists.
 */
data class StageAction(val headline: String, val cta: StageCta?)

private fun act(headline: String, label: String, target: StageTarget) =
    StageAction(headline, StageCta(label, target))

private fun wait(headline: String) = StageAction(headline, null)

private val STAGE_ACTIONS: Map<String, Map<DealRole, StageAction>> = mapOf(
    "New Lead" to mapOf(
        DealRole.CUSTOMER to wait("We have your enquiry — your advisor is setting up your search."),
        // The web sends the CP to a requirement form. Android has no such screen
        // and no endpoint behind one (CpRepository cannot write a requirement),
        // so the move actually available here is the one that advances the lead.
        DealRole.CP to act(
            "Capture what this buyer is looking for so the builder can match them.",
            "Request a site visit", StageTarget.REQUEST_VISIT,
        ),
        DealRole.BUILDER to wait("A channel partner has introduced a buyer. Nothing to do until they qualify them."),
    ),
    "Profile Created" to mapOf(
        DealRole.CUSTOMER to wait("We have your requirement — your advisor is arranging a visit."),
        DealRole.CP to act(
            "Requirement captured. Request a site visit with the builder.",
            "Request a site visit", StageTarget.REQUEST_VISIT,
        ),
        DealRole.BUILDER to wait("The buyer's requirement is in. Expect a visit request shortly."),
    ),
    // ── The two stages a visit is already on the books ──
    // Nobody may book another one. The CP asked for it, the builder owes the
    // slot; offering "schedule" to either the CP or the buyer here would create
    // a second meeting row for a visit that already exists.
    "Meeting Requested" to mapOf(
        DealRole.CUSTOMER to wait("The builder is confirming your site-visit slot."),
        DealRole.CP to wait("Visit requested — waiting on the builder to confirm a slot."),
        DealRole.BUILDER to act(
            "A site visit has been requested. Confirm a slot to keep the deal moving.",
            "Confirm a site visit slot", StageTarget.BUILDER_MEETINGS,
        ),
    ),
    "Meeting Confirmed" to mapOf(
        DealRole.CUSTOMER to act(
            "Your site visit is booked — see you there.",
            "View your visit", StageTarget.CUSTOMER_VISITS,
        ),
        DealRole.CP to wait("Visit confirmed. Make sure the buyer attends."),
        DealRole.BUILDER to act(
            "Visit confirmed — the buyer is expected on site.",
            "View meetings", StageTarget.BUILDER_MEETINGS,
        ),
    ),
    "Meeting Done" to mapOf(
        // The stage the website puts the unit picker on, and for the same
        // reason: a buyer can only name the flat they want once they have
        // stood in it. Before this the app sent them to the project page,
        // where all they could shortlist was a *configuration* — "2 BHK" —
        // so the builder received an expression of interest with nothing in
        // it to reserve. It now opens the project's actual matrix.
        DealRole.CUSTOMER to act(
            "Your site visit is done — pick the unit you liked and we'll ask the builder for a price.",
            "Pick your unit", StageTarget.PICK_UNIT,
        ),
        // The web routes to the follow-ups list; the Android deal screen already
        // carries the follow-up dialog, so the CP logs it without leaving.
        DealRole.CP to act(
            "Visit complete. Follow up with the customer to move the deal forward.",
            "Log a follow-up", StageTarget.LOG_FOLLOW_UP,
        ),
        DealRole.BUILDER to act(
            "Customer has visited. Review their shortlist when it arrives.",
            "Review shortlists", StageTarget.BUILDER_SHORTLISTS,
        ),
    ),
    // From here the web's CTAs deep-link back to the very page the viewer is on
    // (`?deal=<id>`), where the quote, the countersign and the booking already
    // live. On Android those controls are on this screen too, so the card states
    // the position and leaves them to it rather than duplicating a button.
    "Negotiation" to mapOf(
        DealRole.CUSTOMER to wait("Pricing & terms are being worked out. Review the quote and message your builder or CP."),
        DealRole.CP to act(
            "Negotiate on the customer's behalf and agree to the deal terms.",
            "Open deal & agree", StageTarget.AGREE,
        ),
        DealRole.BUILDER to wait("Share a pricing quote and negotiate terms with the customer."),
    ),
    "Agreement" to mapOf(
        // The buyer's half of this stage is two moves — confirm, then send the
        // signed copy — and the confirm already sits on the spine. This is the
        // other half, and without it the deal cannot leave Agreement at all:
        // the builder's accept-agreement returns 400 until a signed document
        // exists on the row.
        DealRole.CUSTOMER to act(
            "The agreement is ready. Confirm acceptance and upload your signed copy.",
            "Upload signed copy", StageTarget.UPLOAD_SIGNED_AGREEMENT,
        ),
        DealRole.CP to wait("Agreement shared — awaiting the customer's signature."),
        // The website's countersign, which is a different move from advancing
        // the stage: it refuses until the buyer has actually sent a signed copy,
        // and it is what tells the CP and the buyer the agreement was accepted.
        // The Advance control below it would do neither.
        DealRole.BUILDER to act(
            "Once the customer uploads the signed agreement, countersign to proceed.",
            "Countersign agreement", StageTarget.BUILDER_ACCEPT_AGREEMENT,
        ),
    ),
    "Pending Booking" to mapOf(
        DealRole.CUSTOMER to wait("Your signed agreement was accepted — the booking is being confirmed."),
        DealRole.CP to wait("Agreement accepted — booking in progress with the builder."),
        DealRole.BUILDER to wait("Confirm the booking to lock the unit for this customer."),
    ),
    "Booked" to mapOf(
        DealRole.CUSTOMER to act(
            "Unit booked! Apply for a home loan if you need financing.",
            "Apply for a home loan", StageTarget.CUSTOMER_LOAN,
        ),
        DealRole.CP to act(
            "Unit booked — your commission is being processed.",
            "View commission", StageTarget.CP_COMMISSIONS,
        ),
        DealRole.BUILDER to wait("Unit booked. Set up the payment schedule for the customer."),
    ),
    "Closed" to mapOf(
        // The web offers an interior vendor here. Android has no vendor surface —
        // VENDOR was descoped platform-wide — so the buyer reads the close and is
        // sent nowhere rather than to a screen that does not exist.
        DealRole.CUSTOMER to wait("Deal complete — welcome home!"),
        DealRole.CP to act(
            "Deal closed. Track your commission payout.",
            "Commission status", StageTarget.CP_COMMISSIONS,
        ),
        DealRole.BUILDER to act(
            "Deal closed. Release the channel-partner commission.",
            "Release commission", StageTarget.BUILDER_COMMISSIONS,
        ),
    ),
)

/**
 * The action a [role] is offered at [rawStatus].
 *
 * Folds legacy spellings onto the canonical ten first, so a deal still carrying
 * "Site Visit Scheduled" reads as Meeting Confirmed and gets that stage's
 * answer — the very row most likely to be sitting on an old status.
 */
fun stageActionFor(rawStatus: String?, role: DealRole): StageAction =
    STAGE_ACTIONS.getValue(canonicalStage(rawStatus) ?: "New Lead").getValue(role)

// ─── StageActionCard ─────────────────────────────────────────────────────────

/**
 * The stage's headline and, when there is one, its single action.
 *
 * Sits under [DealSpine] on all three deal screens — the spine says who the deal
 * is waiting on, this says what to do about it. When the role has nothing to do
 * the card is the headline alone; that absence is the fix, not an omission.
 *
 * @param onAction invoked with the CTA's target. Null, or a target the screen
 *        does not serve, hides the button and leaves the headline.
 */
@Composable
fun StageActionCard(
    rawStatus: String?,
    viewer: DealRole,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onAction: ((StageTarget) -> Unit)? = null,
) {
    val stageAction = stageActionFor(rawStatus, viewer)
    val cta = stageAction.cta
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            stageAction.headline,
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        if (cta != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onAction(cta.target) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
            ) {
                Text(cta.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
