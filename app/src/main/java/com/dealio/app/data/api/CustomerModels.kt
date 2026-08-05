package com.dealio.app.data.api

// ─── Customer-facing deal (My Journey) ───────────────────────────────────────

data class CustomerDeal(
    val dealId: Long = 0,
    val projectId: Long = 0,
    val projectName: String = "Unknown Project",
    val dealStatus: String = "",
    val dealValue: Double? = null,
    val customerConfirmed: Boolean = false,
    val cpAgreed: Boolean = false,
    val createdAt: String = "",
    val loanCaseId: Long? = null,
    val loanAmount: Double? = null,
    val loanStatus: String? = null,
    val tenureMonths: Int? = null,
    val interestRate: Double? = null,
    val dealDocuments: List<DealDocument> = emptyList(),
    val messages: List<DealMessage> = emptyList(),
)

// ─── Available channel partner (for booking) ─────────────────────────────────

data class AvailableCP(
    val id: Long = 0,
    val userId: Long? = null,
    val fullName: String = "",
    val city: String? = null,
    val tier: String? = null,
)

// ─── Request bodies ──────────────────────────────────────────────────────────

data class BookMeetingRequest(
    val builderId: Long,
    val projectId: Long?,
    val customerName: String,
    val customerPhone: String,
    val preferredDate: String,
    val preferredTime: String,
    val meetingType: String? = null,
    val notes: String? = null,
    val cpUserId: Long? = null,
)

data class ShortlistRequest(
    val customerPhone: String,
    val builderId: Long,
    val projectId: Long,
    val cpId: Long? = null,
    val unitId: String,
    val unitDetails: Map<String, String?>,
)

/** What the save/unsave endpoints answer with — enough to trust the toggle. */
data class SavedProjectResult(
    val projectId: Long = 0,
    val saved: Boolean = false,
)

data class PricingRequest(
    val builderId: Long,
    val projectId: Long,
    val customerPhone: String,
    val unitId: String,
    val unitDetails: Map<String, String?>,
    val note: String? = null,
)

data class LoanApplicationRequest(
    val builderId: Long? = null,
    val projectId: Long? = null,
    val customerName: String? = null,
    val customerPhone: String,
    val customerEmail: String? = null,
    val loanAmount: Double,
    val propertyValue: Double,
    val employmentType: String? = null,
    val tenureMonths: Int,
)

data class RateRequest(val rating: Int)
data class PhoneRequest(val phone: String)
data class CustomerMessageRequest(val phone: String, val recipientRole: String, val message: String)
data class PreferredCityRequest(val city: String?)
data class ProfileUpdateRequest(val email: String?)

// ─── Meetups ─────────────────────────────────────────────────────────────────
// A gathering hosted by a channel partner, as a customer sees it.
//
// Deliberately thinner than the organiser's [CpMeetup]: a customer browsing by
// city is a stranger to this event, so the invite list never crosses the wire.
// What arrives is what a public event page shows — plus this customer's own
// standing on it.

data class CustomerMeetup(
    val id: Long = 0,
    val title: String = "",
    val description: String? = null,
    val category: String = "SITE_VISIT",
    /** The event photograph. null falls back to the category wash. */
    val coverImage: String? = null,
    /** Venue photographs, shown under the location. */
    val photos: List<String> = emptyList(),
    /** What the gathering is about, as the organiser described it. */
    val topics: List<String> = emptyList(),
    val location: String = "",
    val city: String? = null,
    val mapsLink: String? = null,
    val mode: String = "IN_PERSON",
    /** Only sent once this customer is going — the link is for attendees. */
    val onlineLink: String? = null,
    val date: String = "",
    val time: String = "",
    val startAt: String? = null,
    val status: String = "SCHEDULED",
    val cancelReason: String? = null,
    val capacity: Int? = null,
    val hostName: String = "",
    /** Only sent when this customer was personally invited. */
    val hostPhone: String? = null,
    val hostPhoto: String? = null,
    val hostTier: String? = null,
    val goingCount: Int = 0,
    /**
     * First names of a few people who are going, for the row of faces.
     *
     * First names only — this reaches any stranger browsing a public meetup —
     * and shorter than [goingCount], which counts guests they are bringing too.
     */
    val goingNames: List<String> = emptyList(),
    /** True when a partner asked this customer by name, rather than them finding it. */
    val invited: Boolean = false,
    /** null until they answer. INVITED means asked but still silent. */
    val myRsvp: String? = null,
    val myGuests: Int = 0,
) {
    val isCancelled: Boolean get() = status == "CANCELLED"
    val isGoing: Boolean get() = myRsvp == "GOING"
    /** Asked, but has not said yes or no yet. */
    val awaitingReply: Boolean get() = invited && (myRsvp == null || myRsvp == "INVITED")
    val isFull: Boolean get() = capacity != null && goingCount >= capacity && !isGoing
}

/**
 * The meetup feed.
 *
 * Carries the city the server resolved from this customer's preference, so the
 * list can say "On in Hyderabad" rather than a vague "near you" — naming the
 * city is what makes it obvious the list is theirs and not a generic listing.
 */
data class CustomerMeetupFeed(
    val city: String? = null,
    val meetups: List<CustomerMeetup> = emptyList(),
)

data class MeetupRsvpRequest(val rsvp: String, val guests: Int = 0)
