package com.leopassos.payment_connector.controllers

import com.leopassos.payment_connector.dtos.connector.request.AuthorizationRequestDTO
import com.leopassos.payment_connector.dtos.connector.response.AuthorizationResponseDTO
import com.leopassos.payment_connector.dtos.connector.response.SuccessResponseDTO
import com.leopassos.payment_connector.dtos.connector.response.success
import com.leopassos.payment_connector.exceptions.ApplicationException
import com.leopassos.payment_connector.services.AuthorizationServiceFactory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Expõe endpoints responsáveis pela autorização de pagamentos.
 *
 * @param authorizationServiceFactory factory usada para resolver o serviço de autorização para cada
 * [AuthorizationRequestDTO.paymentMethod].
 */
@Tag(name = "Payments", description = "Endpoints de autorização de pagamentos")
@RestController
@RequestMapping("/payment")
class AuthorizationController(
    private val authorizationServiceFactory: AuthorizationServiceFactory
) {

    /**
     * Autoriza uma requisição de pagamento.
     *
     * @param request corpo da requisição com os dados de autorização de pagamento.
     * @return resposta HTTP 200 com a [AuthorizationResponseDTO] retornada pelo serviço selecionado.
     * @throws ApplicationException quando nenhum serviço de autorização suporta [AuthorizationRequestDTO.paymentMethod].
     */
    @Operation(
        summary = "Autoriza um pagamento",
        description = "Resolve o provedor pelo método de pagamento informado e executa a autorização.",
    )
    @PostMapping("/authorize")
    fun authorize(
        @Valid @RequestBody request: AuthorizationRequestDTO
    ): ResponseEntity<SuccessResponseDTO<AuthorizationResponseDTO>> {
        val authorizationService = authorizationServiceFactory.create(request.paymentMethod)
        return success(data = authorizationService.authorize(request))
    }
}
