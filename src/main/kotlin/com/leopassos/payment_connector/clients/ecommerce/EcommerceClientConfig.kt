package com.leopassos.payment_connector.clients.ecommerce

import com.leopassos.payment_connector.clients.ClientProperties
import com.leopassos.payment_connector.clients.HttpClientFactory
import io.ktor.client.HttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuração Spring da integração com ecommerce.
 *
 * Declara as propriedades da integração e o bean de [EcommerceClient].
 */
@Configuration
class EcommerceClientConfig {

    /**
     * Carrega as propriedades `integrations.ecommerce`.
     *
     * @return propriedades do client de ecommerce.
     */
    @Bean
    @ConfigurationProperties(prefix = "integrations.ecommerce")
    fun ecommerceClientProperties(): ClientProperties {
        return ClientProperties()
    }

    /**
     * Cria o client HTTP Ktor de ecommerce.
     *
     * @param httpClientFactory factory padronizada para clients externos.
     * @param properties propriedades do client de ecommerce.
     * @return client HTTP Ktor configurado.
     */
    @Bean(destroyMethod = "close")
    fun ecommerceHttpClient(
        httpClientFactory: HttpClientFactory,
        @Qualifier("ecommerceClientProperties")
        properties: ClientProperties,
    ): HttpClient {
        return httpClientFactory.createClient(properties)
    }

    /**
     * Cria o client de ecommerce.
     *
     * @param httpClient client HTTP Ktor configurado para ecommerce.
     * @return implementação de [EcommerceClient].
     */
    @Bean
    fun ecommerceClient(
        @Qualifier("ecommerceHttpClient")
        httpClient: HttpClient,
    ): EcommerceClient {
        return KtorEcommerceClient(httpClient)
    }
}
