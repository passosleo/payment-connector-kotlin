package com.leopassos.payment_connector.clients.ecommerce

import com.leopassos.payment_connector.dtos.ecommerce.response.EcommerceOrderResponseDTO
import com.leopassos.payment_connector.dtos.ecommerce.response.EcommerceProductResponseDTO
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

/**
 * Contrato HTTP declarativo da integração com ecommerce.
 *
 * Expõe somente as operações externas consumidas pela aplicação. Configuração de URL, timeout, retry, logs e tratamento
 * de erro ficam centralizados em [com.leopassos.payment_connector.clients.HttpClientFactory].
 */
@HttpExchange
interface EcommerceClient {

    /**
     * Busca um pedido no ecommerce.
     *
     * @param orderId identificador externo do pedido.
     * @return pedido retornado pelo ecommerce.
     */
    @GetExchange("/orders/{orderId}")
    fun getOrder(
        @PathVariable orderId: String
    ): EcommerceOrderResponseDTO

    /**
     * Busca um produto no ecommerce.
     *
     * @param productId identificador externo do produto.
     * @return produto retornado pelo ecommerce.
     */
    @GetExchange("/products/{productId}")
    fun getProduct(
        @PathVariable productId: String
    ): EcommerceProductResponseDTO
}
