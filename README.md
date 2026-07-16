# PitFlow Payment Service

Microserviço independente responsável pela obrigação financeira, histórico de tentativas e fundações de Inbox/Outbox do PitFlow. O estado atual cobre as Fases 1–3: scaffolding, domínio inicial e persistência.

## Stack

Java 21, Spring Boot 4.0.1, Maven, PostgreSQL 16, Liquibase, Spring Data JPA, Actuator, Springdoc OpenAPI, JUnit 5, Mockito, AssertJ e Testcontainers.

## Arquitetura

O core (`payment/core` e `common/core`) é Java puro. Spring, HTTP e JPA permanecem em `infrastructure`. Entidades JPA e entidades de domínio são separadas e convertidas por mappers manuais. Operações atômicas usam a porta `TransactionGateway`.

Estrutura principal:

```text
src/main/java/br/com/pitflow
├── common/{core,infrastructure}
├── payment/core/{entity,enums,exception,gateway,usecase}
├── payment/infrastructure/persistence/{adapter,entity,mapper,repository}
└── PitflowPaymentApplication.java
```

## Execução local

Copie `.env.example` para `.env`, ajuste somente valores locais e execute:

```bash
docker compose up -d pitflow-payment-db-local
mvn spring-boot:run
```

O PostgreSQL fica em `localhost:5433`, database/user `pitflow_payment` por padrão. Health checks: `/actuator/health`, `/actuator/health/liveness` e `/actuator/health/readiness`. OpenAPI: `/swagger-ui.html`.

## Build e testes

```bash
mvn clean verify
docker compose config
```

Os testes de integração usam PostgreSQL real via Testcontainers e são ignorados explicitamente quando Docker não está disponível; H2 não é usado.

## Liquibase

O Hibernate usa apenas `ddl-auto: validate`. O Liquibase aplica SQL PostgreSQL nativo em `src/main/resources/db/changelog/migrations`: payments, payment attempts, webhook Inbox e Outbox.

## Variáveis

As variáveis suportadas estão documentadas em `.env.example`. Valores do Mercado Pago e do Service Order são placeholders para fases futuras e não são consumidos nesta versão.

## Limitações e próximas etapas

Não há endpoints de pagamentos, autenticação interna, chamadas ao Mercado Pago, webhook funcional, processadores Inbox/Outbox, callback ou reconciliação. A próxima etapa é a Fase 4, API interna de pagamentos e autenticação por `X-Internal-Api-Key`.
