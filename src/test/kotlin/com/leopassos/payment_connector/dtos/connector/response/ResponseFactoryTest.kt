package com.leopassos.payment_connector.dtos.connector.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ResponseFactoryTest {

    @Test
    fun `success returns ok response with data and message`() {
        val response = success(
            data = mapOf("paymentId" to "pay_123"),
            message = "Payment authorized",
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Payment authorized", response.body?.message)
        assertEquals(mapOf("paymentId" to "pay_123"), response.body?.data)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `error returns response with status and error body`() {
        val details = mapOf("amountInCents" to "must be greater than zero")

        val response = error(
            status = HttpStatus.BAD_REQUEST,
            code = "ERR001",
            message = "Request validation failed",
            details = details,
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("ERR001", response.body?.code)
        assertEquals("Request validation failed", response.body?.message)
        assertEquals(details, response.body?.details)
    }
}
