# Regras de negócio do payment connector

Este documento descreve uma base conceitual para estudar e implementar um conector de pagamento em Kotlin/Spring Boot. A ideia não é criar um conector real homologável em uma plataforma específica, mas exercitar os conceitos comuns encontrados em integrações de pagamento para ecommerce, como criação de sessão de pagamento, autorização, captura, cancelamento, estorno, callbacks, idempotência e conciliação de status.

O projeto simula três partes:

- Ecommerce: cria pedidos e inicia o pagamento.
- Payment connector: middleware que orquestra o fluxo.
- Payment gateway: API de pagamento fake que recebe a intenção de cobrança.

## Contexto local

Serviços esperados no ambiente de desenvolvimento:

- Ecommerce fake: `http://localhost:8090`
- Gateway fake Stripe mock: `http://localhost:12111`
- Payment connector: `http://localhost:8085`
- PostgreSQL: `localhost:5432/payment_connector`

Rotas fake já disponíveis no ecommerce:

- `GET /products`
- `GET /products/{productId}`
- `POST /orders`
- `GET /orders/{orderId}`
- `POST /checkout/sessions`

Rotas úteis no gateway fake:

- `POST /v1/payment_intents`
- `GET /v1/payment_intents/{id}`
- `POST /v1/payment_intents/{id}/confirm`
- `POST /v1/refunds`
- `GET /v1/refunds/{id}`

## Ideia central

Um conector de pagamento fica entre o ecommerce e o gateway. Ele recebe uma solicitação de pagamento da plataforma, transforma essa solicitação no formato esperado pelo gateway, persiste o estado local da transação e devolve para o ecommerce uma resposta simples: pagamento criado, pendente, autorizado, pago, negado, cancelado ou estornado.

Mesmo em um projeto de estudo, é importante tratar o conector como uma aplicação de orquestração. Ele não deve depender apenas da resposta imediata do gateway, porque pagamentos podem envolver redirecionamento do comprador, confirmação posterior, callbacks, retentativas, timeout e reconciliação.

## Responsabilidades do conector

O conector deve:

- Expor um manifest com capacidades básicas do provedor.
- Receber uma solicitação de pagamento do ecommerce.
- Validar pedido, valor, moeda e método de pagamento.
- Criar uma transação local.
- Criar uma intenção de pagamento no gateway fake.
- Guardar o identificador retornado pelo gateway.
- Permitir consulta de status pelo ecommerce.
- Permitir captura, cancelamento e estorno.
- Receber callbacks ou eventos de confirmação.
- Evitar cobranças duplicadas por idempotência.
- Registrar transições de status de forma previsível.

O conector não deve:

- Controlar estoque.
- Calcular carrinho.
- Validar cupom, frete ou regra comercial do ecommerce.
- Armazenar dados sensíveis de cartão.
- Criar mais de uma cobrança ativa para o mesmo pedido sem regra explícita.

## Modelo local atual

Entidade principal: `PaymentTransaction`.

Campos atuais:

- `id`: UUID interno da transação no conector.
- `ecommerceOrderId`: identificador do pedido ou sessão no ecommerce.
- `gatewayPaymentId`: identificador da intenção/pagamento no gateway.
- `amountInCents`: valor em centavos.
- `currency`: moeda ISO de 3 letras, por exemplo `BRL`.
- `status`: status local da transação.
- `createdAt`: data de criação local.
- `updatedAt`: data da última alteração local.

Status atuais:

- `PENDING`: transação criada, aguardando confirmação/autorização.
- `AUTHORIZED`: valor autorizado, mas ainda não capturado.
- `PAID`: pagamento confirmado/capturado.
- `FAILED`: pagamento negado ou falha final.
- `CANCELED`: pagamento cancelado antes de ser pago.
- `REFUNDED`: pagamento pago e depois estornado.

## Máquina de estados sugerida

```text
PENDING -> AUTHORIZED
PENDING -> PAID
PENDING -> FAILED
PENDING -> CANCELED

AUTHORIZED -> PAID
AUTHORIZED -> CANCELED
AUTHORIZED -> FAILED

PAID -> REFUNDED

FAILED -> PENDING
```

Regras:

