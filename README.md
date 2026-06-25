# Payment Connector

API em Kotlin com Spring Boot criada para estudar o desenvolvimento de um conector de pagamento entre um ecommerce e um
gateway de pagamento.

O objetivo do projeto é exercitar o desenho de uma integração intermediária: receber dados de autorização de pagamento,
resolver o provedor correto por método de pagamento, criar transações, acompanhar status e expor uma API própria para
orquestrar esse fluxo.

---

## Sumário

- [Stack](#stack)
- [Estrutura](#estrutura)
- [Rodando Localmente](#rodando-localmente)
- [Configuração](#configuração)
- [Clients HTTP](#clients-http)
- [OpenAPI/Swagger](#openapiswagger)
- [Respostas Padronizadas](#respostas-padronizadas)
- [Exceptions e Erros](#exceptions-e-erros)
- [Logs e Request ID](#logs-e-request-id)
- [Banco e Migrations](#banco-e-migrations)
- [Testes](#testes)
- [Coverage](#coverage)
- [Lint e Formatação](#lint-e-formatação)
- [Git Hooks](#git-hooks)
- [Comandos Gradle Úteis](#comandos-gradle-úteis)
- [Fluxo Recomendado Antes de Abrir PR](#fluxo-recomendado-antes-de-abrir-pr)

---

## Stack

- Kotlin 2.2
- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- Flyway
- PostgreSQL
- H2 para testes
- Springdoc OpenAPI/Swagger
- Gradle Kotlin DSL
- Spotless + ktlint
- JUnit 5
- JaCoCo
- Docker Compose para dependências locais

---

## Estrutura

```text
src/main/kotlin/com/leopassos/payment_connector
├── clients/         # integrações HTTP externas declarativas
├── configuration/   # filtros, logger e configurações transversais
├── controllers/     # endpoints HTTP
├── dtos/            # contratos de request/response
├── entities/        # entidades JPA
├── enums/           # enums de domínio
├── exceptions/      # catálogo de erros e handler global
├── repositories/    # repositórios Spring Data
└── services/        # regras de aplicação e serviços por método de pagamento
```

Recursos:

```text
src/main/resources/application.yml
src/main/resources/db/migration/
src/main/resources/logback-spring.xml
src/test/resources/application.yml
```

---

## Rodando Localmente

Suba as dependências locais:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Execute a aplicação:

```bash
./gradlew bootRun
```

Por padrão, a API sobe em:

```text
http://localhost:8085
```

O ambiente local inclui:

- PostgreSQL em `localhost:5432`
- ecommerce fake com WireMock em `localhost:8090`
- gateway fake com Stripe Mock em `localhost:12111`

Mais detalhes estão em [integrations/README.md](integrations/README.md).

---

## Configuração

A configuração principal fica em `src/main/resources/application.yml`.

Ela concentra:

- nome da aplicação
- porta HTTP
- datasource PostgreSQL
- pool de conexões HikariCP
- JPA
- Flyway
- OpenAPI/Swagger
- URLs das integrações externas

O pool de conexões usa HikariCP, padrão do Spring Boot para JDBC/JPA:

```yaml
spring:
  datasource:
    hikari:
      pool-name: payment-connector-pool
      maximum-pool-size: 10
      minimum-idle: 2
```

Os testes usam `src/test/resources/application.yml`, com H2 em memória configurado em modo PostgreSQL.

---

## Clients HTTP

O pacote `clients` concentra integrações HTTP externas. O padrão usado no projeto é client declarativo do Spring com
`RestClient` por baixo e interfaces anotadas com `@HttpExchange`, `@GetExchange` e `@PostExchange`.

Organização:

```text
clients/
├── ClientProperties.kt       # propriedades comuns de baseUrl, timeout e retry
├── ExternalHttpLogger.kt     # logs seguros das chamadas externas
├── HttpClientFactory.kt      # cria proxies declarativos com timeout, retry e tradução de erros
├── ecommerce/                # client e configuração da API de ecommerce
└── paymentgateway/           # client e configuração do gateway de pagamento
```

Para criar uma nova integração:

- declare uma interface no subpacote da integração, com os endpoints anotados
- crie uma classe `Config` com um bean `@ConfigurationProperties`
- configure a integração em `application.yml` usando `base-url`, `connect-timeout`, `read-timeout` e `retry`
- crie o bean do client chamando `HttpClientFactory.createClient(...)`

Exemplo:

```kotlin
@HttpExchange
interface EcommerceClient {

    @GetExchange("/orders/{orderId}")
    fun getOrder(@PathVariable orderId: String): EcommerceOrderResponseDTO
}
```

As propriedades seguem o mesmo formato das integrações existentes:

```yaml
integrations:
  ecommerce:
    base-url: http://localhost:8090
    connect-timeout: 2s
    read-timeout: 5s
    retry:
      max-attempts: 3
      backoff: 200ms
```

O `HttpClientFactory` aplica:

- timeout de conexão e leitura por integração
- retry para falhas transitórias de conectividade
- retry para respostas `5xx` enquanto houver tentativas disponíveis
- logs estruturados e sanitizados via `ExternalHttpLogger`
- tradução de falhas do `RestClient` para `ExternalApiException`

Erros `4xx` retornados pela API externa não são retentados. Erros `5xx` são retentados até `retry.max-attempts`.
Quando as tentativas acabam, ou quando a falha não é retentável, a exceção lançada pelo client é convertida em
`ExternalApiException`.

A `ExternalApiException` preserva:

- nome do client/integração
- status HTTP retornado pela API externa, quando existir
- corpo retornado pela API externa para diagnóstico interno

O `GlobalExceptionHandler` converte essa exception para a resposta padronizada `ERR012` com status `502 Bad Gateway`.
O corpo da API externa não é repassado ao consumidor da API; apenas `integration` e `externalStatusCode` entram em
`details`.

Os logs dos clients HTTP não registram corpo de request ou response. URLs são registradas sem query string, fragmento ou
user-info para evitar vazamento de tokens, credenciais e outros dados sensíveis.

---

## OpenAPI/Swagger

O projeto usa `springdoc-openapi`.

Em desenvolvimento, a documentação fica habilitada por configuração:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

URLs:

```text
http://localhost:8085/swagger-ui.html
http://localhost:8085/v3/api-docs
```

Os controllers usam anotações simples:

- `@Tag` para agrupar endpoints
- `@Operation` para summary/description

Os DTOs usam `@Schema` para descrever campos e exemplos básicos.

---

## Respostas Padronizadas

As respostas da API usam envelopes padronizados.

Sucesso:

```kotlin
SuccessResponseDTO<T>(
    timestamp = "...",
    message = "...",
    data = ...
)
```

Use a factory:

```kotlin
success(data = payload)
success(data = payload, message = "Operation completed")
```

Erro:

```kotlin
ErrorResponseDTO(
    code = "ERR001",
    message = "Request validation failed",
    details = mapOf("field" to "reason")
)
```

Use a factory:

```kotlin
error(
    status = HttpStatus.BAD_REQUEST,
    code = "ERR001",
    message = "Request validation failed",
    details = details,
)
```

As factories ficam em:

```text
src/main/kotlin/com/leopassos/payment_connector/dtos/connector/response/ResponseFactory.kt
```

---

## Exceptions e Erros

O catálogo de erros da aplicação fica em `Errors`.

Cada erro define:

- status HTTP
- código estável da aplicação
- template de mensagem

Exemplo:

```kotlin
PAYMENT_METHOD_NOT_SUPPORTED(
    status = HttpStatus.UNPROCESSABLE_ENTITY,
    code = "ERR011",
    messageTemplate = "Payment method {0} is not supported",
)
```

Para lançar um erro de domínio, use `ApplicationException`:

```kotlin
throw ApplicationException(
    Errors.PAYMENT_METHOD_NOT_SUPPORTED,
    paymentMethod.toString(),
)
```

O `GlobalExceptionHandler` converte exceções em `ErrorResponseDTO`.

Ele trata, entre outros:

- `ApplicationException`
- `ExternalApiException`
- erros de validação com `MethodArgumentNotValidException`
- corpo malformado com `HttpMessageNotReadableException`
- parâmetro ausente
- type mismatch
- método HTTP não suportado
- media type não suportado
- erro inesperado

Falhas de APIs externas devem ser propagadas como `ExternalApiException`, geralmente pela `HttpClientFactory`. Essa
exception mantém o status retornado pela integração em `externalStatusCode`, quando disponível, e é respondida pela API
como `ERR012`/`502 Bad Gateway`.

Política de logs do handler:

- erros `4xx` são logados como `WARN`, sem stack trace, porque normalmente representam entrada inválida ou erro esperado
  de domínio
- erros `5xx` são logados como `ERROR`, com stack trace, porque exigem investigação
- `details` é resumido no log apenas pelas chaves para reduzir ruído

---

## Logs e Request ID

O projeto usa SLF4J/Logback.

O helper de logger fica em:

```text
configuration/Logger.kt
```

Padrão usado no projeto:

```kotlin
private val log = logger<MyClass>()
```

O `RequestIdFilter` gera um `X-Request-Id` interno para cada requisição, adiciona o valor no MDC com a chave `requestId`
e devolve o header na resposta.

Se o cliente enviar `X-Request-Id`, esse valor é preservado no MDC como `clientRequestId`, depois de sanitizado.

O MDC é limpo no `finally` para evitar vazamento de contexto entre requisições, já que threads podem ser reutilizadas
pelo servidor.

O padrão de log inclui:

```text
[payment-connector] [requestId=...] [thread] logger : message
```

---

## Banco e Migrations

O projeto usa Flyway.

As migrations ficam em:

```text
src/main/resources/db/migration
```

Convenção:

```text
V<versao>__<descricao>.sql
```

Exemplo de nome:

```text
V2__add_payment_attempts.sql
```

Regras do projeto:

- toda alteração estrutural no banco deve entrar como nova migration
- migrations já aplicadas não devem ser editadas
- índices, constraints e tipos de coluna devem ser definidos explicitamente na migration
- entidades JPA devem refletir o schema criado pelo Flyway

O JPA está configurado com:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Ou seja: o Hibernate valida o schema, mas quem cria e evolui o banco é o Flyway.

---

## Testes

O projeto usa JUnit 5.

Configuração:

```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
}
```

Organização dos testes:

- testes de contexto devem validar se a aplicação sobe com a configuração de teste
- testes unitários devem cobrir serviços, factories, filtros e helpers sem subir Spring quando não for necessário
- testes de filtros devem validar efeitos colaterais de request/response e limpeza de contexto
- testes de factories de resposta devem garantir o contrato dos envelopes padronizados

Rodar testes:

```bash
./gradlew test
```

---

## Coverage

O projeto usa JaCoCo.

Comandos:

```bash
./gradlew jacocoTestReport
```

O `test` finaliza gerando o report:

```text
build/reports/jacoco/test/html/index.html
build/reports/jacoco/test/jacocoTestReport.xml
```

Não há threshold mínimo de coverage configurado. O JaCoCo é usado apenas para gerar relatório.

---

## Lint e Formatação

O projeto usa Spotless com ktlint.

Verificar formatação:

```bash
./gradlew spotlessCheck
```

Aplicar formatação:

```bash
./gradlew spotlessApply
```

O `check` executa as validações de formatação e testes. O relatório de coverage é gerado após a execução dos testes.

---

## Git Hooks

Os hooks versionados ficam em `.githooks`.

O repositório deve estar configurado com:

```bash
git config core.hooksPath .githooks
```

Hooks disponíveis:

- `pre-commit`: roda `./gradlew spotlessApply spotlessCheck` quando há arquivos Kotlin ou Gradle Kotlin DSL staged, e
  adiciona novamente os arquivos formatados ao commit
- `pre-push`: roda `./gradlew check` antes de enviar commits para o remoto

---

## Comandos Gradle Úteis

Rodar a aplicação:

```bash
./gradlew bootRun
```

Compilar:

```bash
./gradlew build
```

Rodar testes:

```bash
./gradlew test
```

Rodar validação completa:

```bash
./gradlew check
```

Gerar relatório de coverage:

```bash
./gradlew jacocoTestReport
```

Verificar lint/formatação:

```bash
./gradlew spotlessCheck
```

Aplicar formatação:

```bash
./gradlew spotlessApply
```
