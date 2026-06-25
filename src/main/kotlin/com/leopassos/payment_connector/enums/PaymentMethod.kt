package com.leopassos.payment_connector.enums

/**
 * Métodos de pagamento suportados pelo conector.
 */
enum class PaymentMethod {
    /**
     * Método de pagamento por cartão de crédito.
     */
    CREDIT_CARD,

    /**
     * Método de pagamento por boleto.
     */
    BOLETO,

    /**
     * Método de pagamento instantâneo Pix.
     */
    PIX,
}
