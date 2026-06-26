package com.leopassos.payment_connector.clients.ecommerce

import com.leopassos.payment_connector.dtos.ecommerce.response.EcommerceOrderResponseDTO
import com.leopassos.payment_connector.dtos.ecommerce.response.EcommerceProductResponseDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking

/**
 * Contrato da integração com ecommerce.
 *
 * Define apenas as operações externas usadas pela aplicação. Configuração de URL base, timeout, retry, autenticação e
 * logs fica centralizada em [com.leopassos.payment_connector.clients.HttpClientFactory].
 */
interface EcommerceClient {

    /**
     * Busca um pedido no ecommerce.
     *
     * @param orderId identificador externo do pedido.
     * @return pedido retornado pelo ecommerce.
     */
    fun getOrder(orderId: String): EcommerceOrderResponseDTO

    /**
     * Busca um produto no ecommerce.
     *
     * @param productId identificador externo do produto.
     * @return produto retornado pelo ecommerce.
     */
    fun getProduct(productId: String): EcommerceProductResponseDTO
}

/**
 * Implementação Ktor do [EcommerceClient].
 *
 * @property httpClient client HTTP Ktor configurado para a API de ecommerce.
 */
class KtorEcommerceClient(
    private val httpClient: HttpClient,
) : EcommerceClient {

    /**
     * Busca um pedido no ecommerce.
     *
     * @param orderId identificador externo do pedido.
     * @return pedido retornado pelo ecommerce.
     */
    override fun getOrder(orderId: String): EcommerceOrderResponseDTO {
        return runBlocking {
            httpClient.get("/orders/$orderId").body()
        }
    }

    /**
     * Busca um produto no ecommerce.
     *
     * @param productId identificador externo do produto.
     * @return produto retornado pelo ecommerce.
     */
    override fun getProduct(productId: String): EcommerceProductResponseDTO {
        return runBlocking {
            httpClient.get("/products/$productId").body()
        }
    }
}
