# Segurança — Financial Control

## 0. Hierarquia

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

Isolamento por usuário é regra fundamental de segurança.


## 1. Objetivo

Este documento define os requisitos mínimos de segurança da aplicação Financial Control.

O sistema manipula informações financeiras pessoais.

A segurança deve ser considerada desde a primeira versão.


# 2. Princípio fundamental

Nenhum usuário pode acessar dados financeiros de outro usuário.


# 3. Autenticação

A aplicação utilizará autenticação baseada em:

JWT


Access Token + Refresh Token.

Hash de senha: Argon2id.


# 4. Login

O usuário deverá informar:

email

password


# 5. Senha

A senha nunca deve ser armazenada em texto puro.


# 6. Hash de senha

O backend deve utilizar **Argon2id** para armazenamento de senhas.


# 7. Regra

Nunca utilizar:

MD5

SHA1

SHA256


como mecanismo direto de armazenamento de senha.

Não utilizar BCrypt na V1.


# 8. Password Hash

Somente o hash deve ser armazenado no banco.


# 9. JWT

O JWT deve conter somente informações necessárias para autenticação/autorização.


# 10. JWT

Não armazenar no JWT:

senha;

passwordHash;

dados financeiros;

informações sensíveis desnecessárias.


# 11. Identificação

O JWT deve permitir identificar:

userId


e informações mínimas necessárias para autorização.


# 12. Expiração

Access tokens devem possuir tempo de expiração.


# 13. Refresh Token

A V1 utilizará Access Token e Refresh Token.

A implementação exata do fluxo de refresh será definida na fase de autenticação.

A arquitetura deve estar preparada para refresh token desde o início.


# 14. V1

Refresh token deve possuir mecanismo seguro de revogação (detalhes na implementação da autenticação).


# 15. Logout

Logout deve invalidar ou impedir a reutilização de credenciais quando aplicável à estratégia escolhida.


# 16. HTTPS

Produção deve utilizar:

HTTPS


# 17. Desenvolvimento

HTTP pode ser utilizado localmente.


# 18. Banco

O PostgreSQL não deve ficar exposto publicamente.


# 19. Banco

No ambiente local:

PostgreSQL deve estar disponível apenas para serviços necessários.


# 20. Docker

Quando executado através do Docker Compose:

o PostgreSQL deve possuir exposição de porta somente para desenvolvimento local quando necessário.


# 21. Senhas do banco

Senha do PostgreSQL nunca deve ser armazenada diretamente no código-fonte.


# 22. Environment Variables

Credenciais devem utilizar:

environment variables


# 23. .env

Arquivos:

.env


não devem ser versionados.


# 24. .env.example

O projeto deve possuir:

.env.example


sem credenciais reais.


# 25. Segredos

Nunca colocar no Git:

JWT secret;

database password;

API keys;

tokens;

credentials.


# 26. Cursor

Nenhuma credencial deve ser inserida em:

AGENTS.md

documentação;

prompts;

código-fonte.


# 27. Git

Antes de commit:

verificar se arquivos contendo segredos serão enviados.


# 28. Frontend

O Angular nunca deve possuir:

senha do banco;

JWT secret;

credenciais administrativas.


# 29. Frontend

Variáveis públicas do frontend não são segredos.


# 30. Backend

Somente o backend deve acessar o PostgreSQL.


# 31. Autorização

Autenticação responde:

"Quem é o usuário?"


Autorização responde:

"O usuário pode acessar esse recurso?"


# 32. Isolamento

Toda consulta financeira deve considerar:

userId autenticado.


# 33. Regra crítica

Nunca aceitar:

userId


do frontend para definir o proprietário do recurso.


# 34. Exemplo

Request:

POST /api/v1/expenses


não deve permitir:

{
  "userId": "outro-usuario"
}


# 35. Backend

O backend deve obter:

authenticatedUserId


do contexto de segurança.


# 36. Queries

Toda consulta financeira deve possuir filtro de usuário quando necessário.


# 37. Exemplo

Errado:

SELECT * FROM expenses WHERE id = :id


Correto conceitualmente:

SELECT *
FROM expenses
WHERE id = :id
AND user_id = :authenticatedUserId


# 38. IDOR

A aplicação deve prevenir:

Insecure Direct Object Reference


# 39. Exemplo

Usuário A não pode acessar:

/expenses/{id-do-usuario-B}


# 40. HTTP

Resposta para recurso inexistente ou não autorizado deve ser escolhida de forma a não vazar informações indevidas.


