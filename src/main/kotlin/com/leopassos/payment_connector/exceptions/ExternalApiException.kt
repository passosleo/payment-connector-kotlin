package com.leopassos.payment_connector.exceptions

/**
 * Exceção usada para representar falhas em APIs externas chamadas pelos clients HTTP.
 *
 * @property integrationName nome lógico do client ou integração que falhou.
 * @property externalStatusCode status HTTP retornado pela API externa, quando disponível.
 * @property responseBody corpo retornado pela API externa, mantido para diagnóstico interno.
 */
class ExternalApiException(
    val integrationName: String,
    val externalStatusCode: Int? = null,
    val responseBody: String? = null,
    cause: Throwable,
) : RuntimeException("External API request failed: $integrationName", cause)
