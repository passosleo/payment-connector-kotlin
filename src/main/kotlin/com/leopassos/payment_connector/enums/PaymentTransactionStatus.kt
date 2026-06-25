package com.leopassos.payment_connector.enums

/**
 * Status do ciclo de vida de uma transação de pagamento.
 */
enum class PaymentTransactionStatus {
    /**
     * Transação criada e aguardando processamento.
     */
    PENDING,

    /**
     * Transação autorizada pelo provedor de pagamento.
     */
    AUTHORIZED,

    /**
     * Transação paga com sucesso.
     */
    PAID,

    /**
     * Transação falhou durante o processamento.
     */
    FAILED,

    /**
     * Transação cancelada.
     */
    CANCELED,

    /**
     * Transação estornada.
     */
    REFUNDED,
}
