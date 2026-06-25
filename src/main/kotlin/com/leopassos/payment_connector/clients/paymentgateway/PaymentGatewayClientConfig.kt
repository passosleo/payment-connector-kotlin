package com.leopassos.payment_connector.clients.paymentgateway

import com.leopassos.payment_connector.clients.ClientProperties
import com.leopassos.payment_connector.clients.HttpClientFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuração Spring da integração com o gateway de pagamento.
 *
 * Declara as propriedades da integração e o bean de [PaymentGatewayClient] criado pelo [HttpClientFactory].
 */
@Configuration
class PaymentGatewayClientConfig {

    /**
     * Carrega as propriedades `integrations.payment-gateway`.
     *
     * @return propriedades do client do gateway.
     */
    @Bean
    @ConfigurationProperties(prefix = "integrations.payment-gateway")
    fun paymentGatewayClientProperties(): ClientProperties {
        return ClientProperties()
    }

    /**
     * Cria o client declarativo do gateway de pagamento.
     *
     * @param httpClientFactory factory padronizada para clients externos.
     * @param properties propriedades do client do gateway.
     * @return proxy HTTP de [PaymentGatewayClient].
     */
    @Bean
    fun paymentGatewayClient(
        httpClientFactory: HttpClientFactory,
        @Qualifier("paymentGatewayClientProperties")
        properties: ClientProperties,
    ): PaymentGatewayClient {
        return httpClientFactory.createClient(PaymentGatewayClient::class.java, properties)
    }
}
