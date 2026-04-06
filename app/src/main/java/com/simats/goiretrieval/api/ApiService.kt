package com.simats.goiretrieval.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("signup")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    @POST("signin")
    fun signin(@Body request: SigninRequest): Call<SignupResponse>

    @POST("forgot_password")
    fun forgotPassword(@Body request: ForgotRequest): Call<SignupResponse>

    @POST("verify_otp")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<SignupResponse>

    @POST("reset_password")
    fun resetPassword(@Body request: ResetRequest): Call<SignupResponse>

    @GET("documents")
    fun getDocuments(
        @Query("category") category: String,
        @Query("user_id") userId: Int? = null
    ): Call<DocumentsResponse>

    @GET("search")
    fun searchDocuments(@Query("q") query: String): Call<DocumentsResponse>

    @POST("saved_docs")
    fun saveDocument(@Body request: SavedDocRequest): Call<SignupResponse>

    @GET("saved_docs/{user_id}")
    fun getSavedDocuments(@Path("user_id") userId: Int): Call<DocumentsResponse>

    @DELETE("saved_docs/{user_id}/{doc_id}")
    fun deleteSavedDocument(@Path("user_id") userId: Int, @Path("doc_id") docId: String): Call<SignupResponse>

    @GET("content/{folder}/{filename}")
    fun getDocumentContent(
        @Path("folder") folder: String,
        @Path("filename") filename: String,
        @Query("view") view: Boolean = true
    ): Call<ContentResponse>

    @POST("recent_views")
    fun recordView(@Body request: SavedDocRequest): Call<SignupResponse>

    @GET("recent_views/{user_id}")
    fun getRecentViews(@Path("user_id") userId: Int): Call<DocumentsResponse>

    @GET("notifications/{user_id}")
    fun getNotifications(@Path("user_id") userId: Int): Call<NotificationsResponse>

    @POST("notifications/mark_read")
    fun markNotificationsRead(@Body request: MarkReadRequest): Call<SignupResponse>

    @POST("activities")
    fun logActivity(@Body request: ActivityLogRequest): Call<SignupResponse>

    @GET("activities/{user_id}")
    fun getActivities(@Path("user_id") userId: Int): Call<RecentActivityResponse>

    @retrofit2.http.Multipart
    @POST("ai_audit_bulk")
    fun aiAuditBulk(
        @retrofit2.http.Part("user_id") userId: okhttp3.RequestBody,
        @retrofit2.http.Part("source_url") sourceUrl: okhttp3.RequestBody?,
        @retrofit2.http.Part("questions") questions: okhttp3.RequestBody?,
        @retrofit2.http.Part("mode") mode: okhttp3.RequestBody,
        @retrofit2.http.Part files: List<okhttp3.MultipartBody.Part>
    ): Call<AuditResponse>

    @POST("export_audit_pdf")
    @retrofit2.http.Streaming
    fun exportAuditPdf(@Body request: BriefingExportRequest): Call<okhttp3.ResponseBody>

    @POST("delete_report")
    fun deleteReport(@Body request: Map<String, String>): Call<SignupResponse>

    @POST("chatbot")
    fun chatbot(@Body request: ChatBotRequest): Call<ChatBotResponse>

    @POST("update_profile")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<SignupResponse>

    @GET("stats")
    fun getGlobalStats(@Query("user_id") userId: Int? = null): Call<GlobalStatsResponse>

    @POST("update_2fa")
    fun update2FA(@Body request: Update2FARequest): Call<SignupResponse>

    @POST("verify_2fa")
    fun verify2FA(@Body request: Verify2FARequest): Call<SignupResponse>

    @GET("devices/{user_id}")
    fun getDevices(@Path("user_id") userId: Int): Call<DevicesResponse>

    @DELETE("logout_device/{device_id}")
    fun logoutDevice(@Path("device_id") deviceId: Int): Call<SignupResponse>

    @DELETE("logout_all/{user_id}")
    fun logoutAllDevices(@Path("user_id") userId: Int): Call<SignupResponse>

    @POST("notifications/send")
    fun sendNotification(@Body request: NotificationCreateRequest): Call<SignupResponse>

    @DELETE("delete_account/{user_id}")
    fun deleteAccount(@Path("user_id") userId: Int): Call<SignupResponse>

    @GET("content/Reports/{filename}")
    @retrofit2.http.Streaming
    fun downloadReport(@Path("filename") filename: String): Call<okhttp3.ResponseBody>
}

