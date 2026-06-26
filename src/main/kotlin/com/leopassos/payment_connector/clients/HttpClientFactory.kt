package com.leopassos.payment_connector.clients

import com.leopassos.payment_connector.configuration.logger
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestRetryEvent
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.Url
import io.ktor.serialization.jackson.jackson
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = logger<HttpClientFactory>()

/**
 * Cria clients HTTP Ktor configurados para integrações externas.
 *
 * Centraliza URL base, timeouts, headers, autenticação e retry sem criar abstrações próprias para transporte HTTP.
 */
@Component
class HttpClientFactory {

    /**
     * Cria um [HttpClient] configurado para uma integração externa.
     *
     * @param properties propriedades de conexão da integração.
     * @param authenticator autenticador executado antes de cada chamada para resolver tokens fixos ou dinâmicos.
     * @return client HTTP configurado.
     */
    fun createClient(
        properties: ClientProperties,
        authenticator: ClientAuthenticator = ClientAuthenticator.bearer { properties.bearerToken },
    ): HttpClient {
        val maxRetries = (properties.retry.maxAttempts - 1).coerceAtLeast(0)

        return HttpClient(CIO) {
            expectSuccess = true

            install(ContentNegotiation) {
                jackson {
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                }
            }

            install(HttpTimeout) {
                connectTimeoutMillis = properties.connectTimeout.toMillis()
                socketTimeoutMillis = properties.readTimeout.toMillis()
                requestTimeoutMillis = properties.readTimeout.toMillis()
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = maxRetries)
                retryOnException(maxRetries = maxRetries, retryOnTimeout = true)
                delayMillis { properties.retry.backoff.toMillis() }
            }

            defaultRequest {
                url(properties.baseUrl)
                properties.headers.forEach { (name, value) -> header(name, value) }
                authenticator.authenticate(this)
            }
        }.also { client ->
            client.configureExternalHttpLogging()
        }
    }

    /**
     * Registra interceptadores de observabilidade no client Ktor.
     *
     * Os logs registram método, URL sanitizada, status, duração e eventos de retry. Corpo, headers, query string e
     * fragmento da URL não são logados para evitar exposição de dados sensíveis.
     */
    private fun HttpClient.configureExternalHttpLogging() {
        plugin(HttpSend).intercept { request ->
            logExternalRequest(request)

            val startedAt = System.nanoTime()

            try {
                execute(request).also { call ->
                    logExternalResponse(call, startedAt)
                }
            } catch (exception: Throwable) {
                logExternalFailure(request, startedAt, exception)
                throw exception
            }
        }

        monitor.subscribe(HttpRequestRetryEvent) { event ->
            val reason = event.response?.let { "status=${it.status.value}" }
                ?: event.cause?.let { "exception=${it::class.simpleName}" }
                ?: "unknown"

            log.warn(
                "External HTTP retry scheduled. method={}, url={}, retryCount={}, reason={}",
                event.request.method.value,
                safeUrl(event.request),
                event.retryCount,
                reason,
            )
        }
    }

    /**
     * Registra o início de uma chamada HTTP externa.
     *
     * @param request request que será enviado para a API externa.
     */
    private fun logExternalRequest(request: HttpRequestBuilder) {
        log.info(
            "External HTTP request started. method={}, url={}",
            request.method.value,
            safeUrl(request),
        )
    }

    /**
     * Registra a resposta recebida de uma chamada HTTP externa.
     *
     * @param call chamada Ktor concluída com resposta HTTP.
     * @param startedAt instante de início da chamada em nanos, obtido por [System.nanoTime].
     */
    private fun logExternalResponse(
        call: HttpClientCall,
        startedAt: Long,
    ) {
        log.info(
            "External HTTP response received. method={}, url={}, status={}, durationMs={}",
            call.request.method.value,
            safeUrl(call.request.url),
            call.response.status.value,
            elapsedMillis(startedAt),
        )
    }

    /**
     * Registra falhas de transporte ou processamento ocorridas durante uma chamada HTTP externa.
     *
     * @param request request que falhou.
     * @param startedAt instante de início da chamada em nanos, obtido por [System.nanoTime].
     * @param exception exceção lançada pelo Ktor ou pelo engine HTTP.
     */
    private fun logExternalFailure(
        request: HttpRequestBuilder,
        startedAt: Long,
        exception: Throwable,
    ) {
        log.warn(
            "External HTTP request failed. method={}, url={}, durationMs={}, exception={}",
            request.method.value,
            safeUrl(request),
            elapsedMillis(startedAt),
            exception::class.simpleName,
        )
    }

    /**
     * Monta a URL segura de um request ainda em construção.
     *
     * @param request request usado para extrair a URL.
     * @return URL sem query string, fragmento, usuário ou senha.
     */
    private fun safeUrl(request: HttpRequestBuilder): String {
        return safeUrl(request.url.build())
    }

    /**
     * Monta uma URL segura para logs.
     *
     * A URL resultante contém apenas protocolo, host, porta não padrão e path. Query string, fragmento e credenciais são
     * descartados porque podem conter tokens, chaves ou dados de pagamento.
     *
     * @param url URL original retornada pelo Ktor.
     * @return URL sanitizada para log.
     */
    private fun safeUrl(url: Url): String {
        val port = url.specifiedPort
            .takeIf { it != DEFAULT_PORT && it != url.protocol.defaultPort }
            ?.let { ":$it" }
            .orEmpty()

        return "${url.protocol.name}://${url.host}$port${url.encodedPath}"
    }

    /**
     * Calcula a duração de uma chamada em milissegundos.
     *
     * @param startedAt instante de início em nanos, obtido por [System.nanoTime].
     * @return duração aproximada em milissegundos.
     */
    private fun elapsedMillis(startedAt: Long): Long {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
    }
}
