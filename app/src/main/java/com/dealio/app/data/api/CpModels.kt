package com.dealio.app.data.api

// ─── Leads (deals referred by this CP) ───────────────────────────────────────

data class CpLead(
    val id: Long = 0,
    val projectId: Long = 0,
    val projectName: String = "Unknown",
    val builderId: Long? = null,
    val customerName: String = "Unknown",
    val customerPhone: String = "",
    val customerEmail: String? = null,
    val dealValue: Double? = null,
    val status: String = "",
    val commissionStatus: String = "Pending",
    val commissionPercent: Double? = null,
    val estimatedCommission: Double? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class CpDealDetail(
    val id: Long = 0,
    val status: String = "",
    val dealValue: Double? = null,
    val commissionStatus: String? = null,
    val cpAgreed: Boolean = false,
    val customerConfirmed: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val projectName: String = "",
    val cpTier: String? = null,
    val commissionPercent: Double? = null,
    val commissionAmount: Double? = null,
    val messages: List<DealMessage> = emptyList(),
    val dealDocuments: List<DealDocument> = emptyList(),
)

// ─── Commissions ─────────────────────────────────────────────────────────────

data class CpCommission(
    val id: Long = 0,
    val status: String = "",
    val dealValue: Double? = null,
    val commissionStatus: String? = null,
    val commissionPercent: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val commissionReleasedAt: String? = null,
    val customerName: String = "Unknown",
    val projectName: String = "Unknown",
    val projectCity: String = "",
    val cpTier: String? = null,
)

// ─── Profile ─────────────────────────────────────────────────────────────────

data class CpProfile(
    val id: Long = 0,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val cp: CpInfo? = null,
    /** Builders that have formally authorised this CP to represent them. */
    val authorizedBuilders: List<CpAuthorizedBuilder> = emptyList(),
)

data class CpAuthorizedBuilder(
    val builderId: Long = 0,
    val companyName: String = "",
    val authorizedAt: String? = null,
)

data class CpInfo(
    val city: String? = null,
    val bio: String? = null,
    val reraNumber: String? = null,
    val photoUrl: String? = null,
    val tier: String = "Silver",
    val totalDeals: Int = 0,
    val dealsThisMonth: Int = 0,
    val totalEarnings: Double = 0.0,
    val pendingCommission: Double = 0.0,
    val influencerScore: Int = 0,
    val joinedDate: String? = null,
    val phoneVerified: Boolean = false,
    val aadhaarVerified: Boolean = false,
    val panVerified: Boolean = false,
    val reraVerified: Boolean = false,
    val aadhaarUrl: String? = null,
    val panUrl: String? = null,
    val reraUrl: String? = null,
)

// ─── Verification ────────────────────────────────────────────────────────────

data class SendPhoneOtpRequest(val phone: String)

data class VerifyPhoneRequest(val phone: String, val otp: String)

data class CpDocumentUploadResponse(
    val url: String? = null,
    val docType: String? = null,
)

// ─── Contacts (CRM) ──────────────────────────────────────────────────────────

data class CpContact(
    val id: Long = 0,
    val name: String = "",
    /**
     * Dial code, e.g. "+91". Kept apart from [phone] — see CountryCodes.kt.
     * Nullable because Gson ignores Kotlin defaults: a backend that predates
     * the column leaves this null rather than "+91", and every reader of it
     * treats null as India.
     */
    val countryCode: String? = null,
    val phone: String = "",
    val email: String? = null,
    val notes: String? = null,
    val tags: String? = null,
    val bhkPreference: String? = null,
    val designation: String? = null,
    val salary: Double? = null,
    /** What they can put into property in a year. Seeded from salary on import. */
    val investment: Double? = null,
    val address: String? = null,
    val createdAt: String = "",
)

data class CpContactPayload(
    val name: String,
    val phone: String,
    val countryCode: String = "+91",
    val email: String? = null,
    val notes: String? = null,
    val tags: String? = null,
    val bhkPreference: String? = null,
    val designation: String? = null,
    val salary: Double? = null,
    val investment: Double? = null,
    val address: String? = null,
)

// ─── Follow-ups / call logs / due today ─────────────────────────────────────

data class CpFollowUp(
    val id: String = "",
    val customerName: String = "Unknown",
    val projectName: String = "Unknown",
    val reason: String = "",
    val dueDate: String = "",
    val dueTime: String? = null,
    val done: Boolean = false,
)

data class CpCallLog(
    val id: String = "",
    val customerName: String = "Unknown",
    val projectName: String = "Unknown",
    val outcome: String = "",
    val duration: String = "",
    val notes: String? = null,
    val nextFollowUp: String? = null,
    val nextFollowUpTime: String? = null,
    val createdAt: String? = null,
)

data class DueMeeting(
    val id: String = "",
    val customerName: String = "",
    val projectName: String = "",
    val time: String? = null,
    val status: String = "",
)

data class CpDueToday(
    val meetings: List<DueMeeting> = emptyList(),
    val followUps: List<CpFollowUp> = emptyList(),
    val callbacks: List<DueMeeting> = emptyList(),
)

// ─── Share link ──────────────────────────────────────────────────────────────

data class ShareLinkResponse(
    val token: String = "",
    val url: String = "",
    val clickCount: Int = 0,
)

// ─── Request bodies ──────────────────────────────────────────────────────────

data class CreateCpLeadRequest(
    val projectId: Long,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String? = null,
    val stage: String = "NEW_LEAD",
)

data class CreateFollowUpRequest(val dealId: Long, val dueDate: String, val dueTime: String? = null, val reason: String)
data class CreateCallLogRequest(
    val dealId: Long,
    val outcome: String,
    val duration: String,
    val notes: String? = null,
    val nextFollowUp: String? = null,
    val nextFollowUpTime: String? = null,
)
data class MeetingNoteRequest(val notes: String, val cpRating: Int? = null)
data class CpMessageRequest(val message: String)
data class CpProfileUpdateRequest(
    val fullName: String? = null,
    val email: String? = null,
    val city: String? = null,
    val bio: String? = null,
    val reraNumber: String? = null,
)

// ─── Meetups ─────────────────────────────────────────────────────────────────
// A gathering the CP arranges themselves — several invitees, one place. Distinct
// from CpMeeting, which is a builder appointment about one project.

data class CpMeetup(
    val id: Long = 0,
    val title: String = "",
    val description: String? = null,
    /** See [com.dealio.app.ui.cp.meetups.MeetupCategory]. */
    val category: String = "SITE_VISIT",
    /** The photograph at the top of the event page. null falls back to the category wash. */
    val coverImage: String? = null,
    /** Venue photographs, in upload order. */
    val photos: List<String> = emptyList(),
    /** What it is about, in the organiser's words — "First-time buyers", "NRI". */
    val topics: List<String> = emptyList(),
    val location: String = "",
    /** What a customer's preferred city is matched against. */
    val city: String? = null,
    val mapsLink: String? = null,
    /** IN_PERSON | ONLINE | HYBRID */
    val mode: String = "IN_PERSON",
    val onlineLink: String? = null,
    val date: String = "",
    val time: String = "",
    val startAt: String? = null,
    val notes: String? = null,
    /** PRIVATE — invite list only. PUBLIC — also discoverable in [city]. */
    val visibility: String = "PUBLIC",
    /** SCHEDULED | CANCELLED */
    val status: String = "SCHEDULED",
    val cancelReason: String? = null,
    val capacity: Int? = null,
    val invitees: List<CpMeetupInvitee> = emptyList(),
    val counts: CpMeetupCounts = CpMeetupCounts(),
    val createdAt: String = "",
) {
    val isCancelled: Boolean get() = status == "CANCELLED"
    val isPublic: Boolean get() = visibility == "PUBLIC"
    /** Full only once a cap is set and the confirmed heads have reached it. */
    val isFull: Boolean get() = capacity != null && counts.goingHeads >= capacity
}

/**
 * Server-computed tallies.
 *
 * [goingHeads] counts people, [going] counts rows — someone bringing two guests
 * is one row and three heads. Capacity is measured in heads.
 */
data class CpMeetupCounts(
    val invited: Int = 0,
    val going: Int = 0,
    val maybe: Int = 0,
    val declined: Int = 0,
    val noReply: Int = 0,
    val checkedIn: Int = 0,
    val goingHeads: Int = 0,
)

data class CpMeetupInvitee(
    val id: Long = 0,
    /** INVITED — the organiser added them. DISCOVERY — they found it themselves. */
    val source: String = "INVITED",
    val contactId: Long? = null,
    /** Set when this person has a Dealio account, so the invite reaches their app. */
    val userId: Long? = null,
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    /** INVITED | GOING | MAYBE | DECLINED */
    val rsvp: String = "INVITED",
    val guests: Int = 0,
    val respondedAt: String? = null,
    val checkedInAt: String? = null,
) {
    val foundItThemselves: Boolean get() = source == "DISCOVERY"
}

data class CreateCpMeetupRequest(
    val title: String,
    val location: String,
    val date: String,
    val time: String,
    val description: String? = null,
    val category: String = "SITE_VISIT",
    val city: String? = null,
    val mapsLink: String? = null,
    val mode: String = "IN_PERSON",
    val onlineLink: String? = null,
    val notes: String? = null,
    val visibility: String = "PUBLIC",
    val capacity: Int? = null,
    val invitees: List<CpMeetupInviteePayload> = emptyList(),
)

/**
 * An edit. Every field is nullable and only what is sent moves, so a partial
 * edit from one screen cannot blank out what another screen set.
 */
data class UpdateCpMeetupRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val location: String? = null,
    val city: String? = null,
    val mapsLink: String? = null,
    val mode: String? = null,
    val onlineLink: String? = null,
    val date: String? = null,
    val time: String? = null,
    val notes: String? = null,
    val visibility: String? = null,
    val capacity: Int? = null,
)

data class CpMeetupInviteePayload(
    val contactId: Long? = null,
    val name: String,
    val phone: String,
    val email: String? = null,
)

data class AddInviteesRequest(val invitees: List<CpMeetupInviteePayload>)

data class SetRsvpRequest(val rsvp: String? = null, val guests: Int? = null)

data class CancelMeetupRequest(val reason: String? = null)

/**
 * Who the organiser can invite: their own contact book, and Dealio customers.
 *
 * Both arrive in one call so the picker can offer two tabs without a second
 * round-trip while a partner is mid-form.
 */
data class InvitableResponse(
    val contacts: List<InvitableContact> = emptyList(),
    val customers: List<InvitableCustomer> = emptyList(),
)

data class InvitableContact(
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val countryCode: String? = null,
    val email: String? = null,
)

data class InvitableCustomer(
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    /** Their preferred city — how the organiser spots who is even local. */
    val city: String? = null,
)
