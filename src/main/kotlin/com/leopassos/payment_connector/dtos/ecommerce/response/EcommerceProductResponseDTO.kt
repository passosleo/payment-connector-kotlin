package com.leopassos.payment_connector.dtos.ecommerce.response

import com.fasterxml.jackson.annotation.JsonProperty

data class EcommerceProductResponseDTO(
    val id: String,
    val title: String,
    val currency: String,
    @field:JsonProperty("unit_amount")
    val unitAmount: Long,
    @field:JsonProperty("inventory_quantity")
    val inventoryQuantity: Int,
)
