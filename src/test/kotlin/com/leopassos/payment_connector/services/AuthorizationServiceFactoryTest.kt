package com.leopassos.payment_connector.services

import com.leopassos.payment_connector.dtos.connector.request.AuthorizationRequestDTO
import com.leopassos.payment_connector.dtos.connector.response.AuthorizationResponseDTO
import com.leopassos.payment_connector.enums.PaymentMethod
import com.leopassos.payment_connector.exceptions.ApplicationException
import com.leopassos.payment_connector.exceptions.Errors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthorizationServiceFactoryTest {

    @Test
    fun `create returns service registered for payment method`() {
        val pixService = StubAuthorizationService(PaymentMethod.PIX)
        val cardService = StubAuthorizationService(PaymentMethod.CREDIT_CARD)
        val factory = AuthorizationServiceFactory(listOf(pixService, cardService))

        val service = factory.create(PaymentMethod.CREDIT_CARD)

        assertSame(cardService, service)
    }

    @Test
    fun `create throws application exception when payment method has no registered service`() {
        val factory = AuthorizationServiceFactory(
            listOf(StubAuthorizationService(PaymentMethod.PIX)),
        )

        val exception = assertThrows<ApplicationException> {
            factory.create(PaymentMethod.BOLETO)
        }

        assertEquals(Errors.PAYMENT_METHOD_NOT_SUPPORTED, exception.error)
        assertEquals("Payment method BOLETO is not supported", exception.message)
    }

    private class StubAuthorizationService(
        override val paymentMethod: PaymentMethod,
    ) : AuthorizationService {

        override fun authorize(payload: AuthorizationRequestDTO): AuthorizationResponseDTO {
            error("Not used in this test")
        }
    }
}
