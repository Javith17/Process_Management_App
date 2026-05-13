package com.app.confiengg.data.model

data class ApproveQuotationRequest(
    val quotation_id: String?,
    val quotation_type: String = "machine",
    val status: String = "Draft Approval",
    val qty: String?,
    val approved_cost: String?
)