# 41. API

Endpoints privados devem exigir autenticação.


# 42. Endpoint público

Somente endpoints explicitamente definidos como públicos podem ser acessados sem JWT.


Exemplo:

POST /api/v1/auth/login


GET /api/v1/health


# 43. Validação

Todo input recebido pela API deve ser validado.


# 44. Validação

Validar:

tipo;

formato;

tamanho;

intervalo;

enum;

UUID;

datas;

valores.


# 45. Valores

Valores financeiros:

devem ser maiores que zero quando aplicável;

não podem ser NaN;

não podem possuir formato inválido.


# 46. String

Campos textuais devem possuir limites de tamanho.


# 47. SQL Injection

Nunca construir SQL utilizando concatenação de strings com dados fornecidos pelo usuário.


# 48. SQL

Utilizar:

prepared statements;

JPA/ORM;

ou mecanismo equivalente seguro.


# 49. PostgreSQL

Não permitir que dados fornecidos pelo usuário sejam concatenados diretamente em SQL.


# 50. XSS

O frontend deve tratar corretamente conteúdo fornecido pelo usuário.


# 51. HTML

Não renderizar HTML arbitrário fornecido pelo usuário.


# 52. Angular

Evitar:

innerHTML


quando não for absolutamente necessário.


# 53. CORS

Configurar CORS explicitamente.


# 54. Desenvolvimento

Permitir:

http://localhost:4200


# 55. Produção

Não utilizar:

Access-Control-Allow-Origin: *


# 56. CSRF

A estratégia de autenticação deve considerar proteção contra CSRF quando cookies forem utilizados.


# 57. JWT

Se JWT for armazenado no frontend:

avaliar cuidadosamente a estratégia de armazenamento.


# 58. Armazenamento

Evitar armazenar tokens sensíveis de maneira que facilite exposição por XSS.


# 59. Decisão

A estratégia final de armazenamento do token deve ser definida durante a implementação da autenticação.


# 60. Logs

Nunca registrar em logs:

senha;

passwordHash;

JWT;

refresh token;

credenciais;

dados financeiros desnecessários.


# 61. Logs

Logs podem conter:

requestId;

endpoint;

status;

tempo;

erro técnico.


# 62. Dados financeiros

Evitar registrar valores financeiros em logs sem necessidade.


# 63. Erros

Não retornar stack trace ao frontend.


# 64. Erros

Não retornar:

nome da tabela;

SQL;

nome de classe interna;

stack trace;

credenciais.


# 65. Mensagem

Erro deve ser útil sem revelar detalhes internos.


# 66. Exemplo

Errado:

"org.postgresql.util.PSQLException: relation expenses_user_idx does not exist"


Correto:

"Não foi possível concluir a operação."


# 67. Auditoria

Operações financeiras críticas poderão futuramente possuir auditoria.


# 68. V1

Auditoria completa não é obrigatória.


# 69. Banco

Usuário da aplicação no PostgreSQL deve possuir somente permissões necessárias.


# 70. Banco

Não utilizar:

postgres


como usuário da aplicação em ambientes não locais.


# 71. Desenvolvimento

O usuário administrativo do PostgreSQL pode ser utilizado somente para administração do banco.


# 72. Migrations

Migrations devem ser executadas de maneira controlada.


# 73. Produção

Aplicação não deve executar migrations destrutivas automaticamente sem estratégia definida.


# 74. Backup

A estratégia de backup será definida futuramente.


# 75. V1

Backup automatizado não é obrigatório para execução local.


# 76. Docker

Containers não devem executar como root sem necessidade.


# 77. Dependências

Dependências devem ser mantidas atualizadas.


# 78. Dependências

Evitar bibliotecas abandonadas quando houver alternativa madura.


# 79. Dependências

Antes de adicionar uma biblioteca:

avaliar necessidade;

manutenção;

segurança;

compatibilidade;

complexidade.


# 80. Spring Security

Caso seja utilizado Spring Boot:

utilizar Spring Security para:

autenticação;

autorização;

proteção de endpoints.


# 81. Senha

Não implementar manualmente criptografia de senha.


# 82. JWT

Não implementar manualmente parsing/validação criptográfica do JWT se biblioteca consolidada estiver disponível.


# 83. Frontend

O Angular deve possuir interceptor para anexar:

Authorization: Bearer <token>


quando aplicável à estratégia escolhida.


# 84. Token expirado

Frontend deve tratar:

401 Unauthorized


de forma previsível.


