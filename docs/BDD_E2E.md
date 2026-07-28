# BDD E2E — compensação da SAGA de pagamento

O cenário Cucumber automatiza o recorte de compensação homologado:

```text
Payment REJECTED
  -> PaymentRejected
  -> SAGA COMPENSATING
  -> CancelServiceOrder
  -> Operation CANCELLED
  -> ServiceOrderCancelled
  -> SAGA FAILED
```

## Dados acadêmicos de teste

O cenário usa dados determinísticos criados pelas migrations dos serviços:

| Domínio | Dado | Identificador |
|---|---|---|
| Registry | Cliente Pitflow | `366941cf-9853-4514-ae99-1e1ea2b984ea` |
| Registry | Veículo de Teste BDD | `bdd00000-0000-4000-8000-000000000001` |
| Inventory | Alinhamento e Balanceamento | `3ad26f19-d339-446c-8185-e8bf4235ac1e` |

Esses registros são fixtures acadêmicas para homologação e demonstração. Não
representam dados comerciais. Antes de uso produtivo, devem ser removidos ou
condicionados ao ambiente.

O BDD valida as fixtures pelas APIs públicas e cria uma ordem de serviço nova
em cada execução. Em seguida, inicia e conclui o diagnóstico, aprova o orçamento
pela API e aguarda a ordem chegar a `AWAITING_PAYMENT`. Nenhuma OS precisa ser
preparada manualmente.

O setup tenta autenticar o mecânico informado por `BDD_MECHANIC_USERNAME` e
`BDD_MECHANIC_PASSWORD`. Se o usuário ainda não existir, cadastra um mecânico
acadêmico com esses mesmos valores e repete a autenticação. Se o username já
existir com outra senha, a execução falha para indicar inconsistência dos
secrets.

## Execução local

Configure credenciais AWS temporárias com leitura da tabela DynamoDB e exporte:

```bash
export BDD_API_URL="https://example.execute-api.us-east-1.amazonaws.com"
export BDD_MECHANIC_USERNAME="mechanic"
export BDD_MECHANIC_PASSWORD="secret"
export AWS_REGION="us-east-1"
export BDD_ORCHESTRATOR_TABLE="pitflow-orchestrator"

mvn -B verify -Pbdd-e2e
```

Variáveis opcionais:

- `BDD_TIMEOUT_SECONDS`: timeout total de cada espera; padrão `120`;
- `BDD_POLL_INTERVAL_SECONDS`: intervalo do polling; padrão `3`;
- `BDD_ORCHESTRATOR_TABLE`: tabela da SAGA; padrão `pitflow-orchestrator`.

O relatório é gravado em `target/cucumber-report.html` e
`target/cucumber-report.json`.

## GitHub Actions

1. Cadastre `BDD_MECHANIC_USERNAME` e `BDD_MECHANIC_PASSWORD` como GitHub
   Secrets do `pitflow-payment`.
2. Renove as credenciais temporárias AWS já usadas pelos workflows.
3. Publique primeiro a migration do Registry e confirme o rollout.
4. Abra a Action `Payment SAGA BDD E2E` e execute o workflow.
5. Baixe o artefato gerado pela execução.

O workflow consulta `API_PUBLIC_URL` no secret `pitflow/bootstrap`, não imprime
credenciais e executa somente por `workflow_dispatch`.

## Evidência homologada — 27/07/2026

A execução automatizada na AWS foi aprovada:

```text
1 scenarios (1 passed)
10 steps (10 passed)
```

Evidências correlacionadas:

| Componente | Identificador | Estado final |
|---|---|---|
| Operation | `9a982779-841f-4e29-acca-58813e5c87ad` | `CANCELLED` |
| Payment | `c6b3db0d-fafc-4f70-ad0c-36f444294bc1` | `REJECTED` |
| Orchestrator/SAGA | `e88a8f58-15bf-45ee-a21d-dc5dd31299f4` | `FAILED`, versão 5 |

Na mesma execução, 33 testes unitários e 9 testes de persistência com
PostgreSQL/Testcontainers foram aprovados. O replay da rejeição confirmou
idempotência sem nova versão da SAGA.

Mensagens `ERROR` e `WARN` emitidas antes do cenário são esperadas nos testes
negativos de sanitização de exceções, assinatura inválida de webhook e
constraints de unicidade; não representam falhas da execução.
