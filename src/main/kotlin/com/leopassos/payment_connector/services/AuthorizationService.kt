package com.leopassos.payment_connector.services

import com.leopassos.payment_connector.dtos.connector.request.AuthorizationRequestDTO
import com.leopassos.payment_connector.dtos.connector.response.AuthorizationResponseDTO
import com.leopassos.payment_connector.enums.PaymentMethod

/**
 * Define o contrato para provedores de autorização de pagamento.
 *
 * Implementações autorizam uma [AuthorizationRequestDTO] para um [PaymentMethod] específico e retornam uma
 * [AuthorizationResponseDTO] com o resultado do processamento no gateway.
 */
interface AuthorizationService {

    /**
     * Método de pagamento suportado por este serviço de autorização.
     */
    val paymentMethod: PaymentMethod

    /**
     * Autoriza a requisição de pagamento no provedor responsável.
     *
     * @param payload dados de autorização recebidos pela API de pagamentos.
     * @return resultado da autorização produzido pelo provedor de pagamento.
     */
    fun authorize(payload: AuthorizationRequestDTO): AuthorizationResponseDTO
}