# 85. Sessão

O usuário deve ser redirecionado para autenticação quando a sessão expirar.


# 86. Rate Limiting

Endpoints sensíveis devem ser considerados para rate limiting.


Principalmente:

login;

alteração de senha.


# 87. V1

Rate limiting pode ser implementado inicialmente de forma simples ou preparado para evolução.


# 88. Brute Force

Login deve possuir proteção contra tentativas excessivas.


# 89. Email

Mensagens de erro de login não devem revelar desnecessariamente se o email existe.


# 90. Exemplo

Evitar:

"Email não cadastrado."


Preferir:

"Credenciais inválidas."


# 91. Password Policy

A senha deve possuir requisitos mínimos de segurança.


# 92. Senha

Definir durante implementação:

tamanho mínimo;

caracteres permitidos;

política de alteração.


# 93. Complexidade

Não criar uma política excessivamente complexa sem justificativa.


# 94. UUID

IDs públicos devem utilizar UUID.


# 95. Enum

Enums devem ser validados no backend.


# 96. Mass Assignment

Não permitir que o cliente altere campos protegidos.


# 97. Exemplo

Cliente não pode alterar:

userId;

createdAt;

updatedAt;

status administrativo;


sem endpoint/regra específica.


# 98. DTO

Toda fronteira HTTP deve utilizar DTOs.

Não criar DTOs duplicados sem diferença real de contrato.


# 99. Entity

Não expor diretamente entidades do banco como contrato público da API.


# 100. Serialização

Responses devem utilizar DTOs.


# 101. Banco

Campos internos não devem ser expostos automaticamente.


# 102. Transações

Operações financeiras devem ser transacionais.


# 103. Concorrência

Operações envolvendo saldo devem considerar concorrência.


# 104. Transferência

Duas transferências simultâneas não devem conseguir gastar o mesmo saldo de forma inconsistente.


# 105. Pagamento

Dois pagamentos simultâneos não devem ultrapassar o valor devido.


# 106. Lock

Quando necessário, utilizar mecanismos de:

optimistic locking;

ou pessimistic locking.


# 107. V1

A estratégia de concorrência deve ser escolhida conforme o mecanismo de persistência adotado.


# 108. Idempotência

Operações financeiras críticas podem utilizar:

Idempotency-Key


# 109. Segurança de arquivos

Relatórios PDF gerados pelo sistema não devem permitir acesso a dados de outros usuários.


# 110. Exportação

Toda exportação deve validar:

usuário autenticado;

recurso solicitado;

permissão.


# 111. Fatura

Usuário A não pode exportar fatura do usuário B.


# 112. Busca

Busca textual não pode retornar registros de outro usuário.


# 113. Paginação

Paginação não deve permitir contornar isolamento de usuário.


# 114. Ordenação

Campos utilizados em ordenação devem ser controlados pelo backend.


# 115. Query parameters

Não permitir que parâmetros de consulta sejam transformados diretamente em SQL arbitrário.


# 116. Upload

A V1 não possui upload de arquivos.


# 117. Arquivos

Não implementar upload sem requisito explícito.


# 118. Relatórios

PDFs devem conter somente dados autorizados.


# 119. Dados pessoais

Evitar exposição desnecessária de:

email;

informações pessoais;

dados financeiros.


# 120. Princípio do menor privilégio

Cada componente deve possuir somente as permissões necessárias.


# 121. Docker

PostgreSQL não deve ficar exposto à internet.


# 122. Docker

Backend não deve possuir permissões desnecessárias no host.


# 123. Frontend

Frontend não deve acessar diretamente:

PostgreSQL;

Docker;

filesystem do servidor.


# 124. Arquitetura

Fluxo esperado:

Angular

↓

REST API

↓

Service

↓

Repository

↓

PostgreSQL


# 125. Regra

Frontend não acessa banco diretamente.


# 126. Segurança por camada

Segurança deve existir em:

API;

service;

persistência;

banco quando aplicável.


# 127. Regra

Não confiar somente na interface Angular.


# 128. Testes

Regras de segurança críticas devem possuir testes automatizados.


# 129. Teste

Deve existir teste garantindo:

usuário A não acessa despesa do usuário B.


# 130. Teste

Deve existir teste garantindo:

usuário A não altera conta do usuário B.


# 131. Teste

Deve existir teste garantindo:

usuário A não acessa fatura do usuário B.


# 132. Teste

Deve existir teste garantindo:

userId enviado pelo cliente é ignorado.


