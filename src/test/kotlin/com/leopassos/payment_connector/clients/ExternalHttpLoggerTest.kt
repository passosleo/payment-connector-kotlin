package com.leopassos.payment_connector.clients

import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.test.assertEquals

class ExternalHttpLoggerTest {

    @Test
    fun `sanitizes uri before logging external requests`() {
        val sanitizedUri = ExternalHttpLogger().sanitizeUri(
            URI("https://user:secret@example.com:8443/orders/123?token=abc&email=user@example.com#payment"),
        )

        assertEquals("https://example.com:8443/orders/123", sanitizedUri)
    }
}
