# Tarefa: iniciar a construção do microserviço `pitflow-payment`

Atue como um engenheiro de software sênior especializado em:

- Java 21;
- Spring Boot;
- Maven;
- Clean Architecture;
- DDD;
- PostgreSQL;
- Liquibase;
- SQL nativo;
- APIs REST;
- segurança entre microserviços;
- testes automatizados;
- Docker e Docker Compose.

## Diretório de trabalho

A pasta atual já é a raiz do novo projeto:

```text
pitflow-payment
Não crie outra pasta pitflow-payment dentro dela.

Todos os arquivos do serviço devem ser criados diretamente no diretório atual.

Documento obrigatório de referência

Na raiz do projeto existe o arquivo:

PITFLOW_PAYMENT_SERVICE_DEFINITIONS_AND_PLAN.md

Esse arquivo é a principal fonte de verdade para:

arquitetura;
responsabilidades;
limites entre serviços;
modelo de domínio;
modelo de dados;
contratos;
segurança;
idempotência;
Inbox;
Outbox;
reconciliação;
plano de execução;
critérios de aceite.

Antes de alterar qualquer arquivo:

leia o documento integralmente;
identifique as decisões já aprovadas;
identifique os pontos ainda pendentes;
não contradiga decisões registradas;
não altere decisões arquiteturais sem apresentar justificativa e pedir aprovação;
não marque uma etapa como concluída sem evidência objetiva.
Objetivo desta execução

Nesta execução, implemente apenas a fundação do projeto e, se a fundação estiver estável, a base inicial do domínio e da persistência.

Não tente implementar toda a integração com o Mercado Pago em uma única execução.

O trabalho deverá ser dividido em checkpoints.

Escopo obrigatório desta execução

Executar:

Fase 1 — scaffolding do Payment Service;
Fase 2 — domínio inicial;
Fase 3 — persistência inicial e migrations.

Não executar ainda, salvo autorização posterior:

chamadas reais ao Mercado Pago;
webhook;
Inbox funcional completa;
dispatcher da Outbox;
callback para o Service Order Backend;
reconciliação;
Kubernetes;
pipeline CI/CD;
alterações no pitflow-os-backend.

Pode criar interfaces, estruturas e tabelas necessárias para as fases futuras, mas não implemente integrações externas reais nesta etapa.

1. Processo obrigatório de trabalho

Antes de escrever código, apresente:

resumo das decisões relevantes encontradas no documento;
dependências propostas;
árvore de arquivos planejada;
modelo inicial de banco;
migrations planejadas;
possíveis dúvidas ou divergências;
sequência de implementação.

Depois disso, prossiga com a implementação sem aguardar confirmação apenas se não houver dúvida bloqueante.

Se houver uma decisão importante não definida, pare e solicite esclarecimento.

Execução incremental

Trabalhe nesta ordem:

inspecionar o diretório;
ler o documento de definições;
criar o projeto;
configurar build;
criar estrutura de pacotes;
criar domínio;
configurar PostgreSQL e Liquibase;
criar migrations;
implementar persistência;
criar testes;
executar build e testes;
atualizar o plano de execução;
apresentar relatório final.

Após cada bloco importante:

execute os testes correspondentes;
corrija falhas antes de continuar;
não acumule erros para o fim.
2. Stack técnica

Utilize:

Java 21
Spring Boot
Maven
PostgreSQL 16
Liquibase
Spring Data JPA
Spring Validation
Spring Web
Spring Actuator
Springdoc OpenAPI
Testcontainers
JUnit 5
Mockito
AssertJ
Docker
Docker Compose

Antes de definir a versão do Spring Boot:

verifique se há algum projeto de referência disponível no filesystem;
se houver um pitflow-os-backend, consulte o pom.xml;
preserve versões compatíveis com o projeto existente;
não atualize versões apenas por serem mais recentes;
registre no relatório quais versões foram utilizadas.

Exemplo de vesão do spring boot (utilizar v4 se não achar referências): 
```
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
```

Não use bibliotecas desnecessárias.

Não adicione ainda:

Kafka;
MongoDB;
Redis;
SDK do Mercado Pago;
Resilience4j, salvo se já fizer parte do padrão do projeto;
bibliotecas de mapeamento automático sem necessidade;
Lombok, salvo se já for uma convenção explícita do projeto de referência.

Prefira código explícito.

3. Coordenadas e nome da aplicação

Utilize inicialmente:

<groupId>br.com.pitflow</groupId>
<artifactId>pitflow-payment</artifactId>
<name>pitflow-payment</name>

Classe principal:

br.com.pitflow.PitflowPaymentApplication

O nome poderá ser ajustado somente se houver conflito objetivo com as convenções do projeto existente.

4. Clean Architecture

A direção das dependências deverá apontar para o core.

O core não poderá importar:

Spring;
JPA;
Hibernate;
Jackson;
Liquibase;
HTTP clients;
Mercado Pago;
classes de infrastructure;
DTOs REST.

Estrutura inicial esperada:

src/main/java/br/com/pitflow
├── common
│   ├── core
│   │   ├── exception
│   │   └── gateway
│   └── infrastructure
│       ├── configuration
│       ├── exception
│       └── transaction
├── payment
│   ├── controller
│   │   └── dto
│   ├── core
│   │   ├── entity
│   │   ├── enums
│   │   ├── exception
│   │   ├── gateway
│   │   └── usecase
│   │       ├── inputPort
│   │       └── outputData
│   ├── infrastructure
│   │   ├── config
│   │   └── persistence
│   │       ├── adapter
│   │       ├── entity
│   │       ├── mapper
│   │       └── repository
│   └── presenter
│       └── dto
└── PitflowPaymentApplication.java

Ainda não é necessário criar pastas vazias apenas para simular completude.

Crie somente estruturas que possuam propósito na etapa atual.

5. Domínio inicial
5.1. Entidade Payment

Implemente uma entidade de domínio sem anotações Spring/JPA.

Campos mínimos:

UUID id
UUID serviceOrderId
long budgetVersion
String externalReference
String idempotencyKey
String idempotencyPayloadHash
BigDecimal amount
String currency
PaymentStatus status
PaymentProvider provider
String payerEmail
Instant approvedAt
Instant createdAt
Instant updatedAt
long version

Regras mínimas:

id obrigatório;
serviceOrderId obrigatório;
budgetVersion maior que zero;
amount obrigatório e maior que zero;
não usar double;
moeda inicial permitida: BRL;
e-mail obrigatório;
externalReference obrigatória;
idempotencyKey obrigatória;
status inicial consistente com o documento;
timestamps obrigatórios;
impedir transições inválidas;
não permitir regressão de um pagamento terminal para estado intermediário.

Não use setters públicos indiscriminadamente.

As mudanças de estado deverão ocorrer por métodos de domínio com nomes expressivos.

Exemplos conceituais:

payment.markCheckoutPending(...);
payment.markPending(...);
payment.approve(...);
payment.reject(...);
payment.cancel(...);
payment.refund(...);
payment.expire(...);
payment.markError(...);

A implementação final deve respeitar as transições definidas no documento.

5.2. Entidade PaymentAttempt

Campos mínimos:

UUID id
UUID paymentId
String providerPreferenceId
String providerPaymentId
String checkoutUrl
String providerStatus
String providerStatusDetail
Instant expiresAt
Instant createdAt
Instant updatedAt

Regras:

paymentId obrigatório;
providerPreferenceId obrigatório quando a preferência tiver sido criada;
checkoutUrl obrigatória quando a preferência tiver sido criada;
providerPaymentId poderá ser nulo inicialmente;
não expor detalhes de infraestrutura no domínio.
5.3. Enums

Criar:

public enum PaymentStatus {
    CREATED,
    CHECKOUT_PENDING,
    PENDING,
    IN_PROCESS,
    APPROVED,
    REJECTED,
    CANCELLED,
    REFUNDED,
    EXPIRED,
    ERROR
}
public enum PaymentProvider {
    MERCADO_PAGO
}

Caso sejam necessários enums para Inbox e Outbox, mantenha-os no domínio adequado e sem dependências de infraestrutura.

5.4. Exceções

Criar exceções específicas para:

dados inválidos;
transição de status inválida;
pagamento inexistente;
conflito de idempotência.

Não use RuntimeException genérica diretamente nos casos de uso.

6. Gateways iniciais

Criar interfaces no core para:

PaymentGateway
PaymentAttemptGateway
TransactionGateway
ClockGateway

Criar também os contratos estruturais, ainda sem implementação externa, para:

PaymentProviderGateway
WebhookEventGateway
OutboxEventGateway
ServiceOrderNotificationGateway

Não utilizar classes JPA ou DTOs HTTP nas assinaturas.

Exemplo de operações esperadas no PaymentGateway:

save
findById
findByIdempotencyKey
findByExternalReference
findByServiceOrderIdAndBudgetVersion
existsByServiceOrderIdAndBudgetVersion

Ajuste assinaturas para preservar linguagem de domínio.

7. Caso de uso inicial

Implemente pelo menos a fundação de:

CreatePayment
FindPaymentById
FindPaymentByServiceOrderId

O CreatePayment deverá nesta etapa:

validar a entrada;
calcular ou receber de uma porta apropriada o hash do payload idempotente;
verificar se a chave já existe;
se a mesma chave representar os mesmos dados, retornar o pagamento existente;
se a mesma chave representar dados diferentes, lançar conflito;
impedir duplicidade da mesma OS e versão de orçamento;
criar o Payment no estado inicial;
persistir em transação local.

Nesta fase, não chamar o Mercado Pago.

A criação da preferência será adicionada em uma etapa posterior.

Caso seja necessário diferenciar o estado anterior à criação da preferência, siga exatamente o documento de definições e registre qualquer decisão tomada.

8. Liquibase com SQL nativo

O gerenciamento de migrations deverá utilizar Liquibase, mas todo DDL deverá ser escrito em arquivos .sql com SQL nativo do PostgreSQL.

Não escrever criação de tabelas em YAML ou XML.

Utilize a estrutura:

src/main/resources/db/changelog
├── db.changelog-master.yaml
└── migrations
    ├── 001-create-payments-table.sql
    ├── 002-create-payment-attempts-table.sql
    ├── 003-create-webhook-events-table.sql
    └── 004-create-outbox-events-table.sql

O db.changelog-master.yaml deverá apenas incluir os arquivos SQL.

Utilize preferencialmente Liquibase Formatted SQL.

Cada arquivo deverá começar com:

--liquibase formatted sql

E possuir changeset identificável:

--changeset pitflow:001-create-payments-table

Inclua rollback quando for seguro e tecnicamente adequado:

--rollback DROP TABLE IF EXISTS payments;

Exemplo de master:

databaseChangeLog:
  - include:
      file: db/changelog/migrations/001-create-payments-table.sql
  - include:
      file: db/changelog/migrations/002-create-payment-attempts-table.sql
  - include:
      file: db/changelog/migrations/003-create-webhook-events-table.sql
  - include:
      file: db/changelog/migrations/004-create-outbox-events-table.sql

Não utilizar:

ddl-auto=create;
ddl-auto=update;
geração automática de schema pelo Hibernate;
SQL genérico incompatível com PostgreSQL.

Configurar:

spring:
  jpa:
    hibernate:
      ddl-auto: validate

O Liquibase deverá criar o schema e o Hibernate apenas validá-lo.

9. Migrations obrigatórias
9.1. payments

Criar com SQL nativo:

id UUID PK
service_order_id UUID NOT NULL
budget_version BIGINT NOT NULL
external_reference VARCHAR NOT NULL
idempotency_key VARCHAR NOT NULL
idempotency_payload_hash VARCHAR NOT NULL
amount NUMERIC(19,2) NOT NULL
currency CHAR(3) NOT NULL
status VARCHAR NOT NULL
provider VARCHAR NOT NULL
payer_email VARCHAR NOT NULL
approved_at TIMESTAMPTZ NULL
created_at TIMESTAMPTZ NOT NULL
updated_at TIMESTAMPTZ NOT NULL
version BIGINT NOT NULL

Constraints mínimas:

PK em id
UNIQUE em external_reference
UNIQUE em idempotency_key
UNIQUE em service_order_id + budget_version
CHECK amount > 0
CHECK currency = 'BRL'

Índices:

status
service_order_id
created_at
updated_at

Avalie se índices de constraints únicas já atendem algumas consultas antes de criar índices redundantes.

9.2. payment_attempts

Campos conforme o documento.

Constraints:

FK para payments(id)
UNIQUE provider_preference_id
UNIQUE provider_payment_id quando não nulo

Utilize índice parcial do PostgreSQL quando apropriado:

CREATE UNIQUE INDEX ...
ON payment_attempts(provider_payment_id)
WHERE provider_payment_id IS NOT NULL;
9.3. webhook_events

Criar como base para Inbox.

Utilizar JSONB para payload.

Criar:

unique constraint ou índice em event_key;
índices para status;
índice para next_attempt_at;
índice para received_at.
9.4. outbox_events

Utilizar JSONB para payload.

Criar índices para:

status;
next_attempt_at;
created_at;
aggregate_id.
9.5. UUID

Escolher uma estratégia consistente.

Para esta etapa, IDs podem ser gerados pela aplicação com:

UUID.randomUUID()

Não instalar extensão PostgreSQL sem necessidade.

10. Persistência JPA

Criar entidades JPA separadas das entidades de domínio:

PaymentJpa
PaymentAttemptJpa
WebhookEventJpa
OutboxEventJpa

Requisitos:

@Entity;
nomes de tabelas explícitos;
colunas explícitas;
@Version para optimistic locking onde aplicável;
tipos monetários corretos;
Instant para timestamps;
sem lógica de negócio nas entidades JPA.

Criar mappers manuais:

PaymentMapper
PaymentAttemptMapper

Não colocar anotações JPA no core.

Criar repositories Spring Data somente na infraestrutura.

Criar adapters que implementem os gateways do core.

11. Transações

Criar um TransactionGateway no core e uma implementação Spring na infraestrutura.

Não espalhar @Transactional por casos de uso do core.

A implementação poderá utilizar:

TransactionTemplate

ou uma abordagem equivalente que mantenha o core independente.

As seguintes operações deverão ser atômicas localmente:

verificação de idempotência e criação do Payment;
mudança de status e criação futura da Outbox;
processamento futuro de Inbox.

Não implementar transação distribuída.

12. Configuração

Criar application.yml usando variáveis de ambiente.

Configurações mínimas:

spring:
  application:
    name: pitflow-payment

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:pitflow_payment}
    username: ${DB_USERNAME:pitflow_payment}
    password: ${DB_PASSWORD:pitflow_payment}

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml

Não colocar credenciais reais.

Criar .env.example com:

DB_HOST=localhost
DB_PORT=5433
DB_NAME=pitflow_payment
DB_USERNAME=pitflow_payment
DB_PASSWORD=change-me

INTERNAL_API_KEY=change-me

MERCADO_PAGO_ACCESS_TOKEN=
MERCADO_PAGO_WEBHOOK_SECRET=
MERCADO_PAGO_BASE_URL=https://api.mercadopago.com
MERCADO_PAGO_ENVIRONMENT=TEST
MERCADO_PAGO_NOTIFICATION_URL=

SERVICE_ORDER_BASE_URL=

As variáveis do Mercado Pago deverão existir apenas como placeholders nesta etapa.

13. Docker

Criar um docker-compose.yml inicial contendo PostgreSQL.

Nome do container:

pitflow-payment-db-local

Imagem:

postgres:16-alpine

Porta:

5433:5432

Adicionar:

volume nomeado;
healthcheck;
variáveis vindas do .env;
network própria quando útil.

Pode incluir a aplicação no compose somente depois que houver Dockerfile funcional.

Criar Dockerfile multi-stage:

build com JDK 21;
runtime com JRE 21;
usuário não root;
cópia somente do artefato necessário;
sem secrets na imagem.
14. OpenAPI e Actuator

Configurar inicialmente:

/actuator/health
/actuator/health/liveness
/actuator/health/readiness

Configurar OpenAPI com:

PitFlow Payment API

Não expor exemplos contendo credenciais.

Nesta etapa, endpoints de domínio podem ser mínimos, mas a infraestrutura deve estar preparada.

15. Tratamento de erros

Criar handler global seguindo o padrão do projeto de referência.

Formato consistente, contendo pelo menos:

{
  "timestamp": "2026-07-15T02:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "PAYMENT_IDEMPOTENCY_CONFLICT",
  "message": "A chave de idempotência já foi utilizada com dados diferentes",
  "path": "/payments"
}

Não expor stack trace.

Mapeamentos iniciais:

400 — entrada inválida
404 — pagamento inexistente
409 — conflito de idempotência ou duplicidade
422 — transição inválida
500 — erro inesperado
16. Testes obrigatórios desta execução
16.1. Testes unitários do domínio

Criar testes para:

criação válida;
valor zero;
valor negativo;
moeda diferente de BRL;
campos obrigatórios;
transição válida;
transição inválida;
aprovação;
rejeição;
cancelamento;
reembolso;
expiração;
tentativa de regressão de estado.
16.2. Casos de uso

Testar:

criação de Payment;
chave nova;
mesma chave e mesmo payload;
mesma chave e payload diferente;
duplicidade de OS e versão;
consulta por ID;
pagamento inexistente.
16.3. Persistência

Utilizar Testcontainers com PostgreSQL real.

Não usar H2 para validar SQL PostgreSQL.

Testar:

execução das migrations;
ddl-auto=validate;
persistência de Payment;
precisão de NUMERIC(19,2);
unique de idempotency_key;
unique de external_reference;
unique de OS e versão;
check de amount;
check de currency;
optimistic locking;
persistência de JSONB em Inbox e Outbox.

Se Docker não estiver disponível no ambiente do agente, não substitua silenciosamente os testes por H2.

Registre claramente a limitação e mantenha os testes configurados para Testcontainers.

17. Atualização obrigatória do plano de execução

Durante a implementação, atualize o arquivo:

PITFLOW_PAYMENT_SERVICE_DEFINITIONS_AND_PLAN.md

Não reescreva decisões arquiteturais sem autorização.

Atualize principalmente a seção de plano e checklist.

Regras para atualização

Para cada item:

[ ] não iniciado;
[-] em andamento;
[x] concluído;
[!] bloqueado ou divergente.

Adicione ao final do documento uma seção:

## Histórico de execução

### YYYY-MM-DD — Fase X

#### Concluído

- ...

#### Evidências

- `mvn test`
- quantidade de testes executados;
- migrations criadas;
- arquivos principais;
- resultado do build.

#### Decisões técnicas

- ...

#### Pendências

- ...

#### Divergências do plano

- nenhuma;

Não marque uma fase inteira como concluída apenas porque os arquivos foram criados.

Uma fase só estará concluída quando:

código compilar;
testes correspondentes passarem;
migrations forem validadas;
não houver erro conhecido omitido;
documentação for atualizada.
18. README do projeto

Criar um README.md específico do microserviço.

Não substituir o documento de definições.

O README deve conter:

objetivo;
estado atual;
stack;
arquitetura;
estrutura de pacotes;
execução local;
PostgreSQL;
Liquibase;
comando de build;
comando de testes;
Docker Compose;
variáveis;
migrations;
limitações atuais;
próximas etapas.

Deixar explícito que a integração real com Mercado Pago ainda não faz parte desta primeira execução.

19. Comandos de validação

Execute ao final, conforme o projeto:

./mvnw clean verify

Caso não exista Maven Wrapper, crie-o ou use:

mvn clean verify

Também valide:

docker compose config

Quando Docker estiver disponível:

docker compose up -d pitflow-payment-db-local
mvn test

Apresente os resultados reais.

Não afirme que um comando passou se ele não foi executado.

20. Restrições

Não:

criar um projeto aninhado;
alterar o pitflow-os-backend;
implementar frontend;
implementar Mercado Pago real;
adicionar Kafka;
usar MongoDB;
usar H2 como substituto de PostgreSQL;
usar ddl-auto=update;
criar DDL por JPA;
escrever tabelas em YAML Liquibase;
colocar anotações de framework no core;
usar double;
versionar secrets;
deixar TODOs genéricos;
criar classes vazias apenas para completar árvore;
dizer que a fase foi concluída sem testes;
modificar silenciosamente o documento de definições.
21. Resultado esperado

Ao final desta execução, entregar:

projeto Spring Boot funcional;
Java 21 configurado;
Maven configurado;
estrutura Clean Architecture;
domínio inicial;
casos de uso iniciais;
PostgreSQL configurado;
Liquibase utilizando .sql nativo;
migrations para as quatro tabelas;
JPA isolado no adapter;
testes unitários;
testes de integração com Testcontainers;
Dockerfile;
Docker Compose;
.env.example;
README do serviço;
documento de definições atualizado;
relatório final.
Formato do relatório final

Apresente:

Resumo

O que foi implementado.

Arquivos principais

Lista dos arquivos criados ou alterados.

Migrations

Lista e finalidade de cada migration.

Testes

Comandos executados e resultados.

Decisões tomadas

Decisões não explicitamente definidas no documento.

Pendências

O que não foi implementado.

Próxima fase recomendada

Indicar exatamente qual fase deve ser executada em seguida.

Divergências

Informar qualquer divergência entre implementação e documento.