# 133. Teste

Deve existir teste garantindo:

endpoint protegido sem JWT retorna 401.


# 134. Segurança

Credenciais inválidas devem gerar:

401 Unauthorized


quando apropriado.


# 135. Autorização

Usuário autenticado sem acesso ao recurso deve receber resposta adequada.


# 136. CORS

Testar configuração de CORS no ambiente de desenvolvimento.


# 137. Headers

A API deve utilizar headers de segurança apropriados quando aplicável.


# 138. Spring

Se Spring Boot for utilizado, avaliar:

Security Headers;

CSRF;

CORS;

Session Policy;

Password Encoder.


# 139. Swagger

Swagger UI em desenvolvimento pode permitir exploração autenticada da API.


# 140. Swagger

Não expor documentação administrativa ou credenciais.


# 141. Swagger

Endpoints protegidos devem indicar autenticação no OpenAPI.


# 142. Secrets

Não colocar secrets no:

application.properties

application.yml


quando o projeto for versionado.


# 143. Configuração

Utilizar environment variables.


# 144. Exemplo

DATABASE_URL

DATABASE_USERNAME

DATABASE_PASSWORD

JWT_SECRET


# 145. .env

Arquivo:

.env


deve estar no:

.gitignore


# 146. Cursor

Arquivo:

.env


não deve ser enviado ao contexto da IA através do versionamento.


# 147. .cursorignore

Adicionar arquivos sensíveis ao:

.cursorignore


quando apropriado.


# 148. Git

Antes de commit:

verificar alterações.


# 149. GitHub

O repositório não deve conter:

credenciais;

tokens;

dados reais de usuários.


# 150. Dados reais

Desenvolvimento deve utilizar dados fictícios.


# 151. Produção futura

Dados reais não devem ser utilizados em testes automatizados.


# 152. Testes

Fixtures devem utilizar:

dados fictícios.


# 153. Banco de teste

Testes devem utilizar banco isolado.


# 154. Desenvolvimento

Não utilizar banco de produção para testes.


# 155. Migrations

Migrations não devem conter dados financeiros reais.


# 156. Seed

Seeds devem conter somente dados de exemplo.


# 157. Segurança

A aplicação deve seguir:

Secure by Default


# 158. Regra

Quando uma configuração de segurança possuir dúvida:

preferir comportamento mais restritivo.


# 159. Exceções

Exceções de segurança devem ser documentadas.


# 160. Atualizações

Bibliotecas de segurança devem ser monitoradas e atualizadas.


# 161. Dependabot

Pode ser considerado futuramente.


# 162. Scanner

Ferramentas de análise de dependências podem ser adicionadas futuramente.


# 163. OWASP

A aplicação deve seguir boas práticas do:

OWASP Top 10


# 164. Injeção

Proteger contra:

SQL Injection;

Command Injection;

Template Injection.


# 165. Autenticação

Proteger contra:

credential stuffing;

brute force;

session/token abuse.


# 166. Controle de acesso

Proteger contra:

IDOR;

privilege escalation;

broken access control.


# 167. Dados

Proteger dados financeiros contra exposição indevida.


# 168. Logging

Logs devem evitar informações sensíveis.


# 169. Erros

Erros devem ser seguros por padrão.


# 170. Configuração

Configurações inseguras não devem ser utilizadas em produção.


# 171. Regra final

Nenhuma funcionalidade financeira será considerada pronta se permitir acesso a dados de outro usuário.


# 172. Regra final

Segurança não deve ser adicionada somente no final do projeto.


# 173. Regra final

Toda nova funcionalidade deve responder:

1. Quem pode acessar?
2. Quem pode alterar?
3. Quem pode excluir/cancelar?
4. Quais dados pertencem ao usuário?
5. Existe risco de exposição?
6. Existe risco de duplicação financeira?
7. Existe risco de concorrência?


# 174. Regra final

Em caso de dúvida de segurança:

não assumir comportamento permissivo;

parar;

documentar a dúvida;

solicitar decisão.


# 175. Critério de aceitação

A V1 deve possuir:

- autenticação;
- autorização;
- isolamento por usuário;
- hash seguro de senha;
- validação de entrada;
- proteção contra SQL Injection;
- tratamento seguro de erros;
- secrets fora do Git;
- CORS configurado;
- testes de isolamento;
- documentação OpenAPI;
- proteção das operações financeiras.


# 176. Regra final

Segurança é responsabilidade do backend e da arquitetura como um todo, não apenas do frontend.