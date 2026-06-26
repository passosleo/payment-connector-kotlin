package com.leopassos.payment_connector.dtos.paymentgateway.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Dados necessários para criar uma intenção de pagamento no gateway.
 *
 * @property amount valor na menor unidade da moeda.
 * @property currency moeda da intenção de pagamento.
 * @property paymentMethod método de pagamento aceito pelo gateway.
 * @property confirm indica se o gateway deve confirmar a intenção no momento da criação.
 */
data class CreatePaymentIntentRequestDTO(
    val amount: Long,
    val currency: String,
    @field:JsonProperty("payment_method")
    val paymentMethod: String,
    val confirm: Boolean = true,
)
