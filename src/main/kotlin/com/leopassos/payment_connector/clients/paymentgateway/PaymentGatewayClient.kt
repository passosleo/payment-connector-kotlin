package com.leopassos.payment_connector.clients.paymentgateway

import com.leopassos.payment_connector.dtos.paymentgateway.request.CreatePaymentIntentRequestDTO
import com.leopassos.payment_connector.dtos.paymentgateway.response.PaymentIntentResponseDTO
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

/**
 * Contrato HTTP declarativo da integração com o gateway de pagamento.
 *
 * Expõe somente as operações externas consumidas pela aplicação. Configuração de URL, timeout, retry, logs e tratamento
 * de erro ficam centralizados em [com.leopassos.payment_connector.clients.HttpClientFactory].
 */
@HttpExchange
fun interface PaymentGatewayClient {

    /**
     * Cria uma intenção de pagamento no gateway externo.
     *
     * @param request dados esperados pela API do gateway.
     * @return intenção de pagamento criada pelo gateway.
     */
    @PostExchange("/v1/payment_intents")
    fun createPaymentIntent(
        @RequestBody request: CreatePaymentIntentRequestDTO
    ): PaymentIntentResponseDTO
}
