package com.app.confiengg.data.model

data class MonthlyAttendanceResponse(
    val status: Boolean,
    val message: String,
    val data: MonthlyAttendanceData?
)

data class MonthlyAttendanceData(
    val summary: AttendanceSummary?,
    val attendance_list: List<AttendanceData>?,
    val no_of_working_days: String? = null,
    val no_of_leave_days: String? = null
)

data class AttendanceSummary(
    val presentCount: Int,
    val absentCount: Int,
    val halfDayCount: Int,
    val paidLeaveCount: Int,
    val weekOffCount: Int,
    val holidayCount: Int
)