- `FAILED -> PENDING` deve acontecer apenas em retry explícito.
- `PAID -> CANCELED` deve ser bloqueado; depois de pago, use estorno.
- `REFUNDED -> PAID` deve ser bloqueado no escopo simples.
- `CANCELED -> PAID` deve ser bloqueado; crie uma nova tentativa.
- `FAILED -> PAID` direto deve ser bloqueado; confirme ou sincronize com o gateway antes.

## Rotas públicas sugeridas

As rotas abaixo não precisam seguir exatamente nenhum ecommerce real. Elas representam conceitos comuns em conectores de pagamento.

```http
GET  /provider/manifest
POST /payments
GET  /payments/{paymentId}
GET  /payments/by-order/{ecommerceOrderId}
POST /payments/{paymentId}/authorize
POST /payments/{paymentId}/capture
POST /payments/{paymentId}/cancel
POST /payments/{paymentId}/refund
POST /payments/{paymentId}/sync
POST /callbacks/payment-events
```

Rotas internas opcionais para estudo:

```http
GET  /internal/payments
POST /internal/payments/{paymentId}/retry
POST /internal/simulations/gateway-event
```

## `GET /provider/manifest`

Objetivo:

- Permitir que o ecommerce descubra o que o conector suporta.
- Simular a ideia de manifest/capabilities comum em conectores e apps de pagamento.

Resposta sugerida:

```json
{
  "provider": "study-payment-connector",
  "version": "1.0.0",
  "supported_currencies": ["BRL"],
  "supported_payment_methods": ["card", "pix"],
  "capabilities": {
    "authorize": true,
    "capture": true,
    "cancel": true,
    "refund": true,
    "redirect_checkout": true,
    "webhooks": true,
    "partial_capture": false,
    "partial_refund": false
  }
}
```

Regras:

- Não chamar o gateway nessa rota.
- Responder rápido.
- Usar para documentar o escopo atual do conector.

## `POST /payments`

Objetivo:

- Criar uma transação de pagamento a partir de um pedido do ecommerce.

Request sugerido:

```json
{
  "ecommerce_order_id": "ord_123",
  "amount_in_cents": 32980,
  "currency": "BRL",
  "payment_method": "card",
  "capture_mode": "automatic",
  "success_url": "http://localhost:8090/checkout/success",
  "cancel_url": "http://localhost:8090/checkout/cancel",
  "idempotency_key": "checkout-ord_123-attempt-1"
}
```

Regras:

- `ecommerce_order_id` é obrigatório.
- `amount_in_cents` deve ser maior que zero.
- `currency` deve ter 3 letras. Para o ambiente local, aceite `BRL`.
- `payment_method` deve estar entre os métodos suportados no manifest.
- `capture_mode` pode ser `automatic` ou `manual`.
- `idempotency_key` deve evitar duplicidade em chamadas repetidas.
- O conector deve verificar se já existe transação ativa para o mesmo pedido.
- Criar a transação local como `PENDING` antes de chamar o gateway.
- Chamar o gateway para criar um `payment_intent`.
- Salvar `gatewayPaymentId`.
- Se `capture_mode = automatic`, o conector pode confirmar o pagamento e mover para `PAID`.
- Se `capture_mode = manual`, o conector deve manter `AUTHORIZED` ou `PENDING` até uma captura.

Response sugerido:

```json
{
  "id": "uuid-da-transação",
  "ecommerce_order_id": "ord_123",
  "gateway_payment_id": "pi_123",
  "amount_in_cents": 32980,
  "currency": "BRL",
  "payment_method": "card",
  "status": "PENDING",
  "next_action": {
    "type": "redirect",
    "url": "http://localhost:8085/payments/uuid-da-transacao/checkout"
  }
}
```

## `GET /payments/{paymentId}`

Objetivo:

- Permitir que o ecommerce consulte a visão local da transação.

Regras:

- Retornar a transação local sem chamar o gateway a cada request.
- Usar `/payments/{paymentId}/sync` quando for necessário reconciliar com o gateway.

Response sugerido:

```json
{
  "id": "uuid-da-transação",
  "ecommerce_order_id": "ord_123",
  "gateway_payment_id": "pi_123",
  "amount_in_cents": 32980,
  "currency": "BRL",
  "status": "PAID",
  "created_at": "2026-06-11T16:00:00-03:00",
  "updated_at": "2026-06-11T16:01:00-03:00"
}
```

