# PitFlow Payment Service

Microserviço responsável pela obrigação financeira das ordens de serviço e pela
integração com o Mercado Pago Checkout Pro.

## Stack

Java 21, Spring Boot 4, Maven, PostgreSQL 16, Liquibase, SQS, Spring Data JPA,
Actuator, Springdoc OpenAPI e Testcontainers.

## Arquitetura

O core (`payment/core` e `common/core`) é Java puro. Spring, HTTP, SQS e JPA
permanecem em `infrastructure`. O `controller` é o agregador de casos de uso; o
`@RestController` é um adapter web.

Fluxo implementado:

```text
CreatePayment (SQS)
  -> cria/reutiliza preferência do Checkout Pro
  -> persiste payment e payment_attempt
  -> outbox PaymentLinkCreated

Webhook assinado
  -> valida HMAC-SHA256
  -> consulta GET /v1/payments/{id}
  -> valida external_reference, valor e moeda
  -> atualiza payment e webhook inbox
  -> outbox PaymentApproved ou PaymentRejected
```

O publisher da outbox roda no mesmo container, com claim, lease, retry e
backoff. Não existe serviço ou pod de outbox separado.

## Execução local

```bash
docker compose up -d pitflow-payment-db-local
mvn spring-boot:run
```

Por padrão, consumer, publisher, integração e webhook ficam desabilitados no
ambiente local. As variáveis estão listadas em `.env.example`.

## Endpoints

Com o `context-path` `/payment`:

- `POST /payment/webhooks/mercado-pago`
- `/payment/swagger-ui/index.html`
- `/payment/v3/api-docs`
- `/payment/actuator/health`

O webhook é público, mas rejeita notificações sem assinatura válida. O Access
Token e a assinatura secreta nunca devem ser enviados ao cliente ou gravados no
repositório.

## Configuração do Mercado Pago

- `MERCADO_PAGO_ACCESS_TOKEN`: Access Token de teste usado pelo backend.
- `MERCADO_PAGO_WEBHOOK_SECRET`: assinatura secreta configurada no painel.
- `MERCADO_PAGO_NOTIFICATION_URL`: URL HTTPS pública do webhook.
- `MERCADO_PAGO_TEST_MODE=true`: seleciona `sandbox_init_point`.
- `MERCADO_PAGO_ENABLED=true`: habilita o adapter.
- `PAYMENT_WEBHOOK_ENABLED=true`: habilita o endpoint.

Pagamentos criados com credenciais de teste não disparam notificações reais.
Use o simulador de Webhooks do painel do Mercado Pago para homologar o receptor.

## Build e testes

```bash
mvn clean verify
docker build -t pitflow-payment:local .
```

Os testes de integração usam PostgreSQL real via Testcontainers. H2 não é usado.

## Pendências

- reconciliação periódica para webhook perdido;
- eventos de expiração/cancelamento;
- endpoints REST de consulta;
- métricas e quality gate;
- homologação integrada do webhook e da SAGA.
