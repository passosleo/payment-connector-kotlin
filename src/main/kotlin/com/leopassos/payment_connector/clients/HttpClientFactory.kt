package com.leopassos.payment_connector.clients

import com.leopassos.payment_connector.exceptions.ExternalApiException
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

/**
 * Cria clients HTTP declarativos para integrações externas.
 *
 * Centraliza a criação do [RestClient], aplicação de timeout/retry, logs sanitizados e conversão de falhas externas em
 * [ExternalApiException].
 */
@Component
class HttpClientFactory {

    /**
     * Helper responsável por logs seguros das chamadas externas criadas por esta factory.
     */
    private val externalHttpLogger = ExternalHttpLogger()

    /**
     * Cria um proxy HTTP declarativo a partir de uma interface anotada pelo Spring.
     *
     * @param clientType contrato da integração, anotado com `@HttpExchange` e operações HTTP.
     * @param properties propriedades de conexão da integração.
     * @return proxy que implementa o contrato informado.
     */
    fun <T : Any> createClient(
        clientType: Class<T>,
        properties: ClientProperties,
    ): T {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }

        val integrationName = clientType.simpleName

        val restClient = RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .requestInterceptor(retryInterceptor(properties))
            .requestInterceptor(externalHttpLogger.interceptor(integrationName))
            .build()

        val client = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(clientType)

        return wrapExternalApiErrors(clientType, client)
    }

    /**
     * Envolve o proxy declarativo em um proxy de tratamento de erro.
     *
     * @param clientType contrato da integração.
     * @param client proxy original criado pelo [HttpServiceProxyFactory].
     * @return proxy que converte falhas HTTP externas em [ExternalApiException].
     */
    private fun <T : Any> wrapExternalApiErrors(clientType: Class<T>, client: T): T {
        val integrationName = clientType.simpleName

        val proxy = Proxy.newProxyInstance(
            clientType.classLoader,
            arrayOf(clientType),
        ) { _, method, arguments ->
            try {
                method.invoke(client, *(arguments ?: emptyArray()))
            } catch (exception: InvocationTargetException) {
                throw toExternalApiException(integrationName, exception.targetException)
            } catch (exception: RestClientResponseException) {
                throw toExternalApiException(integrationName, exception)
            } catch (exception: ResourceAccessException) {
                throw toExternalApiException(integrationName, exception)
            }
        }

        return clientType.cast(proxy)
    }

    /**
     * Converte exceções do `RestClient` para exceções padronizadas de integração externa.
     *
     * @param integrationName nome lógico da integração que falhou.
     * @param exception exceção original lançada pelo client HTTP.
     * @return [ExternalApiException] quando a falha é de integração externa, ou a exceção original nos demais casos.
     */
    private fun toExternalApiException(integrationName: String, exception: Throwable): Throwable {
        return when (exception) {
            is ExternalApiException -> exception
            is RestClientResponseException -> {
                externalHttpLogger.httpErrorMapped(integrationName, exception)
                ExternalApiException(
                    integrationName = integrationName,
                    externalStatusCode = exception.statusCode.value(),
                    responseBody = exception.responseBodyAsString,
                    cause = exception,
                )
            }
            is ResourceAccessException -> {
                externalHttpLogger.accessErrorMapped(integrationName, exception)
                ExternalApiException(
                    integrationName = integrationName,
                    cause = exception,
                )
            }
            else -> exception
        }
    }

    /**
     * Cria o interceptor que aplica a política de retry configurada para a integração.
     *
     * @param properties propriedades do client externo.
     * @return interceptor HTTP com retry para falhas transitórias.
     */
    private fun retryInterceptor(properties: ClientProperties): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request, body, execution ->
            executeWithRetry(properties, request, body, execution)
        }
    }

    /**
     * Executa a requisição HTTP repetindo tentativas elegíveis para retry.
     *
     * @param properties propriedades com limite de tentativas e backoff.
     * @param request requisição HTTP externa.
     * @param body corpo serializado pelo `RestClient`.
     * @param execution executor da cadeia de interceptors.
     * @return resposta HTTP externa obtida com sucesso ou não elegível para retry.
     */
    private fun executeWithRetry(
        properties: ClientProperties,
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        var attempt = 1

        while (true) {
            try {
                val response = execution.execute(request, body)
                if (!response.statusCode.is5xxServerError || attempt >= properties.retry.maxAttempts) {
                    return response
                }

                response.close()
                externalHttpLogger.retry(request, attempt, properties)
            } catch (exception: Exception) {
                if (!shouldRetry(exception) || attempt >= properties.retry.maxAttempts) {
                    throw exception
                }

                externalHttpLogger.retry(request, attempt, properties)
            }

            Thread.sleep(properties.retry.backoff.toMillis())
            attempt += 1
        }
    }

    /**
     * Indica se uma exceção representa falha transitória para retry.
     *
     * @param exception exceção capturada durante a chamada externa.
     * @return `true` quando a chamada pode ser tentada novamente.
     */
    private fun shouldRetry(exception: Exception): Boolean {
        return when (exception) {
            is ResourceAccessException -> true
            is RestClientResponseException -> exception.statusCode.is5xxServerError
            else -> false
        }
    }
}