## `GET /payments/by-order/{ecommerceOrderId}`

Objetivo:

- Permitir consulta de pagamento pelo pedido do ecommerce.

Regras:

- Deve retornar a transação associada ao pedido.
- Se houver múltiplas tentativas no futuro, retornar a tentativa ativa ou a lista de tentativas.
- No modelo atual, prefira uma transação por pedido.

## `POST /payments/{paymentId}/authorize`

Objetivo:

- Simular a etapa de autorização do pagamento.
- Em alguns fluxos, autorizar significa reservar o valor sem capturar imediatamente.

Regras:

- Permitir autorização apenas para `PENDING`.
- Chamar o gateway quando fizer sentido para o método de pagamento.
- Atualizar para `AUTHORIZED` quando a autorização for aceita.
- Atualizar para `FAILED` quando o gateway negar.
- Se o gateway fake não diferenciar autorização de captura, simular a autorização localmente ou tratar confirmação bem-sucedida como `AUTHORIZED`.

## `POST /payments/{paymentId}/capture`

Objetivo:

- Capturar/liquidar um pagamento previamente autorizado.

Request sugerido:

```json
{
  "amount_in_cents": 32980,
  "idempotency_key": "capture-ord_123-1"
}
```

Regras:

- Permitir captura para `AUTHORIZED`.
- Em um fluxo simplificado, também pode permitir captura para `PENDING` se o gateway fake não tiver etapa separada de autorização.
- No primeiro escopo, implemente apenas captura total.
- Chamar `POST /v1/payment_intents/{id}/confirm`.
- Atualizar para `PAID` em sucesso.
- Manter status atual e registrar erro se houver timeout.

## `POST /payments/{paymentId}/cancel`

Objetivo:

- Cancelar uma transação que ainda não foi paga.

Request sugerido:

```json
{
  "reason": "customer_abandoned_checkout",
  "idempotency_key": "cancel-ord_123-1"
}
```

Regras:

- Permitir cancelamento para `PENDING` e `AUTHORIZED`.
- Bloquear cancelamento para `PAID`; nesse caso use refund.
- Se a transação ainda não tiver `gatewayPaymentId`, cancelar apenas localmente.
- Se o gateway fake tiver endpoint de cancelamento, chamar gateway; se não tiver, simular localmente.
- Atualizar para `CANCELED` em sucesso.

## `POST /payments/{paymentId}/refund`

Objetivo:

- Estornar um pagamento já pago.

Request sugerido:

```json
{
  "amount_in_cents": 32980,
  "reason": "customer_requested",
  "idempotency_key": "refund-ord_123-1"
}
```

Regras:

- Permitir refund apenas para `PAID`.
- No primeiro escopo, implemente apenas estorno total.
- Chamar `POST /v1/refunds` no gateway.
- Atualizar para `REFUNDED` quando o gateway retornar sucesso.
- Estorno parcial exige evolução do modelo, por exemplo `refundedAmountInCents` ou tabela `payment_refunds`.

## `POST /payments/{paymentId}/sync`

Objetivo:

- Reconciliar a transação local com o estado atual no gateway.

Regras:

- Buscar a transação local.
- Consultar `GET /v1/payment_intents/{gatewayPaymentId}`.
- Traduzir o status externo para `PaymentTransactionStatus`.
- Atualizar `updatedAt`.
- Nunca regredir status final local sem regra explícita.
- Usar para corrigir casos de timeout, erro de rede ou callback perdido.

## `POST /callbacks/payment-events`

Objetivo:

- Simular webhooks/callbacks recebidos de um gateway ou de um checkout externo.

Request sugerido:

```json
{
  "event_id": "evt_123",
  "type": "payment.succeeded",
  "gateway_payment_id": "pi_123",
  "occurred_at": "2026-06-11T16:01:00-03:00"
}
```

Eventos sugeridos:

- `payment.authorized` -> `AUTHORIZED`
- `payment.succeeded` -> `PAID`
- `payment.failed` -> `FAILED`
- `payment.canceled` -> `CANCELED`
- `payment.refunded` -> `REFUNDED`

Regras:

