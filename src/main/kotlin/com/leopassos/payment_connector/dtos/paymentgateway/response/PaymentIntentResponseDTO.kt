package com.leopassos.payment_connector.dtos.paymentgateway.response

data class PaymentIntentResponseDTO(
    val id: String,
    val status: String,
    val amount: Long,
    val currency: String,
)
