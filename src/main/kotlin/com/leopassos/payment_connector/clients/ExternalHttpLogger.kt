package com.leopassos.payment_connector.clients

import com.leopassos.payment_connector.configuration.logger
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import java.util.concurrent.TimeUnit

private val log = logger<ExternalHttpLogger>()

/**
 * Registra chamadas HTTP externas com dados seguros para log.
 *
 * Não registra body de request/response. URLs são registradas sem query string, fragmento ou user-info para reduzir
 * risco de vazamento de tokens, credenciais ou identificadores sensíveis.
 */
internal class ExternalHttpLogger {

    /**
     * Cria um interceptor para logar início, resposta e falha de transporte da chamada externa.
     *
     * @param integrationName nome lógico da integração.
     * @return interceptor HTTP com sanitização aplicada aos campos registrados.
     */
    fun interceptor(integrationName: String): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request, body, execution ->
            val startedAt = System.nanoTime()
            logRequestStarted(integrationName, request)

            try {
                val response = execution.execute(request, body)
                logResponse(integrationName, request, response, startedAt)
                response
            } catch (exception: ResourceAccessException) {
                logAccessFailure(integrationName, request, startedAt, exception)
                throw exception
            }
        }
    }

    /**
     * Registra uma tentativa adicional após falha transitória.
     *
     * @param request requisição externa executada.
     * @param attempt tentativa que falhou e motivou o retry.
     * @param properties propriedades usadas para informar limite de tentativas e backoff.
     */
    fun retry(request: HttpRequest, attempt: Int, properties: ClientProperties) {
        log.warn(
            "Retrying external request. method={}, uri={}, attempt={}, maxAttempts={}, backoff={}",
            request.method,
            sanitizeUri(request.uri),
            attempt,
            properties.retry.maxAttempts,
            properties.retry.backoff,
        )
    }

    /**
     * Registra erro HTTP convertido em exceção pelo `RestClient`.
     *
     * @param integrationName nome lógico da integração.
     * @param exception exceção com o status retornado pela API externa.
     */
    fun httpErrorMapped(integrationName: String, exception: RestClientResponseException) {
        log.warn(
            "External API HTTP error mapped. integration={}, externalStatus={}, exceptionType={}",
            integrationName,
            exception.statusCode.value(),
            exception.javaClass.simpleName,
        )
    }

    /**
     * Registra falha em que a API externa não retornou resposta HTTP.
     *
     * @param integrationName nome lógico da integração.
     * @param exception exceção de acesso ao recurso externo.
     */
    fun accessErrorMapped(integrationName: String, exception: ResourceAccessException) {
        log.warn(
            "External API access error mapped. integration={}, exceptionType={}, message={}",
            integrationName,
            exception.javaClass.simpleName,
            exception.message,
        )
    }

    /**
     * Registra o início de uma chamada externa.
     *
     * @param integrationName nome lógico da integração.
     * @param request requisição externa que será executada.
     */
    private fun logRequestStarted(integrationName: String, request: HttpRequest) {
        log.info(
            "External API request started. integration={}, method={}, uri={}",
            integrationName,
            request.method,
            sanitizeUri(request.uri),
        )
    }

    /**
     * Registra a resposta HTTP retornada por uma API externa.
     *
     * @param integrationName nome lógico da integração.
     * @param request requisição externa executada.
     * @param response resposta retornada pela API externa.
     * @param startedAt instante inicial da chamada em nanos.
     */
    private fun logResponse(
        integrationName: String,
        request: HttpRequest,
        response: ClientHttpResponse,
        startedAt: Long,
    ) {
        val message = "External API response received. integration={}, method={}, uri={}, externalStatus={}, durationMs={}"
        val arguments = arrayOf<Any>(
            integrationName,
            request.method,
            sanitizeUri(request.uri),
            response.statusCode.value(),
            elapsedMillis(startedAt),
        )

        if (response.statusCode.isError) {
            log.warn(message, *arguments)
            return
        }

        log.info(message, *arguments)
    }

    /**
     * Registra falhas de transporte antes de uma resposta HTTP ser recebida.
     *
     * @param integrationName nome lógico da integração.
     * @param request requisição externa executada.
     * @param startedAt instante inicial da chamada em nanos.
     * @param exception falha de acesso ao recurso externo.
     */
    private fun logAccessFailure(
        integrationName: String,
        request: HttpRequest,
        startedAt: Long,
        exception: ResourceAccessException,
    ) {
        log.warn(
            "External API request failed before response. integration={}, method={}, uri={}, durationMs={}, exceptionType={}, message={}",
            integrationName,
            request.method,
            sanitizeUri(request.uri),
            elapsedMillis(startedAt),
            exception.javaClass.simpleName,
            exception.message,
        )
    }

    /**
     * Remove partes sensíveis da URL antes de registrar em log.
     *
     * @param uri URI original da chamada externa.
     * @return URI sem user-info, query string e fragmento.
     */
    internal fun sanitizeUri(uri: URI): String {
        return URI(
            uri.scheme,
            null,
            uri.host,
            uri.port,
            uri.path,
            null,
            null,
        ).toString()
    }

    /**
     * Calcula a duração da chamada externa em milissegundos.
     *
     * @param startedAt instante inicial da chamada em nanos.
     * @return duração aproximada em milissegundos.
     */
    private fun elapsedMillis(startedAt: Long): Long {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
    }
}