- Eventos devem ser idempotentes por `event_id`.
- Buscar transação por `gatewayPaymentId`.
- Aplicar apenas transições permitidas.
- Responder sucesso se o evento já tiver sido processado.
- Registrar evento desconhecido sem quebrar a aplicação.

## Idempotência

Idempotência é essencial em conectores de pagamento porque ecommerce e gateway podem repetir chamadas em caso de timeout ou erro temporário.

Regras:

- `POST /payments` deve usar `idempotency_key` ou `ecommerce_order_id` para evitar duplicidade.
- Operações como capture, cancel e refund também devem aceitar `idempotency_key`.
- Uma chamada repetida com a mesma chave deve retornar o mesmo resultado lógico.
- Se a mesma chave vier com valor, moeda ou pedido diferente, responder `409 Conflict`.
- Não criar novo `payment_intent` no gateway sem verificar a transação local existente.

No modelo atual, a idempotência pode começar simples usando `ecommerceOrderId`. Em uma evolução, crie uma tabela `payment_operations`.

## Tratamento de erros

Erros esperados:

- Pedido inexistente.
- Valor inválido.
- Moeda não suportada.
- Método de pagamento não suportado.
- Transação não encontrada.
- Status atual não permite a operação.
- Gateway indisponível.
- Timeout no gateway.
- Resposta inesperada do gateway.

Padrão de erro sugerido:

```json
{
  "code": "PAYMENT_STATUS_NOT_ALLOWED",
  "message": "Pagamento não pode ser capturado a partir do status REFUNDED",
  "details": {
    "payment_id": "uuid-da-transação"
  }
}
```

Mapeamento HTTP sugerido:

- `400 Bad Request`: request malformado ou campos inválidos.
- `404 Not Found`: pedido/transação não encontrado.
- `409 Conflict`: duplicidade inconsistente ou transição inválida.
- `422 Unprocessable Entity`: pagamento não pode ser processado com os dados recebidos.
- `502 Bad Gateway`: gateway/ecommerce retornou erro inesperado.
- `504 Gateway Timeout`: timeout chamando serviço externo.

## Componentes Kotlin sugeridos

Estrutura possível:

```text
controllers/
  ProviderManifestController.kt
  PaymentController.kt
  PaymentCallbackController.kt
  InternalPaymentController.kt

services/
  PaymentService.kt
  PaymentOperationService.kt
  PaymentStatusMapper.kt

clients/
  EcommerceClient.kt
  PaymentGatewayClient.kt

dtos/
  request/
  response/

exceptions/
  ApiException.kt
  GlobalExceptionHandler.kt
```

Responsabilidades:

- `ProviderManifestController`: expor capacidades do conector.
- `PaymentController`: criar, consultar, autorizar, capturar, cancelar, estornar e sincronizar pagamentos.
- `PaymentCallbackController`: receber eventos externos.
- `PaymentService`: concentrar regras de negócio e transições de status.
- `PaymentOperationService`: controlar idempotência de operações.
- `EcommerceClient`: buscar pedido no ecommerce fake quando necessário.
- `PaymentGatewayClient`: chamar a API fake de pagamento.
- `PaymentStatusMapper`: traduzir status externos para status locais.

## Fluxo completo para simular

Subir dependências:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Subir a aplicação Kotlin:

```bash
./gradlew bootRun
```

Consultar o manifest:

```bash
curl http://localhost:8085/provider/manifest
```

Criar pedido no ecommerce fake:

```bash
curl -X POST http://localhost:8090/orders \
  -H 'Content-Type: application/json' \
  -d '{"customer_email":"cliente@example.com","total_amount":32980}'
```

Criar pagamento no conector:

```bash
curl -X POST http://localhost:8085/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "ecommerce_order_id":"ord_123",
    "amount_in_cents":32980,
    "currency":"BRL",
    "payment_method":"card",
    "capture_mode":"manual",
    "success_url":"http://localhost:8090/checkout/success",
    "cancel_url":"http://localhost:8090/checkout/cancel",
    "idempotency_key":"checkout-ord_123-attempt-1"
  }'
```

O que deve ocorrer:

