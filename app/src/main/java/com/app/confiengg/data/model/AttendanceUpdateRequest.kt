package com.app.confiengg.data.model

data class AttendanceUpdateRequest(
    val user_id: String,
    val attendance_date: String,
    val type: String,
    val time: Long,
    val location: String
)
