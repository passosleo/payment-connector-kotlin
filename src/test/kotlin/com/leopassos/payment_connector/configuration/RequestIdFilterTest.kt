package com.leopassos.payment_connector.configuration

import jakarta.servlet.ServletException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class RequestIdFilterTest {

    private val filter = RequestIdFilter()

    @Test
    fun `adds generated request id to response header and MDC during request`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var requestIdFromMdc: String? = null

        filter.doFilter(
            request,
            response,
            object : MockFilterChain() {
                override fun doFilter(
                    request: jakarta.servlet.ServletRequest,
                    response: jakarta.servlet.ServletResponse,
                ) {
                    requestIdFromMdc = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)
                }
            },
        )

        assertNotNull(requestIdFromMdc)
        assertEquals(requestIdFromMdc, response.getHeader(RequestIdFilter.REQUEST_ID_HEADER))
        assertDoesNotThrowUuidParse(requestIdFromMdc)
        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY))
    }

    @Test
    fun `sanitizes client request id and removes it from MDC after request`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var clientRequestIdFromMdc: String? = null

        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, " external-id\r\n ")

        filter.doFilter(
            request,
            response,
            object : MockFilterChain() {
                override fun doFilter(
                    request: jakarta.servlet.ServletRequest,
                    response: jakarta.servlet.ServletResponse,
                ) {
                    clientRequestIdFromMdc = MDC.get(RequestIdFilter.CLIENT_REQUEST_ID_MDC_KEY)
                }
            },
        )

        assertEquals("external-id", clientRequestIdFromMdc)
        assertNull(MDC.get(RequestIdFilter.CLIENT_REQUEST_ID_MDC_KEY))
    }

    @Test
    fun `clears MDC when request processing fails`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        assertThrows<ServletException> {
            filter.doFilter(
                request,
                response,
                object : MockFilterChain() {
                    override fun doFilter(
                        request: jakarta.servlet.ServletRequest,
                        response: jakarta.servlet.ServletResponse,
                    ) {
                        throw ServletException("Downstream failure")
                    }
                },
            )
        }

        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY))
        assertNull(MDC.get(RequestIdFilter.CLIENT_REQUEST_ID_MDC_KEY))
    }

    private fun assertDoesNotThrowUuidParse(value: String?) {
        assertNotNull(value)

        val parsed = UUID.fromString(value)

        assertTrue(parsed.toString().isNotBlank())
        assertFalse(parsed.toString().contains(" "))
    }
}
