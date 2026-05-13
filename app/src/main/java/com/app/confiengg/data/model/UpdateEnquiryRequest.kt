package com.app.confiengg.data.model

data class UpdateEnquiryRequest(
    val enquiry_id: String,
    val status: String,
    val level2_user: String? = null,
    val remarks: String? = null,
    val quotation_date: String? = null,
    val reminder_date: String? = null,
    val cost: Double? = null,
    val qty: Int? = null,
    val approved_by: String? = null,
    val quotation_terms: List<String>? = null
)
