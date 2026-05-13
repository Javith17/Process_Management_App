package com.app.confiengg.data.model

data class QuotationResponse(
    val list: List<Quotation>? = null,
    val count: Int? = null
)

data class Quotation(
    val id: String? = null,
    val quotation_no: String? = null,
    val status: String? = null,
    val customer: QuotationCustomer? = null,
    val machine: QuotationMachine? = null,
    val created_at: String? = null,
    val quotation_date: String? = null,
    val reminder_date: String? = null,
    val cost: String? = null,
    val initial_cost: String? = null,
    val qty: String? = null,
    val remarks: String? = null,
    val quotation_terms: List<String>? = null
)

data class QuotationCustomer(
    val customer_name: String? = null
)

data class QuotationMachine(
    val id: String? = null,
    val machine_name: String? = null
)
