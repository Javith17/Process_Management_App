package com.app.confiengg.data.model

import com.google.gson.annotations.SerializedName

data class LeaveRequestListResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<LeaveRequestData>?
)

data class LeaveRequestData(
    @SerializedName("leave_date") val leaveDate: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("description") val description: String?
)
