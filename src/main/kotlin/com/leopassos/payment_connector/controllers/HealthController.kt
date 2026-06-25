package com.leopassos.payment_connector.controllers

import com.leopassos.payment_connector.dtos.connector.response.SuccessResponseDTO
import com.leopassos.payment_connector.dtos.connector.response.success
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Expõe o endpoint de verificação de saúde da aplicação.
 */
@Tag(name = "Health", description = "Endpoint de verificação de saúde da aplicação")
@RestController
@RequestMapping("/health")
class HealthController {

    /**
     * Retorna uma resposta simples de sucesso quando a aplicação está em execução.
     *
     * @return resposta HTTP 200 com a mensagem de saúde da aplicação.
     */
    @Operation(
        summary = "Verifica a saúde da aplicação",
        description = "Retorna sucesso quando a aplicação está em execução.",
    )
    @GetMapping
    fun health(): ResponseEntity<SuccessResponseDTO<String>> {
        return success(data = "Payment Connector is healthy")
    }
}
