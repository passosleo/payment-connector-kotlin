package com.leopassos.payment_connector.dtos.paymentgateway.request

import com.fasterxml.jackson.annotation.JsonProperty

data class CreatePaymentIntentRequestDTO(
    val amount: Long,
    val currency: String,
    @field:JsonProperty("payment_method")
    val paymentMethod: String,
    val confirm: Boolean = true,
)
