package com.leopassos.payment_connector.dtos.connector.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Resposta de erro padronizada retornada pela API.
 *
 * @property code código de erro estável da aplicação.
 * @property message mensagem de erro legível.
 * @property details detalhes opcionais do erro por campo ou por contexto.
 */
@Schema(description = "Envelope de erro padronizado retornado pela API")
data class ErrorResponseDTO(
    @field:Schema(
        description = "Código de erro estável da aplicação",
        example = "ERR001",
    )
    val code: String,
    @field:Schema(
        description = "Mensagem de erro legível",
        example = "Request validation failed",
    )
    val message: String,
    @field:Schema(
        description = "Detalhes opcionais do erro por campo ou contexto",
        example = "{\"errorField\": \"Invalid value\"}",
        nullable = true,
    )
    val details: Map<String, String>? = null,
)