1. Conector valida request.
2. Conector cria `PaymentTransaction` como `PENDING`.
3. Conector chama `POST /v1/payment_intents`.
4. Conector salva `gatewayPaymentId`.
5. Conector retorna a transação e, se aplicável, uma próxima ação.

Autorizar pagamento:

```bash
curl -X POST http://localhost:8085/payments/{paymentId}/authorize
```

Capturar pagamento:

```bash
curl -X POST http://localhost:8085/payments/{paymentId}/capture \
  -H 'Content-Type: application/json' \
  -d '{"amount_in_cents":32980,"idempotency_key":"capture-ord_123-1"}'
```

Consultar pagamento:

```bash
curl http://localhost:8085/payments/{paymentId}
```

Estornar pagamento:

```bash
curl -X POST http://localhost:8085/payments/{paymentId}/refund \
  -H 'Content-Type: application/json' \
  -d '{"amount_in_cents":32980,"reason":"customer_requested","idempotency_key":"refund-ord_123-1"}'
```

Simular callback:

```bash
curl -X POST http://localhost:8085/callbacks/payment-events \
  -H 'Content-Type: application/json' \
  -d '{
    "event_id":"evt_123",
    "type":"payment.succeeded",
    "gateway_payment_id":"pi_123",
    "occurred_at":"2026-06-11T16:01:00-03:00"
  }'
```

## Cenários de teste recomendados

Criação feliz:

- Dado request válido
- Quando `POST /payments`
- Então criar transação local e intenção no gateway

Manifest:

- Dado conector rodando
- Quando `GET /provider/manifest`
- Então retornar capacidades sem chamar o gateway

Idempotência:

- Dado `idempotency_key` já processada
- Quando repetir a mesma chamada
- Então retornar o mesmo resultado sem criar nova cobrança

Captura:

- Dado transação `AUTHORIZED`
- Quando `POST /payments/{paymentId}/capture`
- Então atualizar para `PAID`

Cancelamento:

- Dado transação `PENDING` ou `AUTHORIZED`
- Quando `POST /payments/{paymentId}/cancel`
- Então atualizar para `CANCELED`

Refund:

- Dado transação `PAID`
- Quando `POST /payments/{paymentId}/refund`
- Então atualizar para `REFUNDED`

Callback:

- Dado evento `payment.succeeded`
- Quando `POST /callbacks/payment-events`
- Então atualizar a transação relacionada para `PAID`

Status inválido:

- Dado transação `REFUNDED`
- Quando tentar capturar/cancelar
- Então retornar `409 Conflict`

Gateway indisponível:

- Dado gateway fora do ar
- Quando criar ou capturar pagamento
- Então retornar `502` ou `504` e preservar estado local coerente

## Ordem de implementação sugerida

1. DTOs de request/response.
2. Tratamento global de erros.
3. `GET /provider/manifest`.
4. `PaymentGatewayClient` para criar, confirmar e estornar payment intents.
5. `PaymentService` com validações e transições de status.
6. `POST /payments` e consultas.
7. `authorize`, `capture`, `cancel` e `refund`.
8. `sync` com gateway.
9. Callback de eventos.
10. Idempotência persistida por operação.

## Evoluções prováveis do modelo

O modelo atual é suficiente para o primeiro ciclo, mas alguns recursos pedem novas colunas ou tabelas:

- `externalPaymentId`: identificador da sessão/pagamento no ecommerce.
- `idempotencyKey`: chave de idempotência de criação.
- `authorizationId`: autorização retornada pelo gateway.
- `captureId`: identificador de captura.
- `failureReason`: motivo da falha.
- `customerEmail`: email do comprador para auditoria.
- `paymentMethod`: método usado, por exemplo `card` ou `pix`.
- `captureMode`: `automatic` ou `manual`.
- `refundedAmountInCents`: valor estornado.
- `successUrl` e `cancelUrl`: URLs recebidas do ecommerce.
- `metadata`: JSON com dados externos relevantes.
- Tabela `payment_operations`: histórico idempotente de authorize, capture, cancel e refund.
- Tabela `payment_events`: eventos recebidos de ecommerce/gateway.
- Tabela `payment_refunds`: controle de múltiplos estornos.

Para estudo, comece pequeno: uma transação por pedido, captura total, estorno total e status local bem controlado. Depois evolua para eventos, idempotência persistida e operações parciais.
