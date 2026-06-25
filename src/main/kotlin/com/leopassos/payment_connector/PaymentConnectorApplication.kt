package com.leopassos.payment_connector

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Configuração principal da aplicação Spring Boot da API Payment Connector.
 */
@OpenAPIDefinition(
    info =
        Info(
            title = "Payment Connector API",
            version = "v1",
            description = "API para integração de pagamentos",
        )
)
@SpringBootApplication
class PaymentConnectorApplication

/**
 * Inicia a aplicação Payment Connector.
 *
 * @param args argumentos de linha de comando repassados ao Spring Boot.
 */
fun main(args: Array<String>) {
    runApplication<PaymentConnectorApplication>(*args)
}
