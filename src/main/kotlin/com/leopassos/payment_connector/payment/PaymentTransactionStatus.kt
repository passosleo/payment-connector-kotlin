package com.leopassos.payment_connector.payment

enum class PaymentTransactionStatus {
    PENDING,
    AUTHORIZED,
    PAID,
    FAILED,
    CANCELED,
    REFUNDED,
}
