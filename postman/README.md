# Postman — Financial Control

Coleção oficial para testes manuais e validação da API REST do Financial Control.

A coleção é mantida junto ao código-fonte e deve acompanhar a evolução do projeto por fases.

A fonte final para existência de uma rota é o backend implementado. Esta collection não contém endpoints fictícios nem contratos futuros do roadmap.

---

## Estrutura

A coleção segue a seguinte organização:

```text
Financial Control API
├── 00 - Sistema
│   └── Health
│
├── 01 - Cadastros
│   ├── Cartões
│   ├── Categorias
│   ├── Contas
│   └── Usuários
│
├── 02 - Processos
│   ├── Acerto de Saldos
│   ├── Autenticação
│   ├── Despesas
│   ├── Faturas
│   ├── Metas
│   ├── Negociações
│   ├── Pagamentos
│   ├── Receitas
│   └── Transferências
│
└── 99 - Cenários de testes
```

Cadastros e processos permanecem em ordem alfabética.

Estado da collection em relação às fases implementadas:

- Fases 3 e 4: health, autenticação, usuários, contas (incluindo saldo inicial da Fase 14).
- Fase 5: categorias.
- Fase 6: receitas.
- Fases 7 e 8: despesas, parcelas, pagamentos e ajustes de parcela.
- Fase 9: cartões, créditos, faturas e pagamentos/ajustes de fatura.
- Fase 13: negociações, renegociação e antecipação de parcela.
- Fase 14: transferências, acerto de saldos e `PUT /accounts/{id}/initial-balance`.
- Fase 15: metas financeiras (10 rotas em `02 - Processos → Metas`).

Não adicionar requests de endpoints que ainda não existem no backend. Não existem, por exemplo:

- `POST /api/v1/auth/refresh`
- `GET /api/v1/categories/{id}`
- `POST /api/v1/categories/{id}/activate`
- `DELETE` de recursos financeiros
- `GET /api/v1/accounts/{id}/statement`
- `POST /api/v1/invoices/{id}/close`
- `GET /api/v1/invoices` (listagem geral; a listagem é por cartão)

### Regras de organização

- **Cadastros** representam entidades e sua manutenção.
- **Processos** representam operações ou fluxos de negócio.
- **Cenários de Teste** representam fluxos pontuais, inclusive casos negativos críticos.
- Cadastros e processos devem permanecer organizados alfabeticamente.
- Os nomes das requests descrevem a ação e não repetem o método HTTP.
- O método HTTP já é exibido pelo Postman.

Exemplos de nomes de requests:

- `Listar`
- `Consultar`
- `Criar`
- `Alterar`
- `Ativar`
- `Desativar`
- `Receber`
- `Pagar`
- `Cancelar`
- `Estornar`
- `Refundar`
- `Login`

---

# Importação

## 1. Importar a Collection

No Postman:

1. Selecione **Import**.
2. Escolha o arquivo `Financial Control API.postman_collection.json`.
3. Confirme a importação.

A Collection contém:

- endpoints;
- métodos HTTP;
- URLs;
- bodies;
- scripts;
- variáveis;
- autenticação;
- testes/assertions;
- organização das pastas.

## 2. Importar o Environment

Importe o arquivo:

`Financial Control — Local.postman_environment.json`

Depois selecione o ambiente:

`Financial Control — Local`

O Environment versionado contém somente a estrutura das variáveis.

Valores locais ou sensíveis não devem ser versionados.

A Collection define `baseUrl` padrão `http://localhost:8080`. O Environment pode sobrescrever esse valor.

---

# Variáveis

O ambiente utiliza as seguintes variáveis:

| Variável | Finalidade |
|---|---|
| `baseUrl` | URL base da API |
| `accessToken` | JWT utilizado na autenticação |
| `userId` | ID do usuário utilizado nos testes |
| `userName` | Nome do usuário de teste |
| `userEmail` | E-mail do usuário de teste |
| `userPassword` | Senha do usuário de teste |
| `accountId` | ID da conta principal |
| `destinationAccountId` | ID da segunda conta (`BANK_ACCOUNT`) para transferências |
| `categoryId` | ID da última categoria criada |
| `expenseCategoryId` | Categoria `EXPENSE` |
| `incomeCategoryId` | Categoria `INCOME` |
| `creditCardId` | ID do cartão |
| `incomeId` | ID da receita |
| `expenseId` | ID da despesa |
| `installmentId` | ID da parcela da despesa |
| `adjustmentId` | ID do ajuste da parcela |
| `paymentId` | ID do pagamento da despesa (`/payments`) |
| `invoiceId` | ID da fatura |
| `invoicePaymentId` | ID do pagamento da fatura |
| `invoiceAdjustmentId` | ID do ajuste da fatura |
| `agreementId` | ID da negociação |
| `agreementInstallmentId` | ID da parcela da negociação |
| `transferId` | ID da transferência |
| `balanceAdjustmentId` | ID do acerto de saldo |
| `financialGoalId` | ID da meta financeira |

