package com.app.confiengg.data.model

data class ReviseQuotationRequest(
    val quotation_id: String?,
    val reminder_date: String?,
    val qty: String?,
    val cost: String?,
    val remarks: String?,
    val quotation_terms: List<String>?
)
