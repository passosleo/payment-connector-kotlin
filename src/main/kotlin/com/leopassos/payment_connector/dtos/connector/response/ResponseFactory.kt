package com.leopassos.payment_connector.dtos.connector.response

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity

/**
 * Cria uma resposta HTTP 200 usando o envelope padronizado de sucesso da API.
 *
 * @param T tipo do payload retornado pela API.
 * @param data payload da resposta, quando houver conteúdo de retorno.
 * @param message mensagem opcional com contexto sobre a operação.
 * @return resposta HTTP 200 com [SuccessResponseDTO].
 */
fun <T> success(
    data: T? = null,
    message: String? = null,
): ResponseEntity<SuccessResponseDTO<T>> {
    return ResponseEntity.ok(
        SuccessResponseDTO(
            message = message,
            data = data,
        )
    )
}

/**
 * Cria uma resposta de erro usando o envelope padronizado de erro da API.
 *
 * @param status status HTTP da resposta.
 * @param code código de erro estável da aplicação.
 * @param message mensagem de erro legível.
 * @param details detalhes opcionais do erro por campo ou por contexto.
 * @return resposta HTTP com [ErrorResponseDTO].
 */
fun error(
    status: HttpStatusCode,
    code: String,
    message: String,
    details: Map<String, String>? = null,
): ResponseEntity<ErrorResponseDTO> {
    return ResponseEntity.status(status)
        .body(
            ErrorResponseDTO(
                code = code,
                message = message,
                details = details,
            )
        )
}
