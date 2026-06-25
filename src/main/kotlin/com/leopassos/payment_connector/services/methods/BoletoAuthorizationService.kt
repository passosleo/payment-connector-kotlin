package com.leopassos.payment_connector.services.methods

import com.leopassos.payment_connector.clients.paymentgateway.PaymentGatewayClient
import com.leopassos.payment_connector.configuration.logger
import com.leopassos.payment_connector.dtos.connector.request.AuthorizationRequestDTO
import com.leopassos.payment_connector.dtos.connector.response.AuthorizationResponseDTO
import com.leopassos.payment_connector.dtos.paymentgateway.request.CreatePaymentIntentRequestDTO
import com.leopassos.payment_connector.dtos.paymentgateway.response.PaymentIntentResponseDTO
import com.leopassos.payment_connector.enums.PaymentMethod
import com.leopassos.payment_connector.enums.PaymentTransactionStatus
import com.leopassos.payment_connector.services.AuthorizationService
import org.springframework.stereotype.Service

private val log = logger<BoletoAuthorizationService>()

@Service
class BoletoAuthorizationService(
    private val paymentGatewayClient: PaymentGatewayClient,
) : AuthorizationService {

    override val paymentMethod: PaymentMethod = PaymentMethod.BOLETO

    override fun authorize(payload: AuthorizationRequestDTO): AuthorizationResponseDTO {
        log.info("Iniciando autorização de boleto para o pedido {}", payload.ecommerceOrderId)

        val paymentIntent = paymentGatewayClient.createPaymentIntent(
            CreatePaymentIntentRequestDTO(
                amount = payload.amountInCents,
                currency = payload.currency.lowercase(),
                paymentMethod = payload.paymentMethod.name.lowercase(),
            ),
        )

        return AuthorizationResponseDTO(
            ecommerceOrderId = payload.ecommerceOrderId,
            gatewayPaymentId = paymentIntent.id,
            status = paymentIntent.toTransactionStatus(),
        )
    }

    private fun PaymentIntentResponseDTO.toTransactionStatus(): PaymentTransactionStatus {
        return when (status.lowercase()) {
            "succeeded", "processing", "requires_capture" -> PaymentTransactionStatus.AUTHORIZED
            "canceled" -> PaymentTransactionStatus.CANCELED
            "requires_payment_method" -> PaymentTransactionStatus.FAILED
            else -> PaymentTransactionStatus.PENDING
        }
    }
}
