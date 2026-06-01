# Payment Connector

API em Kotlin com Spring Boot criada para estudar o desenvolvimento de um conector de pagamento entre um ecommerce e um gateway de pagamento.

O objetivo do projeto é exercitar o desenho de uma integração intermediária: receber ou buscar dados de compra no ecommerce, criar transações no gateway, acompanhar status de pagamento e expor uma API própria para orquestrar esse fluxo.

## Stack

- Kotlin
- Spring Boot
- Gradle Kotlin DSL
- Docker Compose para dependências fake de integração

## Rodando a aplicação

```bash
./gradlew bootRun
```

Por padrão, a API sobe em:

```text
http://localhost:8085
```

## Ambiente local de integração

O projeto inclui um ambiente local com ecommerce fake e gateway de pagamento fake para desenvolvimento e testes manuais.

Veja o guia completo em [integrations/README.md](integrations/README.md).