A Collection também possui:

| Variável | Finalidade |
|---|---|
| `today` | Data corrente em `America/Sao_Paulo` (`YYYY-MM-DD`), preenchida pelo script de pré-request |

A variável `baseUrl` deve apontar para a instância local da API:

`http://localhost:8080`

As demais variáveis de execução podem ser preenchidas automaticamente pelos scripts da Collection.

---

# Segurança

## Não versionar valores reais

O Environment versionado deve permanecer sem:

- JWTs reais;
- senhas pessoais;
- credenciais;
- tokens;
- segredos;
- dados sensíveis.

O arquivo versionado funciona como um **modelo de ambiente**.

Cada desenvolvedor deve preencher seus valores locais quando necessário.

Antes da primeira execução, preencha no Environment local:

- `userName`
- `userEmail`
- `userPassword`

---

# Pré-requisitos

Antes de executar os testes da API, certifique-se de que:

- PostgreSQL está em execução;
- backend está em execução;
- o ambiente `Financial Control — Local` está selecionado no Postman.

O backend local utiliza:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/api/v1/health`

Consulte o `README.md` e `ManualExecucao.md` na raiz do projeto para obter as instruções completas de execução do ambiente.

---

# Primeira execução

Para iniciar uma sessão de testes do zero, utilize esta sequência:

**Health → Usuários → Criar → Autenticação → Login → Contas → Criar → Categorias → Criar → Demais operações**

## 1. Health

Execute:

`00 - Sistema → Health`

Resultado esperado:

**200 OK**

Resposta:

`{"status":"UP"}`

---

## 2. Criar usuário

Execute:

`01 - Cadastros → Usuários → Criar`

O cadastro cria um usuário para os testes locais.

O script da request salva automaticamente o ID retornado em:

`{{userId}}`

---

## 3. Login

Execute:

`02 - Processos → Autenticação → Login`

O login utiliza o usuário criado anteriormente.

O script da request captura automaticamente o JWT retornado pela API e salva em:

`{{accessToken}}`

As requests autenticadas utilizam esse token automaticamente.

---

# Testes de Contas

Depois de realizar o login, as operações de contas podem ser executadas em:

`01 - Cadastros → Contas`

As requests disponíveis são:

- `Listar`
- `Consultar`
- `Criar`
- `Alterar`
- `Consultar Saldo`
- `Alterar saldo inicial`
- `Ativar`
- `Desativar`

Ao criar uma conta, o ID retornado é armazenado automaticamente em:

`{{accountId}}`

`Alterar saldo inicial` chama `PUT /api/v1/accounts/{id}/initial-balance`. Não existe outro endpoint de criação de saldo inicial.

Para transferências, crie uma segunda conta `BANK_ACCOUNT` (execute `Criar` de novo com outro nome) e em seguida `Listar`. O script de `Listar` preenche `{{destinationAccountId}}` quando houver pelo menos duas contas.

---

# Categorias, receitas e despesas

`01 - Cadastros → Categorias → Criar` nasce como `EXPENSE` e preenche `{{expenseCategoryId}}`.

Receitas exigem categoria `INCOME`. Altere o body para `"type": "INCOME"`, execute `Criar` novamente e o script preencherá `{{incomeCategoryId}}`.

Não existe `GET /api/v1/categories/{id}` nem reativação de categoria.

---

# Cartões, faturas e negociações

`01 - Cadastros → Cartões` cobre cadastro, limite derivado, créditos, ativar e desativar.

Faturas ficam em `02 - Processos → Faturas`. A listagem é por cartão (`GET /credit-cards/{id}/invoices`). `Consultar atual` retorna a fatura `OPEN` ou **404** se ela não existir.

Negociações ficam em `02 - Processos → Negociações`. A fatura precisa estar `CLOSED` com remaining > 0. Renegociação exige `anticipatedFuturesNetAmount` (envie `0` quando não houver futuros).

---

# Transferências e acerto de saldos

`02 - Processos → Transferências`: listar, consultar, criar e estornar. Não há PUT.

`02 - Processos → Acerto de Saldos`: listar, consultar, criar e estornar em `/api/v1/accounts/{accountId}/balance-adjustments`.

Não confundir acerto de saldos com ajustes de parcela ou de fatura.

---

# Metas

`02 - Processos → Metas` cobre as 10 rotas da Fase 15:

- `Listar` / `Consultar` / `Criar` / `Alterar`
- `Contribuir` / `Listar contribuições`
- `Resgatar` / `Listar resgates`
- `Concluir` / `Cancelar`

`Criar` preenche `{{financialGoalId}}`. Contribuição e resgate usam a conta vinculada da meta — o body **não** envia `accountId`. Não existem `DELETE` nem reverse.

`Concluir` é manual (permitido com `currentAmount = 0`). `Cancelar` exige meta `ACTIVE` com reservado zero. Resgate também é permitido em `COMPLETED` e não reabre a meta.

---

# Testes automatizados das requests

As requests possuem scripts de **Post-response** para validar as respostas da API.

Os testes verificam, conforme cada operação:

- código HTTP esperado;
- presença de campos obrigatórios;
- valores e estados esperados;
- identificação correta dos recursos;
- regras específicas do contrato da API.

Uma resposta HTTP `200` ou `201`, por si só, não significa que o teste passou.

O Postman também executa as assertions configuradas na request.

Alguns testes de estado (por exemplo `Cancelar` ou `Refundar`) pressupõem a transição válida. Execute-os no contexto correto da despesa/receita; não é um runner linear de toda a pasta.

---

# Autenticação

As rotas protegidas utilizam:

`Authorization: Bearer {{accessToken}}`

A autenticação é configurada na Collection para que as requests protegidas herdem automaticamente o token.

Endpoints públicos, como cadastro, login e health check, utilizam `No Auth`.

O Access Token utilizado pelo projeto é um JWT com validade definida pelo backend (30 minutos). Refresh Token não está implementado.

---

# Fluxo das variáveis

A Collection utiliza variáveis para evitar cópia manual de identificadores.

```text
Criar usuário
 ↓
{{userId}}
 ↓
