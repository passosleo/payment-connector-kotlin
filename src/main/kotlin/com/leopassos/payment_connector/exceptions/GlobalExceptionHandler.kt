package com.leopassos.payment_connector.exceptions

import com.leopassos.payment_connector.configuration.logger
import com.leopassos.payment_connector.dtos.connector.response.ErrorResponseDTO
import com.leopassos.payment_connector.dtos.connector.response.error
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.NoHandlerFoundException
import java.io.IOException

private val log = logger<GlobalExceptionHandler>()

/**
 * Trata exceções lançadas durante o processamento de requisições REST e as converte em respostas de erro padronizadas.
 *
 * Este advice é aplicado globalmente aos controllers gerenciados pelo Spring MVC.
 *
 * @see ErrorResponseDTO
 * @see Errors
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * Trata exceções de aplicação específicas do domínio.
     *
     * @param exception exceção com os metadados do erro da aplicação.
     * @return entidade de resposta com status e corpo mapeados a partir do erro da aplicação.
     */
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(exception: ApplicationException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = exception.error,
            message = exception.message,
            exception = exception,
        )
    }

    /**
     * Trata respostas HTTP de erro retornadas por APIs externas chamadas via Ktor.
     *
     * @param exception exceção com status retornado pela API externa.
     * @return resposta de bad gateway com status externo nos detalhes.
     */
    @ExceptionHandler(ResponseException::class)
    fun handleKtorResponseException(exception: ResponseException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.EXTERNAL_API_ERROR,
            details = mapOf("externalStatusCode" to exception.response.status.value.toString()),
            exception = exception,
        )
    }

    /**
     * Trata falhas de transporte ao chamar APIs externas via Ktor.
     *
     * @param exception exceção de timeout ou I/O.
     * @return resposta de bad gateway sem expor detalhes sensíveis.
     */
    @ExceptionHandler(HttpRequestTimeoutException::class, IOException::class)
    fun handleKtorTransportException(exception: Exception): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.EXTERNAL_API_ERROR,
            exception = exception,
        )
    }

    /**
     * Trata respostas inválidas ou incompatíveis retornadas por APIs externas chamadas via Ktor.
     *
     * @param exception exceção lançada durante a conversão do corpo da resposta externa.
     * @return resposta de bad gateway sem expor o payload externo.
     */
    @ExceptionHandler(JsonConvertException::class)
    fun handleKtorJsonConvertException(exception: JsonConvertException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.EXTERNAL_API_ERROR,
            details = mapOf("reason" to "invalidExternalResponse"),
            exception = exception,
        )
    }

    /**
     * Trata erros de validação da requisição produzidos por bean validation.
     *
     * Erros de validação de campos e objetos são retornados no mapa de detalhes da resposta.
     *
     * @param exception exceção com erros de validação de campo e globais.
     * @return resposta de requisição inválida com os detalhes dos erros de validação.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponseDTO> {
        val fieldErrors =
            exception.bindingResult.fieldErrors.associate {
                it.field to (it.defaultMessage ?: "Invalid value")
            }

        val globalErrors =
            exception.bindingResult.globalErrors.associate {
                it.objectName to (it.defaultMessage ?: "Invalid value")
            }

        return buildResponse(
            error = Errors.VALIDATION_ERROR,
            details = fieldErrors + globalErrors,
            exception = exception,
        )
    }

    /**
     * Trata corpos de requisição HTTP malformados ou ilegíveis.
     *
     * @param exception exceção lançada quando o corpo da requisição não pode ser interpretado.
     * @return resposta de requisição inválida indicando uma requisição malformada.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(
        exception: HttpMessageNotReadableException
    ): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.MALFORMED_REQUEST,
            exception = exception,
        )
    }

    /**
     * Trata requisições sem um parâmetro obrigatório de query, form ou multipart.
     *
     * @param exception exceção com o nome do parâmetro ausente.
     * @return resposta de requisição inválida indicando qual parâmetro está ausente.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        exception: MissingServletRequestParameterException
    ): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.MISSING_PARAMETER,
            message = Errors.MISSING_PARAMETER.message(exception.parameterName),
            exception = exception,
        )
    }

    /**
     * Trata parâmetros de requisição que não podem ser convertidos para o tipo esperado.
     *
     * @param exception exceção com o nome do parâmetro inválido e o contexto da conversão.
     * @return resposta de requisição inválida indicando o parâmetro inválido.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        exception: MethodArgumentTypeMismatchException
    ): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.INVALID_PARAMETER,
            message = "Parameter '${exception.name}' has an invalid value",
            exception = exception,
        )
    }

    /**
     * Trata erros de argumento inválido lançados durante o processamento de uma requisição.
     *
     * @param exception exceção com a mensagem do argumento inválido.
     * @return resposta de requisição inválida com a mensagem da exceção quando disponível.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.INVALID_ARGUMENT,
            message = exception.message ?: Errors.INVALID_ARGUMENT.message(),
            exception = exception,
        )
    }

    /**
     * Trata requisições que usam um método HTTP não suportado pelo endpoint.
     *
     * @param exception exceção que descreve o método HTTP não suportado.
     * @return resposta de método não permitido.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.METHOD_NOT_ALLOWED,
            message = exception.message,
            exception = exception,
        )
    }

    /**
     * Trata requisições com tipo de conteúdo não suportado.
     *
     * @param exception exceção que descreve o tipo de mídia não suportado.
     * @return resposta de tipo de mídia não suportado.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(exception: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.UNSUPPORTED_MEDIA_TYPE,
            message = exception.message,
            exception = exception,
        )
    }

    /**
     * Trata requisições para tipos de mídia de resposta que não podem ser produzidos.
     *
     * @param exception exceção que descreve o tipo de mídia solicitado e não aceitável.
     * @return resposta de tipo de mídia não aceitável.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleMediaTypeNotAcceptable(exception: HttpMediaTypeNotAcceptableException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.NOT_ACCEPTABLE,
            message = exception.message,
            exception = exception,
        )
    }

    /**
     * Trata requisições que não correspondem a nenhum handler de controller.
     *
     * @param exception exceção com informações sobre o handler não resolvido.
     * @return resposta de recurso não encontrado.
     */
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFound(exception: NoHandlerFoundException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.NOT_FOUND,
            message = exception.message,
            exception = exception,
        )
    }

    /**
     * Trata exceções de status de resposta do Spring.
     *
     * @param exception exceção com o status HTTP e uma razão opcional.
     * @return entidade de resposta mapeada a partir do código de status da exceção.
     */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.fromHttpStatus(exception.statusCode.value()),
            message = exception.reason,
            exception = exception,
        )
    }

    /**
     * Trata qualquer exceção inesperada que não foi capturada por um handler mais específico.
     *
     * @param exception exceção inesperada lançada durante o processamento da requisição.
     * @return resposta de erro interno no servidor.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ErrorResponseDTO> {
        return buildResponse(
            error = Errors.INTERNAL_SERVER_ERROR,
            exception = exception,
        )
    }

    /**
     * Monta uma resposta de erro padronizada para o erro da aplicação informado.
     *
     * @param error metadados do erro usados para definir status HTTP, código e mensagem padrão.
     * @param message mensagem opcional que sobrescreve a mensagem padrão do erro.
     * @param details mapa opcional com detalhes de erro por campo ou por contexto.
     * @param exception exceção original tratada pelo handler.
     * @return entidade de resposta contendo o corpo de erro padronizado.
     */
    private fun buildResponse(
        error: Errors,
        message: String? = null,
        details: Map<String, String>? = null,
        exception: Exception? = null,
    ): ResponseEntity<ErrorResponseDTO> {
        val responseMessage = message ?: error.message()

        logHandledException(error, responseMessage, details, exception)

        return error(
            status = error.status,
            code = error.code,
            message = responseMessage,
            details = details,
        )
    }

    /**
     * Registra exceções tratadas pelo advice com severidade baseada no status HTTP.
     *
     * Erros 5xx são registrados com stack trace para investigação. Erros 4xx são registrados como avisos
     * estruturados, sem stack trace, por representarem falhas esperadas de entrada ou domínio.
     *
     * @param error erro da aplicação usado para status e código estável.
     * @param message mensagem enviada no corpo da resposta.
     * @param details detalhes opcionais da resposta; apenas as chaves são logadas para reduzir ruído.
     * @param exception exceção original tratada pelo advice.
     */
    private fun logHandledException(
        error: Errors,
        message: String,
        details: Map<String, String>?,
        exception: Exception?,
    ) {
        val detailsKeys = details?.keys?.joinToString()

        if (error.status.is5xxServerError) {
            log.error(
                "Handled exception mapped to API error. status={}, code={}, message={}, detailsKeys={}",
                error.status.value(),
                error.code,
                message,
                detailsKeys,
                exception,
            )
            return
        }

        log.warn(
            "Handled request exception. status={}, code={}, message={}, detailsKeys={}, exceptionType={}",
            error.status.value(),
            error.code,
            message,
            detailsKeys,
            exception?.javaClass?.simpleName,
        )
    }
}
