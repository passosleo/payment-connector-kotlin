package com.leopassos.payment_connector.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "payment_transactions")
class PaymentTransaction(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "ecommerce_order_id", nullable = false)
    val ecommerceOrderId: String,

    @Column(name = "gateway_payment_id")
    var gatewayPaymentId: String? = null,

    @Column(name = "amount_in_cents", nullable = false)
    val amountInCents: Long,

    @Column(nullable = false, length = 3)
    val currency: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PaymentTransactionStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