Login
 ↓
{{accessToken}}
 ↓
Criar conta
 ↓
{{accountId}}
 ↓
Criar categoria
 ↓
{{expenseCategoryId}} / {{incomeCategoryId}}
 ↓
Criar cartão / receita / despesa / transferência / meta
 ↓
IDs correspondentes preenchidos pelos scripts
```

A data `{{today}}` é definida automaticamente antes de cada request, no fuso `America/Sao_Paulo`.

---

# Cenários negativos

`99 - Cenários de testes` contém exemplos pontuais, sem esgotar as regras:

- login com credenciais inválidas (**401**);
- consulta autenticada sem token (**401**);
- conta inexistente (**404**);
- transferência com saldo insuficiente (**400** `BUSINESS_RULE_VIOLATION`);
- contribuição com saldo disponível insuficiente (**400** `BUSINESS_RULE_VIOLATION`);
- resgate acima do `currentAmount` (**400** `BUSINESS_RULE_VIOLATION`);
- listagem de metas com `page < 0` (**400** `BUSINESS_RULE_VIOLATION`);
- criar meta com conta inexistente (**404**);
- consultar meta inexistente (**404**);
- concluir meta já concluída (**400** `BUSINESS_RULE_VIOLATION`).

---

# Manutenção

A Collection e o Environment de exemplo fazem parte do projeto e devem acompanhar as alterações da API.

Ao adicionar ou alterar um endpoint:

1. Atualize a Collection.
2. Atualize os scripts/assertions necessários.
3. Atualize o Environment se houver novas variáveis.
4. Atualize este README se o fluxo de utilização mudar.
5. Faça o commit junto com a alteração correspondente da fase.

A Collection não deve conter endpoints que não existam no backend.

---

# Fonte de verdade

O Postman **não é a fonte de verdade do sistema**.

Os contratos oficiais permanecem na documentação do projeto, especialmente:

- `docs/25-api.md`
- `docs/26-seguranca.md`
- `docs/27-testes.md`
- `docs/28-roadmap.md`

Em caso de divergência entre o Postman e a documentação oficial, a documentação deve ser corrigida/alinhada antes de continuar.

Se a documentação e o backend divergirem, esta collection segue o endpoint efetivamente implementado.

O Postman existe para facilitar a execução e validação prática desses contratos.
