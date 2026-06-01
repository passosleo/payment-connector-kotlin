package com.leopassos.payment_connector.payment

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, UUID> {
    fun findByEcommerceOrderId(ecommerceOrderId: String): PaymentTransaction?

    fun findByGatewayPaymentId(gatewayPaymentId: String): PaymentTransaction?
}
