# Ambiente local de desenvolvimento

Este diretório guarda a configuração dos serviços fake usados no desenvolvimento do `payment-connector`.

## Subir banco + ecommerce fake + gateway fake

```bash
docker compose -f docker-compose.dev.yml up -d
```

Endpoints:

- PostgreSQL: `localhost:5432`
- Ecommerce fake REST: `http://localhost:8090`
- Stripe mock HTTP: `http://localhost:12111`
- Stripe mock HTTPS: `https://localhost:12112`

Chamadas úteis:

```bash
curl http://localhost:8090/products

curl -X POST http://localhost:8090/orders \
  -H 'Content-Type: application/json' \
  -d '{"customer_email":"cliente@example.com","total_amount":32980}'

curl -X POST http://localhost:12111/v1/payment_intents \
  -u sk_test_fake: \
  -d amount=32980 \
  -d currency=brl \
  -d payment_method_types[]=card
```

Para a aplicação Kotlin, os valores locais esperados são:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/payment_connector
spring.datasource.username=payment_connector
spring.datasource.password=payment_connector
integrations.ecommerce.base-url=http://localhost:8090
integrations.payment-gateway.base-url=http://localhost:12111
```

Essas propriedades ainda precisam ser conectadas ao código quando você criar os clients HTTP.

## Ver rotas disponíveis

### Ecommerce fake

As rotas do ecommerce fake são os mappings do WireMock em `integrations/wiremock/ecommerce/mappings`.

Para listar pelo terminal:

```bash
curl http://localhost:8090/__admin/mappings
```

Rotas configuradas hoje:

- `GET /products`
- `GET /products/{productId}`
- `POST /orders`
- `GET /orders/{orderId}`
- `POST /checkout/sessions`

### Gateway fake

O gateway fake usa `stripe/stripe-mock`, que simula a API da Stripe.

Rotas úteis para essa integração:

- `POST /v1/payment_intents`
- `GET /v1/payment_intents/{id}`
- `POST /v1/payment_intents/{id}/confirm`
- `POST /v1/refunds`
- `GET /v1/refunds/{id}`
- `POST /v1/customers`
- `GET /v1/customers/{id}`

Para testar:

```bash
curl -X POST http://localhost:12111/v1/payment_intents \
  -u sk_test_fake: \
  -d amount=32980 \
  -d currency=brl \
  -d payment_method_types[]=card
```

Algumas versões da imagem também expõem a especificação OpenAPI localmente:

```bash
curl http://localhost:12111/spec
```
