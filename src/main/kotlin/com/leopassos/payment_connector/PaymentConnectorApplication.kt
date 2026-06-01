package com.leopassos.payment_connector

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
    info = Info(
        title = "Payment Connector API",
        version = "v1",
        description = "API para integração de pagamentos"
    )
)
@SpringBootApplication
class PaymentConnectorApplication

fun main(args: Array<String>) {
    runApplication<PaymentConnectorApplication>(*args)
}