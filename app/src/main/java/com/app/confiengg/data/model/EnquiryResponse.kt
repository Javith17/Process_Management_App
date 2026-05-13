package com.app.confiengg.data.model

data class EnquiryResponse(
    val list: List<Enquiry>? = null,
    val count: Int? = null
)

data class Enquiry(
    val id: String? = null,
    val customer_name: String? = null,
    val machine_name: String? = null,
    val contact_no: String? = null,
    val address: Address? = null,
    val gst_no: String? = null,
    val enquiry_resource: String? = null,
    val remarks: String? = null,
    val level1_user: Level1User? = null,
    val quotation_terms: List<String>? = null
)

data class Address(
    val address_1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postal_code: String? = null
)

data class Level1User(
    val emp_name: String? = null
)
