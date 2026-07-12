package com.app.confiengg.data.api

import com.app.confiengg.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/signIn")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("admin/enquiries")
    suspend fun getEnquiries(
        @Query("limit") limit: Int,
        @Query("page") page: Int,
        @Query("status") status: String,
        @Query("user") user: String? = null,
        @Query("search") search: String? = null
    ): Response<EnquiryResponse>

    @POST("admin/updateEnquiryStatus")
    suspend fun updateEnquiryStatus(@Body request: UpdateEnquiryRequest): Response<Unit>

    @POST("admin/updateNotificationToken")
    suspend fun updateNotificationToken(@Body request: UpdateTokenRequest): Response<Unit>

    @FormUrlEncoded
    @POST("admin/marketingDashboard")
    suspend fun getMarketingDashboard(@Field("user_id") userId: String): Response<DashboardResponse>

    @GET("quotation/machineQuotationList")
    suspend fun getQuotations(@Query("userId") userId: String?): Response<QuotationResponse>

    @GET("quotation/quotation_doc/{id}/machine")
    suspend fun getQuotationDoc(@Path("id") id: String): Response<ResponseBody>

    @POST("quotation/reviseMachineQuotation")
    suspend fun reviseQuotation(@Body request: ReviseQuotationRequest): Response<Unit>

    @POST("quotation/approveRejectQuotation")
    suspend fun approveQuotation(@Body request: ApproveQuotationRequest): Response<Unit>

    @GET("machine/machineAttachmentLinks/{machine_id}")
    suspend fun getMachineAttachments(@Path("machine_id") machineId: String): Response<AttachmentResponse>

    @POST("user/attendance-update")
    suspend fun updateAttendance(@Body request: AttendanceUpdateRequest): Response<Unit>

    @Multipart
    @POST("user/attendence-image-upload")
    suspend fun uploadAttendanceImage(
        @Part("user_id") userId: okhttp3.RequestBody,
        @Part("attendance_date") attendanceDate: okhttp3.RequestBody,
        @Part files: List<okhttp3.MultipartBody.Part>
    ): Response<Unit>

    @POST("user/daily-user-attendance")
    suspend fun getDailyAttendance(
        @Body request: Map<String, String>
    ): Response<DailyAttendanceResponse>

    @POST("user/monthly-user-attendance")
    suspend fun getMonthlyAttendance(
        @Body request: Map<String, String>
    ): Response<MonthlyAttendanceResponse>

    @POST("user/leave-request")
    suspend fun applyLeave(@Body request: Map<String, String>): Response<Unit>

    @POST("user/leave-request-list")
    suspend fun getLeaveRequests(@Body request: Map<String, String>): Response<LeaveRequestListResponse>

    @POST("user/location-alert")
    suspend fun sendLocationAlert(@Body request: Map<String, String>): Response<Unit>

    @POST("user/screen-alert")
    suspend fun sendScreenAlert(@Body request: Map<String, String>): Response<Unit>

    @POST("auth/sendOTP")
    suspend fun sendOTP(@Body request: Map<String, String>): Response<Unit>

    @POST("auth/verifyOTP")
    suspend fun verifyOTP(@Body request: Map<String, String>): Response<LoginResponse>
}
