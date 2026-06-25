package com.leopassos.payment_connector.dtos.connector.response

import com.leopassos.payment_connector.enums.PaymentTransactionStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Corpo da resposta retornado após uma tentativa de autorização de pagamento.
 *
 * @property ecommerceOrderId identificador do pedido na plataforma de ecommerce.
 * @property gatewayPaymentId identificador do pagamento no gateway, quando disponível.
 * @property status [PaymentTransactionStatus] resultante da tentativa de autorização.
 */
@Schema(description = "Dados retornados após uma tentativa de autorização de pagamento")
data class AuthorizationResponseDTO(
    @field:Schema(
        description = "Identificador do pedido na plataforma de ecommerce",
        example = "order-123",
    )
    val ecommerceOrderId: String,
    @field:Schema(
        description = "Identificador do pagamento no gateway",
        example = "pay_123",
        nullable = true,
    )
    val gatewayPaymentId: String?,
    @field:Schema(
        description = "Status resultante da autorização",
        example = "AUTHORIZED",
    )
    val status: PaymentTransactionStatus,
)
