package com.app.confiengg.data.model

data class DailyAttendanceResponse(
    val message: String,
    val data: AttendanceData?
)

data class AttendanceData(
    val attendance_date: String?,
    val date: String? = null,
    val check_in_time: String?,
    val check_out_time: String?,
    val total_working_hrs: String?,
    val location_details: LocationDetails?,
    val is_break: Boolean?,
    val break_details: List<BreakDetail>?,
    val total_break_hrs: String?,
    val is_leave: Boolean?,
    val is_verified: Boolean?,
    val remarks: String?,
    val total_ot: String? = null,
    val attachment_links: List<String>? = null
)

data class LocationDetails(
    val check_in_location: String?,
    val check_out_location: String?
)

data class BreakDetail(
    val break_in: String?,
    val break_out: String?
)
