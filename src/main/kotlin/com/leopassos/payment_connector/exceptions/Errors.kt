package com.leopassos.payment_connector.exceptions

import org.springframework.http.HttpStatus
import java.text.MessageFormat

/**
 * Catálogo de erros da aplicação expostos pela API.
 *
 * @property status status HTTP associado ao erro.
 * @property code código de erro estável retornado em [com.leopassos.payment_connector.dtos.connector.response.ErrorResponseDTO].
 * @property messageTemplate template usado para montar a mensagem de erro legível.
 */
enum class Errors(
    val status: HttpStatus,
    val code: String,
    private val messageTemplate: String,
) {
    /**
     * Falha na validação da requisição.
     */
    VALIDATION_ERROR(
        status = HttpStatus.BAD_REQUEST,
        code = "ERR001",
        messageTemplate = "Request validation failed",
    ),

    /**
     * Corpo da requisição ausente ou sem possibilidade de leitura.
     */
    MALFORMED_REQUEST(
        status = HttpStatus.BAD_REQUEST,
        code = "ERR002",
        messageTemplate = "Request body is missing or malformed",
    ),

    /**
     * Parâmetro obrigatório da requisição não foi informado.
     */
    MISSING_PARAMETER(
        status = HttpStatus.BAD_REQUEST,
        code = "ERR003",
        messageTemplate = "Missing required argument: {0}",
    ),

    /**
     * Valor de parâmetro da requisição é inválido.
     */
    INVALID_PARAMETER(
        status = HttpStatus.BAD_REQUEST,
        code = "ERR004",
        messageTemplate = "Request parameter has an invalid value",
    ),

    /**
     * Argumento da requisição é inválido.
     */
    INVALID_ARGUMENT(
        status = HttpStatus.BAD_REQUEST,
        code = "ERR005",
        messageTemplate = "Invalid argument",
    ),

    /**
     * Requisição é sintaticamente válida, mas não pode ser processada.
     */
    UNPROCESSABLE_ENTITY(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        code = "ERR006",
        messageTemplate = "Unprocessable entity",
    ),

    /**
     * Método HTTP não é suportado pelo endpoint.
     */
    METHOD_NOT_ALLOWED(
        status = HttpStatus.METHOD_NOT_ALLOWED,
        code = "ERR007",
        messageTemplate = "HTTP method is not supported",
    ),

    /**
     * Tipo de conteúdo da requisição não é suportado.
     */
    UNSUPPORTED_MEDIA_TYPE(
        status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        code = "ERR008",
        messageTemplate = "Content type is not supported",
    ),

    /**
     * Tipo de mídia solicitado para a resposta não pode ser produzido.
     */
    NOT_ACCEPTABLE(
        status = HttpStatus.NOT_ACCEPTABLE,
        code = "ERR009",
        messageTemplate = "Requested media type is not acceptable",
    ),

    /**
     * Recurso solicitado não foi encontrado.
     */
    NOT_FOUND(
        status = HttpStatus.NOT_FOUND,
        code = "ERR010",
        messageTemplate = "Resource not found",
    ),

    /**
     * Erro para um método de pagamento que não é suportado pela aplicação.
     */
    PAYMENT_METHOD_NOT_SUPPORTED(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        code = "ERR011",
        messageTemplate = "Payment method {0} is not supported",
    ),

    /**
     * Erro ao chamar uma API externa necessária para processar a requisição.
     */
    EXTERNAL_API_ERROR(
        status = HttpStatus.BAD_GATEWAY,
        code = "ERR012",
        messageTemplate = "External API request failed",
    ),

    /**
     * Erro interno inesperado no servidor.
     */
    INTERNAL_SERVER_ERROR(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        code = "ERR999",
        messageTemplate = "Unexpected internal error",
    );

    /**
     * Formata o template da mensagem de erro usando os argumentos informados.
     *
     * @param arguments valores usados por [MessageFormat] para substituir os placeholders do template.
     * @return mensagem de erro formatada.
     */
    fun message(vararg arguments: Any): String {
        return MessageFormat.format(messageTemplate, *arguments)
    }

    /**
     * Funções utilitárias para resolver [Errors].
     */
    companion object {

        /**
         * Resolve o erro da aplicação que melhor representa um código de status HTTP.
         *
         * @param statusCode código de status HTTP numérico.
         * @return [Errors] correspondente, ou [INTERNAL_SERVER_ERROR] quando não houver mapeamento específico.
         */
        fun fromHttpStatus(statusCode: Int): Errors {
            return when (statusCode) {
                HttpStatus.BAD_REQUEST.value() -> INVALID_ARGUMENT
                HttpStatus.METHOD_NOT_ALLOWED.value() -> METHOD_NOT_ALLOWED
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value() -> UNSUPPORTED_MEDIA_TYPE
                HttpStatus.NOT_ACCEPTABLE.value() -> NOT_ACCEPTABLE
                HttpStatus.NOT_FOUND.value() -> NOT_FOUND
                else -> INTERNAL_SERVER_ERROR
            }
        }
    }
}
