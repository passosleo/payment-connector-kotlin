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
 * @property retry política de retry aplicada pelo [HttpClientFactory].
 */
open class ClientProperties {
    lateinit var baseUrl: String
    var connectTimeout: Duration = Duration.ofSeconds(2)
    var readTimeout: Duration = Duration.ofSeconds(5)
    var retry: RetryPolicy = RetryPolicy()
}

/**
 * Política de retry para chamadas HTTP externas.
 *
 * @property maxAttempts número máximo de tentativas, contando a chamada inicial.
 * @property backoff intervalo fixo entre tentativas.
 */
class RetryPolicy {
    var maxAttempts: Int = 3
    var backoff: Duration = Duration.ofMillis(200)
}
