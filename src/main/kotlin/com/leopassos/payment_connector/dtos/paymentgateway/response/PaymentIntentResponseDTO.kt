package com.leopassos.payment_connector.dtos.paymentgateway.response

/**
 * Resposta de criação de PaymentIntent retornada pelo gateway.
 *
 * @property id identificador da PaymentIntent no gateway.
 * @property status status textual retornado pela API externa.
 * @property amount valor na menor unidade da moeda.
 * @property currency moeda da PaymentIntent.
 */
data class PaymentIntentResponseDTO(
    val id: String,
    val status: String,
    val amount: Long,
    val currency: String,
)
