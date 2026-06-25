package com.leopassos.payment_connector.dtos.ecommerce.response

import com.fasterxml.jackson.annotation.JsonProperty

data class EcommerceOrderResponseDTO(
    val id: String,
    val status: String,
    val currency: String,
    @field:JsonProperty("total_amount")
    val totalAmount: Long,
    @field:JsonProperty("customer_email")
    val customerEmail: String,
    val items: List<EcommerceOrderItemResponseDTO> = emptyList(),
)

data class EcommerceOrderItemResponseDTO(
    @field:JsonProperty("product_id")
    val productId: String,
    val quantity: Int,
    @field:JsonProperty("unit_amount")
    val unitAmount: Long,
)
