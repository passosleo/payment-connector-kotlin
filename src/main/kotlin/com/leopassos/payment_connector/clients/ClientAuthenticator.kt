package com.leopassos.payment_connector.clients

import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpMessageBuilder

/**
 * Aplica autenticação em chamadas HTTP externas.
 *
 * A implementação pode usar token fixo, token em cache ou token gerado programaticamente a cada requisição.
 */
fun interface ClientAuthenticator {

    /**
     * Aplica os dados de autenticação no request.
     *
     * @param request builder da mensagem HTTP que será enviada para a API externa.
     */
    fun authenticate(request: HttpMessageBuilder)

    companion object {

        /**
         * Cria um autenticador bearer.
         *
         * @param tokenProvider função chamada por requisição para obter o token atual.
         * @return autenticador que aplica `Authorization: Bearer ...` quando o token não está em branco.
         */
        fun bearer(tokenProvider: () -> String?): ClientAuthenticator {
            return ClientAuthenticator { request ->
                tokenProvider()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { request.bearerAuth(it) }
            }
        }
    }
}
