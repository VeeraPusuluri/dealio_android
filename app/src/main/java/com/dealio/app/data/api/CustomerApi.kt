package com.dealio.app.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Customer-facing endpoints: discovery under "customer/..." and the customer
 * portal under "portal/customer/..." (plus a couple of "builder/customer/..."
 * routes). Portal calls are keyed by the signed-in customer's phone number.
 */
interface CustomerApi {

    // ── Discovery ───────────────────────────────────────────────────────────
    @GET("customer/cities")
    suspend fun getCities(): Response<ApiEnvelope<List<String>>>

    // ── Meetups ─────────────────────────────────────────────────────────────
    // Gatherings hosted by channel partners. The list is everything this
    // customer was personally invited to, plus public ones in the city they
    // follow — the server decides which, from their preferred city.

    @GET("customer/meetups")
    suspend fun getMeetups(
        @Query("city") city: String? = null,
        @Query("category") category: String? = null,
    ): Response<ApiEnvelope<CustomerMeetupFeed>>

    @GET("customer/meetups/{id}")
    suspend fun getMeetup(@Path("id") id: Long): Response<ApiEnvelope<CustomerMeetup>>

    @POST("customer/meetups/{id}/rsvp")
    suspend fun rsvpMeetup(
        @Path("id") id: Long,
        @Body body: MeetupRsvpRequest,
    ): Response<ApiEnvelope<CustomerMeetup>>

    @GET("customer/projects")
    suspend fun getProjects(@Query("city") city: String? = null): Response<ApiEnvelope<List<Project>>>

    @GET("customer/projects/{id}")
    suspend fun getProject(@Path("id") id: Long): Response<ApiEnvelope<Project>>

    /** Project photos / floor plans / brochures (shared with the builder doc vault). */
    @GET("builder/{builderId}/projects/{projectId}/documents")
    suspend fun getProjectDocuments(
        @Path("builderId") builderId: Long,
        @Path("projectId") projectId: Long,
    ): Response<ApiEnvelope<List<ProjectDocument>>>

    @GET("customer/cps")
    suspend fun getAvailableCPs(): Response<ApiEnvelope<List<AvailableCP>>>

    // ── Profile / notifications ───────────────────────────────────────────────
    @PATCH("customer/preferred-city")
    suspend fun setPreferredCity(@Body body: PreferredCityRequest): Response<ApiEnvelope<Any>>

    @PATCH("customer/profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Response<ApiEnvelope<Any>>

    @GET("customer/notifications")
    suspend fun getNotifications(): Response<ApiEnvelope<List<BuilderNotification>>>

    @PATCH("customer/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiEnvelope<Any>>

    /** Marks one notification read. The list endpoint returns unread-only, so
     *  without this a tapped notification comes straight back on the next load. */
    @PATCH("customer/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<ApiEnvelope<Any>>

    // ── Meetings / site visits ────────────────────────────────────────────────
    @GET("portal/customer/meetings")
    suspend fun getMyMeetings(@Query("phone") phone: String): Response<ApiEnvelope<List<Meeting>>>

    @GET("portal/customer/booked-slots")
    suspend fun getBookedSlots(
        @Query("builderId") builderId: Long,
        @Query("date") date: String,
    ): Response<ApiEnvelope<List<String>>>

    @POST("portal/customer/meetings")
    suspend fun bookMeeting(@Body body: BookMeetingRequest): Response<ApiEnvelope<Any>>

    @PATCH("portal/customer/meetings/{id}/rating")
    suspend fun rateMeeting(@Path("id") id: Long, @Body body: RateRequest): Response<ApiEnvelope<Any>>

    // ── Deals (My Journey) ──────────────────────────────────────────────────
    @GET("portal/customer/deals")
    suspend fun getMyDeals(@Query("phone") phone: String): Response<ApiEnvelope<List<CustomerDeal>>>

    @PATCH("builder/customer/deals/{dealId}/confirm")
    suspend fun confirmDeal(@Path("dealId") dealId: Long, @Body body: PhoneRequest): Response<ApiEnvelope<Any>>

    @PATCH("portal/customer/deals/{dealId}/accept-negotiation")
    suspend fun acceptNegotiation(@Path("dealId") dealId: Long, @Body body: PhoneRequest): Response<ApiEnvelope<Any>>

    @POST("portal/customer/deals/{dealId}/messages")
    suspend fun sendDealMessage(@Path("dealId") dealId: Long, @Body body: CustomerMessageRequest): Response<ApiEnvelope<Any>>

    /**
     * The buyer's half of Agreement: submit the countersigned copy.
     *
     * The builder's `accept-agreement` refuses with a 400 until a signed
     * agreement exists on the deal, so without this the deal cannot leave
     * Agreement from the app at all.
     */
    @Multipart
    @POST("builder/customer/deals/{dealId}/signed-agreement")
    suspend fun uploadSignedAgreement(
        @Path("dealId") dealId: Long,
        @Part file: MultipartBody.Part,
        @Part("phone") phone: RequestBody,
    ): Response<ApiEnvelope<DealDocument>>

    // ── Saved projects (bookmarks) ────────────────────────────────────────────
    // The caller is read from the token, so no phone travels in the request.

    @GET("customer/saved-projects")
    suspend fun getSavedProjects(): Response<ApiEnvelope<List<Project>>>

    @POST("customer/saved-projects/{projectId}")
    suspend fun saveProject(@Path("projectId") projectId: Long): Response<ApiEnvelope<SavedProjectResult>>

    @DELETE("customer/saved-projects/{projectId}")
    suspend fun unsaveProject(@Path("projectId") projectId: Long): Response<ApiEnvelope<SavedProjectResult>>

    // ── Shortlists ────────────────────────────────────────────────────────────
    @GET("portal/customer/shortlist")
    suspend fun getMyShortlists(@Query("phone") phone: String): Response<ApiEnvelope<List<Shortlist>>>

    @POST("portal/customer/shortlist")
    suspend fun shortlistUnit(@Body body: ShortlistRequest): Response<ApiEnvelope<Any>>

    @POST("portal/customer/pricing-requests")
    suspend fun requestPricing(@Body body: PricingRequest): Response<ApiEnvelope<Any>>

    // ── Home loan ─────────────────────────────────────────────────────────────
    @POST("portal/customer/applications")
    suspend fun submitLoanApplication(@Body body: LoanApplicationRequest): Response<ApiEnvelope<Any>>
}
