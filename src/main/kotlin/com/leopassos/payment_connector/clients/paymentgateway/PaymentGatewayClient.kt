package com.leopassos.payment_connector.clients.paymentgateway

import com.leopassos.payment_connector.dtos.paymentgateway.request.CreatePaymentIntentRequestDTO
import com.leopassos.payment_connector.dtos.paymentgateway.response.PaymentIntentResponseDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking

/**
 * Contrato da integração com o gateway de pagamento.
 *
 * Define apenas as operações externas usadas pela aplicação. Configuração de URL base, timeout, retry, autenticação e
 * logs fica centralizada em [com.leopassos.payment_connector.clients.HttpClientFactory].
 */
fun interface PaymentGatewayClient {

    /**
     * Cria uma intenção de pagamento no gateway externo.
     *
     * @param request dados da intenção de pagamento.
     * @return intenção de pagamento criada pelo gateway.
     */
    fun createPaymentIntent(
        request: CreatePaymentIntentRequestDTO
    ): PaymentIntentResponseDTO
}

/**
 * Implementação Ktor do [PaymentGatewayClient].
 *
 * @property httpClient client HTTP Ktor configurado para o gateway de pagamento.
 */
class KtorPaymentGatewayClient(
    private val httpClient: HttpClient,
) : PaymentGatewayClient {

    /**
     * Cria uma intenção de pagamento usando form-data, formato esperado pelo Stripe Mock.
     *
     * @param request dados da intenção de pagamento.
     * @return intenção de pagamento criada pelo gateway.
     */
    override fun createPaymentIntent(request: CreatePaymentIntentRequestDTO): PaymentIntentResponseDTO {
        return runBlocking {
            httpClient.submitForm(
                url = "/v1/payment_intents",
                formParameters = parameters {
                    append("amount", request.amount.toString())
                    append("currency", request.currency)
                    append("payment_method", request.paymentMethod)
                    append("confirm", request.confirm.toString())
                },
            ).body()
        }
    }
}
