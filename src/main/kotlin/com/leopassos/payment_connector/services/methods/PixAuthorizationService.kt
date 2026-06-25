package com.leopassos.payment_connector.services.methods

import com.leopassos.payment_connector.configuration.logger
import com.leopassos.payment_connector.dtos.connector.request.AuthorizationRequestDTO
import com.leopassos.payment_connector.dtos.connector.response.AuthorizationResponseDTO
import com.leopassos.payment_connector.enums.PaymentMethod
import com.leopassos.payment_connector.services.AuthorizationService
import org.springframework.stereotype.Service

private val log = logger<PixAuthorizationService>()

@Service
class PixAuthorizationService : AuthorizationService {

    override val paymentMethod: PaymentMethod = PaymentMethod.PIX

    override fun authorize(payload: AuthorizationRequestDTO): AuthorizationResponseDTO {
        log.info("Iniciando autorização de PIX para o pedido {}", payload.ecommerceOrderId)
        throw NotImplementedError()
    }

}
