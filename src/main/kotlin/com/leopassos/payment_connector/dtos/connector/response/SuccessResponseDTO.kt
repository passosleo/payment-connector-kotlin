package com.leopassos.payment_connector.dtos.connector.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * Resposta de sucesso genérica retornada pela API.
 *
 * @param T tipo do payload retornado pela API.
 * @property timestamp timestamp da resposta no formato ISO-8601.
 * @property message mensagem opcional com contexto sobre a operação.
 * @property data payload da resposta, quando houver conteúdo de retorno.
 */
@Schema(description = "Envelope de sucesso genérico retornado pela API")
data class SuccessResponseDTO<T>(
    @field:Schema(
        description = "Timestamp da resposta no formato ISO-8601",
        example = "2026-06-25T16:30:00Z",
    )
    val timestamp: String = Instant.now().toString(),
    @field:Schema(
        description = "Mensagem opcional com contexto sobre a operação",
        nullable = true,
    )
    val message: String? = null,
    @field:Schema(
        description = "Payload da resposta",
        nullable = true,
    )
    val data: T? = null,
)
