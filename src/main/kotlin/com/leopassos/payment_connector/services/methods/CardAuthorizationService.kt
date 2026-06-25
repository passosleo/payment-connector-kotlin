package com.leopassos.payment_connector.services.methods

import com.leopassos.payment_connector.configuration.logger
import com.leopassos.payment_connector.dtos.connector.request.AuthorizationRequestDTO
import com.leopassos.payment_connector.dtos.connector.response.AuthorizationResponseDTO
import com.leopassos.payment_connector.enums.PaymentMethod
import com.leopassos.payment_connector.services.AuthorizationService
import org.springframework.stereotype.Service

private val log = logger<CardAuthorizationService>()

@Service
class CardAuthorizationService : AuthorizationService {

    override val paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD

    override fun authorize(payload: AuthorizationRequestDTO): AuthorizationResponseDTO {
        log.info("Iniciando autorização de cartão de crédito para o pedido {}", payload.ecommerceOrderId)
        throw NotImplementedError()
    }
}
