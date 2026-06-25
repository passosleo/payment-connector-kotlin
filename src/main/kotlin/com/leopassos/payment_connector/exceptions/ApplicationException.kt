package com.leopassos.payment_connector.exceptions

/**
 * Exceção de runtime usada para propagar erros mapeados da aplicação.
 *
 * A mensagem da exceção é gerada a partir do template de mensagem de [Errors] e dos [arguments] informados.
 *
 * @property error metadados do erro da aplicação associados a esta exceção.
 * @param arguments valores usados para formatar o template da mensagem de erro.
 */
class ApplicationException(
    val error: Errors,
    vararg arguments: Any,
) : RuntimeException(error.message(*arguments))
