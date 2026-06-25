package com.leopassos.payment_connector.dtos.connector.request

import com.leopassos.payment_connector.enums.PaymentMethod
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

/**
 * Corpo da requisição usado para autorizar um pagamento.
 *
 * @property paymentMethod método de pagamento que define qual provedor de autorização será usado.
 * @property ecommerceOrderId identificador do pedido na plataforma de ecommerce.
 * @property amountInCents valor a ser autorizado, representado na menor unidade da moeda.
 * @property currency código ISO 4217 da moeda usada no valor da autorização.
 */
@Schema(description = "Corpo da requisição usado para autorizar um pagamento")
data class AuthorizationRequestDTO(
    @field:Schema(
        description = "Método de pagamento usado na autorização",
        example = "CREDIT_CARD",
    )
    val paymentMethod: PaymentMethod,
    @field:Schema(
        description = "Identificador do pedido na plataforma de ecommerce",
        example = "order-123",
    )
    val ecommerceOrderId: String,
    @field:Schema(
        description = "Valor a ser autorizado, em centavos",
        example = "12990",
    )
    @field:Positive(message = "amountInCents must be greater than zero")
    val amountInCents: Long,
    @field:Schema(
        description = "Código ISO 4217 da moeda",
        example = "BRL",
    )
    val currency: String,
)
