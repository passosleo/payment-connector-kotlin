package com.leopassos.payment_connector.clients

import com.leopassos.payment_connector.exceptions.ExternalApiException
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpClientFactoryTest {

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun `wraps external api response errors preserving status code and response body`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/failure") { exchange ->
                val responseBody = """{"error":"invalid request"}"""
                exchange.sendResponseHeaders(422, responseBody.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(responseBody.toByteArray()) }
            }
            start()
        }

        val client = HttpClientFactory().createClient(
            TestExternalClient::class.java,
            ClientProperties().apply {
                baseUrl = "http://localhost:${server!!.address.port}"
                connectTimeout = Duration.ofSeconds(1)
                readTimeout = Duration.ofSeconds(1)
                retry.maxAttempts = 1
            },
        )

        val exception = assertFailsWith<ExternalApiException> {
            client.failure()
        }

        assertEquals("TestExternalClient", exception.integrationName)
        assertEquals(422, exception.externalStatusCode)
        assertEquals("""{"error":"invalid request"}""", exception.responseBody)
    }

    @HttpExchange
    private interface TestExternalClient {

        @GetExchange("/failure")
        fun failure(): String
    }
}
