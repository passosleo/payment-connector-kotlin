package com.leopassos.payment_connector.clients.paymentgateway

import com.leopassos.payment_connector.clients.ClientProperties
import com.leopassos.payment_connector.clients.HttpClientFactory
import io.ktor.client.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuração Spring da integração com o gateway de pagamento.
 *
 * Declara as propriedades da integração e o bean de [PaymentGatewayClient].
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
     * Cria o client HTTP Ktor do gateway de pagamento.
     *
     * @param httpClientFactory factory padronizada para clients externos.
     * @param properties propriedades do client do gateway.
     * @return client HTTP Ktor configurado.
     */
    @Bean(destroyMethod = "close")
    fun paymentGatewayHttpClient(
        httpClientFactory: HttpClientFactory,
        @Qualifier("paymentGatewayClientProperties")
        properties: ClientProperties,
    ): HttpClient {
        return httpClientFactory.createClient(properties)
    }

    /**
     * Cria o client do gateway de pagamento.
     *
     * @param httpClient client HTTP Ktor configurado para o gateway.
     * @return implementação de [PaymentGatewayClient].
     */
    @Bean
    fun paymentGatewayClient(
        @Qualifier("paymentGatewayHttpClient")
        httpClient: HttpClient,
    ): PaymentGatewayClient {
        return KtorPaymentGatewayClient(httpClient)
    }
}
