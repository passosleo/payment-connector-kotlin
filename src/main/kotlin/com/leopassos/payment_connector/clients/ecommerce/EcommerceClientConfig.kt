package com.leopassos.payment_connector.clients.ecommerce

import com.leopassos.payment_connector.clients.ClientProperties
import com.leopassos.payment_connector.clients.HttpClientFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuração Spring da integração com ecommerce.
 *
 * Declara as propriedades da integração e o bean de [EcommerceClient] criado pelo [HttpClientFactory].
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
     * Cria o client declarativo de ecommerce.
     *
     * @param httpClientFactory factory padronizada para clients externos.
     * @param properties propriedades do client de ecommerce.
     * @return proxy HTTP de [EcommerceClient].
     */
    @Bean
    fun ecommerceClient(
        httpClientFactory: HttpClientFactory,
        @Qualifier("ecommerceClientProperties")
        properties: ClientProperties,
    ): EcommerceClient {
        return httpClientFactory.createClient(EcommerceClient::class.java, properties)
    }
}
