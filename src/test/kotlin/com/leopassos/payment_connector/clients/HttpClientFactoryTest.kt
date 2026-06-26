package com.leopassos.payment_connector.clients

import com.sun.net.httpserver.HttpServer
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
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
    fun `creates ktor client with bearer authentication`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/authorized") { exchange ->
                assertEquals("Bearer sk_test_unit", exchange.requestHeaders.getFirst("Authorization"))

                val responseBody = "authorized"
                exchange.sendResponseHeaders(200, responseBody.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(responseBody.toByteArray()) }
            }
            start()
        }

        val client = HttpClientFactory().createClient(
            ClientProperties().apply {
                baseUrl = "http://localhost:${server!!.address.port}"
                bearerToken = "sk_test_unit"
                connectTimeout = Duration.ofSeconds(1)
                readTimeout = Duration.ofSeconds(1)
            },
        )

        val response = runBlocking {
            client.get("/authorized").body<String>()
        }

        assertEquals("authorized", response)
        client.close()
    }

    @Test
    fun `propagates external api response errors from ktor client`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/failure") { exchange ->
                val responseBody = """{"error":"invalid request"}"""
                exchange.sendResponseHeaders(422, responseBody.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(responseBody.toByteArray()) }
            }
            start()
        }

        val client = HttpClientFactory().createClient(
            ClientProperties().apply {
                baseUrl = "http://localhost:${server!!.address.port}"
                connectTimeout = Duration.ofSeconds(1)
                readTimeout = Duration.ofSeconds(1)
            },
        )

        val exception = assertFailsWith<ClientRequestException> {
            runBlocking {
                client.get("/failure").body<String>()
            }
        }

        assertEquals(422, exception.response.status.value)
        client.close()
    }
}
