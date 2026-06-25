package com.leopassos.payment_connector.entities

import com.leopassos.payment_connector.enums.PaymentTransactionStatus
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

/**
 * Modelo de persistência que representa uma transação de pagamento processada pelo conector.
 *
 * @property id identificador único da transação.
 * @property ecommerceOrderId identificador do pedido relacionado no ecommerce.
 * @property gatewayPaymentId identificador atribuído pelo gateway de pagamento, quando disponível.
 * @property amountInCents valor da transação representado na menor unidade da moeda.
 * @property currency código ISO 4217 da moeda usada no valor da transação.
 * @property status [PaymentTransactionStatus] atual da transação.
 * @property createdAt data e hora em que a transação foi criada.
 * @property updatedAt data e hora da última atualização da transação.
 */
@Entity
@Table(name = "payment_transactions")
class PaymentTransaction(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "ecommerce_order_id", nullable = false)
    var ecommerceOrderId: String,
    @Column(name = "gateway_payment_id")
    var gatewayPaymentId: String? = null,
    @Column(name = "amount_in_cents", nullable = false)
    var amountInCents: Long,
    @Column(nullable = false, length = 3)
    var currency: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PaymentTransactionStatus,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
