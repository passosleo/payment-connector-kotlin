package com.leopassos.payment_connector.services

import com.leopassos.payment_connector.enums.PaymentMethod
import com.leopassos.payment_connector.exceptions.ApplicationException
import com.leopassos.payment_connector.exceptions.Errors
import org.springframework.stereotype.Component

/**
 * Resolve a implementação de [AuthorizationService] que suporta o [PaymentMethod] solicitado.
 *
 * @param authorizationServices serviços registrados no contexto do Spring.
 */
@Component
class AuthorizationServiceFactory(
    authorizationServices: List<AuthorizationService>
) {

    /**
     * Tabela de busca que associa cada [PaymentMethod] ao seu [AuthorizationService].
     */
    private val authorizationServiceMap: Map<PaymentMethod, AuthorizationService> =
        authorizationServices.associateBy { it.paymentMethod }

    /**
     * Obtém, por busca, o serviço de autorização para o método de pagamento informado.
     *
     * @param paymentMethod método de pagamento solicitado pelo cliente.
     * @return serviço responsável por autorizar o método de pagamento solicitado.
     * @throws ApplicationException quando nenhum [AuthorizationService] está registrado para [paymentMethod].
     */
    fun create(paymentMethod: PaymentMethod): AuthorizationService {
        return authorizationServiceMap[paymentMethod]
            ?: throw ApplicationException(Errors.PAYMENT_METHOD_NOT_SUPPORTED, paymentMethod.toString())
    }
}
