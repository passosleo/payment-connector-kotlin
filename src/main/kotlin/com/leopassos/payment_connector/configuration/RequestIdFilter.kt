package com.leopassos.payment_connector.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Adiciona um identificador de correlação ao ciclo de vida de cada requisição HTTP.
 *
 * Um UUID interno é sempre gerado e exposto no MDC com a chave [REQUEST_ID_MDC_KEY]. Quando o cliente envia
 * [REQUEST_ID_HEADER], o valor é preservado apenas como referência externa em [CLIENT_REQUEST_ID_MDC_KEY].
 */
@Component
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = UUID.randomUUID().toString()
        val clientRequestId = request.getHeader(REQUEST_ID_HEADER)
            ?.sanitizeClientRequestId()

        MDC.put(REQUEST_ID_MDC_KEY, requestId)
        clientRequestId?.let { MDC.put(CLIENT_REQUEST_ID_MDC_KEY, it) }
        response.setHeader(REQUEST_ID_HEADER, requestId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY)
            MDC.remove(CLIENT_REQUEST_ID_MDC_KEY)
        }
    }

    private fun String.sanitizeClientRequestId(): String? {
        return trim()
            .takeIf { it.isNotBlank() }
            ?.replace("\r", "")
            ?.replace("\n", "")
            ?.take(CLIENT_REQUEST_ID_MAX_LENGTH)
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val REQUEST_ID_MDC_KEY = "requestId"
        const val CLIENT_REQUEST_ID_MDC_KEY = "clientRequestId"

        private const val CLIENT_REQUEST_ID_MAX_LENGTH = 128
    }
}
