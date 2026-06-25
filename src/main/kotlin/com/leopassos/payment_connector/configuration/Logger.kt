package com.leopassos.payment_connector.configuration

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Cria um logger SLF4J associado ao tipo informado.
 *
 * @param T tipo usado como origem do logger.
 * @return logger configurado para o tipo [T].
 */
inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)
