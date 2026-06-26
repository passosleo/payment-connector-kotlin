package com.leopassos.payment_connector.clients.paymentgateway

import com.leopassos.payment_connector.clients.ClientProperties
import com.leopassos.payment_connector.clients.HttpClientFactory
import com.leopassos.payment_connector.dtos.paymentgateway.request.CreatePaymentIntentRequestDTO
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentGatewayClientTest {

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun `sends payment intent request as form url encoded`() {
        var contentType: String? = null
        var requestBody: String? = null
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/v1/payment_intents") { exchange ->
                contentType = exchange.requestHeaders.getFirst("Content-Type")
                requestBody = String(exchange.requestBody.readAllBytes(), StandardCharsets.UTF_8)

                val responseBody = """{"id":"pi_123","status":"processing","amount":12990,"currency":"brl"}"""
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, responseBody.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(responseBody.toByteArray()) }
            }
            start()
        }

        val httpClient = HttpClientFactory().createClient(
            ClientProperties().apply {
                baseUrl = "http://localhost:${server!!.address.port}"
                bearerToken = "sk_test_unit"
                connectTimeout = Duration.ofSeconds(1)
                readTimeout = Duration.ofSeconds(1)
            },
        )
        val client = KtorPaymentGatewayClient(httpClient)

        val response = client.createPaymentIntent(
            CreatePaymentIntentRequestDTO(
                amount = 12990,
                currency = "brl",
                paymentMethod = "pm_card_visa",
            ),
        )

        assertTrue(contentType!!.startsWith("application/x-www-form-urlencoded"))
        assertTrue(requestBody!!.contains("amount=12990"))
        assertTrue(requestBody!!.contains("currency=brl"))
        assertTrue(requestBody!!.contains("payment_method=pm_card_visa"))
        assertTrue(requestBody!!.contains("confirm=true"))
        assertEquals("pi_123", response.id)
        httpClient.close()
    }

    @Test
    fun `ignores unknown payment intent response fields`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/v1/payment_intents") { exchange ->
                val responseBody = """
                    {
                      "id": "pi_123",
                      "status": "processing",
                      "amount": 12990,
                      "currency": "brl",
                      "amount_capturable": 0
                    }
                """.trimIndent()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, responseBody.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(responseBody.toByteArray()) }
            }
            start()
        }

        val httpClient = HttpClientFactory().createClient(
            ClientProperties().apply {
                baseUrl = "http://localhost:${server!!.address.port}"
                connectTimeout = Duration.ofSeconds(1)
                readTimeout = Duration.ofSeconds(1)
            },
        )
        val client = KtorPaymentGatewayClient(httpClient)

        val response = client.createPaymentIntent(
            CreatePaymentIntentRequestDTO(
                amount = 12990,
                currency = "brl",
                paymentMethod = "pm_card_visa",
            ),
        )

        assertEquals("pi_123", response.id)
        assertEquals("processing", response.status)
        httpClient.close()
    }
}
