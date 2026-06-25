package com.leopassos.payment_connector.repositories

import com.leopassos.payment_connector.entities.PaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

/**
 * Repositório para consultar e persistir registros de [PaymentTransaction].
 */
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, UUID> {

    /**
     * Busca uma transação de pagamento pelo identificador do pedido no ecommerce.
     *
     * @param ecommerceOrderId identificador do pedido no ecommerce.
     * @return [PaymentTransaction] correspondente, ou `null` quando nenhuma transação existir.
     */
    fun findByEcommerceOrderId(ecommerceOrderId: String): PaymentTransaction?

    /**
     * Busca uma transação de pagamento pelo identificador do gateway de pagamento.
     *
     * @param gatewayPaymentId identificador atribuído pelo gateway de pagamento.
     * @return [PaymentTransaction] correspondente, ou `null` quando nenhuma transação existir.
     */
    fun findByGatewayPaymentId(gatewayPaymentId: String): PaymentTransaction?
}
