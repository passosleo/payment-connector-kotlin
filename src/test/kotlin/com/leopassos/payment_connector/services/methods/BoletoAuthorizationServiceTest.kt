package com.leopassos.payment_connector.services.methods

import com.leopassos.payment_connector.clients.paymentgateway.PaymentGatewayClient
import com.leopassos.payment_connector.dtos.connector.request.AuthorizationRequestDTO
import com.leopassos.payment_connector.dtos.paymentgateway.request.CreatePaymentIntentRequestDTO
import com.leopassos.payment_connector.dtos.paymentgateway.response.PaymentIntentResponseDTO
import com.leopassos.payment_connector.enums.PaymentMethod
import com.leopassos.payment_connector.enums.PaymentTransactionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BoletoAuthorizationServiceTest {

    @Test
    fun `authorizes boleto using payment gateway client`() {
        var gatewayRequest: CreatePaymentIntentRequestDTO? = null
        val service = BoletoAuthorizationService(
            PaymentGatewayClient { request ->
                gatewayRequest = request
                PaymentIntentResponseDTO(
                    id = "pi_boleto_123",
                    status = "processing",
                    amount = request.amount,
                    currency = request.currency,
                )
            },
        )

        val response = service.authorize(
            AuthorizationRequestDTO(
                paymentMethod = PaymentMethod.BOLETO,
                ecommerceOrderId = "order-123",
                amountInCents = 12990,
                currency = "BRL",
            ),
        )

        assertEquals(
            CreatePaymentIntentRequestDTO(
                amount = 12990,
                currency = "brl",
                paymentMethod = "boleto",
            ),
            gatewayRequest,
        )
        assertEquals("order-123", response.ecommerceOrderId)
        assertEquals("pi_boleto_123", response.gatewayPaymentId)
        assertEquals(PaymentTransactionStatus.AUTHORIZED, response.status)
    }
}
