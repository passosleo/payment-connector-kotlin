package com.leopassos.payment_connector.clients

import java.time.Duration

/**
 * Propriedades comuns para integrações HTTP externas.
 *
 * Cada integração deve declarar um bean com `@ConfigurationProperties` apontando para seu prefixo em `application.yml`.
 *
 * @property baseUrl URL base da API externa.
 * @property connectTimeout tempo máximo para estabelecer a conexão.
 * @property readTimeout tempo máximo de espera pela resposta.
 * @property bearerToken token usado no header `Authorization: Bearer`, quando a integração exigir autenticação.
 * @property headers headers fixos enviados em todas as chamadas da integração.
 * @property retry política de retry aplicada em falhas transitórias.
 */
open class ClientProperties {
    lateinit var baseUrl: String
    var connectTimeout: Duration = Duration.ofSeconds(2)
    var readTimeout: Duration = Duration.ofSeconds(5)
    var bearerToken: String? = null
    var headers: Map<String, String> = emptyMap()
    var retry: RetryPolicy = RetryPolicy()
}

/**
 * Política de retry para chamadas HTTP externas.
 *
 * @property maxAttempts número máximo de tentativas, contando a chamada inicial.
 * @property backoff intervalo fixo entre tentativas.
 */
class RetryPolicy {
    var maxAttempts: Int = 1
    var backoff: Duration = Duration.ofMillis(100)
}
