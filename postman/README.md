# Postman — Financial Control

Coleção oficial para testes manuais e validação da API REST do Financial Control.

A coleção é mantida junto ao código-fonte e deve acompanhar a evolução do projeto por fases.

---

## Estrutura

A coleção segue a seguinte organização:

Financial Control API  
├── 00 - Sistema  
│   └── Health  
│  
├── 01 - Cadastros  
│   ├── Cartões          (pasta reservada; vazia — fases posteriores)  
│   ├── Categorias       (pasta reservada; vazia nesta collection)  
│   ├── Contas           (Fase 4 — implementado)  
│   └── Usuários         (cadastro)  
│  
├── 02 - Processos  
│   ├── Autenticação     (Fase 3 — implementado)  
│   └── Transferências   (pasta reservada; vazia — fases posteriores)  
│  
└── 99 - Cenários de testes

Estado da collection em relação às fases:

- Fases 3 e 4: presentes (health, auth, usuários, contas).
- Fase 5 (categorias): API implementada; a pasta `Categorias` na collection ainda está vazia — preencher na manutenção da collection, sem inventar contrato.
- Fase 6 (receitas): API implementada; **ainda não há pasta de Receitas** nesta collection. Adicionar na manutenção da collection, alinhada a `docs/25`.
- Fase 7 (despesas simples): API implementada (`/api/v1/expenses`, `GET /api/v1/payments/{id}`). **Ainda não há pasta de Despesas** nesta collection. Adicionar na manutenção da collection, alinhada a `docs/25`.

Não adicionar requests de endpoints que ainda não existem no backend.

### Regras de organização

- **Cadastros** representam entidades e sua manutenção.
- **Processos** representam operações ou fluxos de negócio.
- **Cenários de Teste** representam fluxos completos envolvendo múltiplas operações.
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

---

## 2. Importar o Environment

Importe o arquivo:

`Financial Control — Local.postman_environment.json`

Depois selecione o ambiente:

`Financial Control — Local`

O Environment versionado contém somente a estrutura das variáveis.

Valores locais ou sensíveis não devem ser versionados.

---

# Variáveis

O ambiente utiliza as seguintes variáveis:

| Variável | Finalidade |
|---|---|
| `baseUrl` | URL base da API |
| `accessToken` | JWT utilizado na autenticação |
| `userId` | ID do usuário utilizado nos testes |
| `accountId` | ID da conta utilizada nos testes |
| `userName` | Nome do usuário de teste |
| `userEmail` | E-mail do usuário de teste |
| `userPassword` | Senha do usuário de teste |

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

**Health → Usuários → Criar → Autenticação → Login → Contas → Criar → Demais operações**

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
- `Ativar`
- `Desativar`

Ao criar uma conta, o ID retornado é armazenado automaticamente em:

`{{accountId}}`

As requests seguintes utilizam essa variável para consultar e manipular a conta criada.

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

---

# Autenticação

As rotas protegidas utilizam:

`Authorization: Bearer {{accessToken}}`

A autenticação é configurada na Collection para que as requests protegidas herdem automaticamente o token.

Endpoints públicos, como cadastro, login e health check, utilizam `No Auth` quando necessário.

O Access Token utilizado pelo projeto é um JWT com validade definida pelo backend.

---

# Fluxo das variáveis

A Collection utiliza variáveis para evitar cópia manual de identificadores.

**Criar usuário**  
↓  
`{{userId}}`  
↓  
**Login**  
↓  
`{{accessToken}}`  
↓  
**Criar conta**  
↓  
`{{accountId}}`  
↓  
**Consultar / Alterar / Consultar Saldo / Desativar / Ativar**

---

# Manutenção

A Collection e o Environment de exemplo fazem parte do projeto e devem acompanhar as alterações da API.

Ao adicionar ou alterar um endpoint:

1. Atualize a Collection.
2. Atualize os scripts/assertions necessários.
3. Atualize o Environment se houver novas variáveis.
4. Atualize este README se o fluxo de utilização mudar.
5. Faça o commit junto com a alteração correspondente da fase.

A Collection não deve conter endpoints que não existam no contrato oficial da API.

---

# Fonte de verdade

O Postman **não é a fonte de verdade do sistema**.

Os contratos oficiais permanecem na documentação do projeto, especialmente:

- `docs/25-api.md`
- `docs/26-seguranca.md`
- `docs/27-testes.md`
- `docs/28-roadmap.md`

Em caso de divergência entre o Postman e a documentação oficial, a documentação deve ser corrigida/alinhada antes de continuar.

O Postman existe para facilitar a execução e validação prática desses contratos.