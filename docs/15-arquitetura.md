# Arquitetura e Tecnologias — Financial Control

## 1. Objetivo

Este documento define a arquitetura técnica da V1 do Financial Control.

O projeto deve ser moderno, didático, sustentável e preparado para evolução.

A arquitetura deve evitar complexidade desnecessária.

O projeto será desenvolvido inicialmente para execução local.


# 2. Arquitetura geral

O sistema será dividido em três aplicações principais:

1. Backend
2. Frontend
3. Banco de dados


Arquitetura:

Angular
    |
    | HTTP/REST
    v
Spring Boot
    |
    | JPA/Hibernate
    v
PostgreSQL


# 3. Backend

Tecnologia principal:

Java


Framework:

Spring Boot


# 4. Versão do Java

Utilizar uma versão LTS moderna do Java.


Preferência:

Java 21


Caso durante a implementação exista uma versão LTS mais adequada e compatível com todo o ecossistema:

a IA deve informar a diferença antes de alterar a decisão.


# 5. Framework backend

Utilizar:

Spring Boot


A versão deve ser a versão estável compatível com o Java escolhido.


Não utilizar versões snapshot.


# 6. Build

Utilizar:

Maven


O projeto deve possuir:

pom.xml


# 7. Estrutura do backend

Organizar por responsabilidade.


Estrutura inicial esperada:

backend/
|
+-- src/
|   |
|   +-- main/
|   |   |
|   |   +-- java/
|   |   |   |
|   |   |   +-- .../
|   |   |
|   |   +-- resources/
|   |
|   +-- test/
|
+-- pom.xml


A organização interna será definida de forma detalhada no documento de padrões.


# 8. Arquitetura interna

O backend deve utilizar arquitetura em camadas.


Camadas principais:

Controller
Service
Repository
Domain/Entity
DTO
Mapper


# 9. Controller

Responsável por:

- HTTP;
- autenticação/autorização;
- validação de entrada;
- resposta HTTP.


Controller não deve conter regra de negócio complexa.


# 10. Service

Responsável por:

- regras de negócio;
- orquestração;
- transações;
- validações de domínio.


# 11. Repository

Responsável por acesso ao banco.


Não colocar regra de negócio no Repository.


# 12. Entity

Representa o modelo persistido.


Não utilizar Entity diretamente como contrato da API.


# 13. DTO

A API deve utilizar DTOs.


Não expor diretamente entidades JPA nos endpoints.


# 14. Mapper

Conversões entre:

Entity

e:

DTO


devem ser explícitas.


# 15. MapStruct

Utilizar:

MapStruct


para mapeamentos quando isso simplificar o código.


Evitar mapeamentos manuais repetitivos.


# 16. Banco de dados

Banco:

PostgreSQL


Versão:

utilizar versão estável moderna.


A versão exata será fixada no Docker Compose.


# 17. ORM

Utilizar:

Spring Data JPA

+
Hibernate


# 18. JPA

Utilizar JPA como abstração de persistência.


# 19. Hibernate

Hibernate será o provider JPA.


# 20. Schema

O Hibernate não será responsável pela criação oficial do banco.


Não utilizar:

spring.jpa.hibernate.ddl-auto=create


ou:

update


como mecanismo de migration.


# 21. ddl-auto

Preferência:

validate


O schema oficial será controlado por migrations.


# 22. Migrations

Utilizar:

Flyway


# 23. Flyway

Todas as alterações estruturais do banco devem ocorrer por migrations.


Exemplo:

V1__create_users.sql

V2__create_accounts.sql

V3__create_categories.sql


# 24. Regra de migration

Depois que uma migration for aplicada em ambiente compartilhado:

não editar a migration antiga.


Criar uma nova migration.


# 25. PostgreSQL local

O PostgreSQL deve rodar em Docker.


Não exigir instalação nativa do PostgreSQL na máquina do desenvolvedor.


# 26. Docker

O projeto deve possuir:

Dockerfile

docker-compose.yml


ou:

compose.yaml


Preferência:

compose.yaml


# 27. Containers

A V1 deverá permitir executar:

PostgreSQL


O backend e frontend podem inicialmente ser executados diretamente pela IDE/terminal.


Posteriormente podemos containerizar tudo.


# 28. Desenvolvimento local

Fluxo inicial:

Docker
    |
    +-- PostgreSQL


Backend:

Maven / Spring Boot


Frontend:

Angular CLI


# 29. Docker Compose

O Compose deve permitir subir o banco com um comando.


Exemplo conceitual:

docker compose up -d


# 30. Variáveis de ambiente

Não colocar:

senhas;

tokens;

segredos;


diretamente no código.


Utilizar:

environment variables.


# 31. Arquivo .env

O projeto pode utilizar:

.env


para desenvolvimento local.


O arquivo contendo segredos reais não deve ser versionado.


Deve existir:

.env.example


# 32. Banco

Exemplo de variáveis:

DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD


# 33. Backend

Exemplo:

SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD


# 34. Configuração

Configurações específicas devem ser externalizadas.


Evitar hardcode.


# 35. Perfil de ambiente

Utilizar profiles do Spring.


Inicialmente:

local


Futuro:

test

production


# 36. API

A comunicação frontend/backend será REST.


# 37. Formato

Request:

JSON


Response:

JSON


# 38. Versionamento

A API deve ser versionada.


Preferência:

/api/v1


Exemplo:

/api/v1/expenses


# 39. REST

Os endpoints devem seguir princípios REST.


Exemplo:

GET /api/v1/expenses

POST /api/v1/expenses

GET /api/v1/expenses/{id}

PUT /api/v1/expenses/{id}

DELETE /api/v1/expenses/{id}


Quando uma operação for específica:

POST /api/v1/expenses/{id}/payments


# 40. Operações financeiras

Operações críticas devem possuir endpoints explícitos.


Exemplo:

POST /expenses/{id}/payments

POST /expenses/{id}/refunds

POST /credit-card-invoices/{id}/payments

POST /transfers


Não utilizar:

PUT


para operações que representam eventos financeiros.


# 41. HTTP Status

Utilizar corretamente:

200

201

204

400

401

403

404

409

422

500


# 42. Erros

A API deve possuir formato padronizado de erro.


Exemplo conceitual:

{
  "timestamp": "...",
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "Mensagem amigável",
  "path": "...",
  "errors": []
}


# 43. Tratamento de exceções

Utilizar:

@ControllerAdvice


ou mecanismo equivalente do Spring.


# 44. Validação

Utilizar:

Bean Validation


com:

Jakarta Validation


Exemplos:

@NotNull

@NotBlank

@Positive

@PositiveOrZero

@Email

@Size


# 45. Validação

A validação deve ocorrer:

frontend;

backend;

banco.


Cada camada possui objetivo diferente.


# 46. Autenticação

Utilizar:

Spring Security


# 47. Autenticação

A V1 utilizará:

JWT


# 48. Login

Endpoint:

POST /api/v1/auth/login


# 49. Registro

Endpoint:

POST /api/v1/auth/register


# 50. JWT

O token deve identificar o usuário autenticado.


# 51. User ID

O backend deve obter o user ID do contexto de segurança.


Não confiar no:

user_id


enviado pelo frontend.


# 52. Senha

Nunca armazenar senha em texto puro.


Utilizar algoritmo de hashing adequado.


Preferência:

BCrypt


ou algoritmo recomendado pelo Spring Security.


# 53. Expiração

JWT deve possuir expiração.


Não criar tokens eternos.


# 54. Refresh Token

A V1 pode inicialmente utilizar somente access token.


Refresh token pode ser implementado posteriormente.


# 55. CORS

Configurar CORS para permitir somente o frontend local conhecido.


Exemplo:

localhost:4200


Não utilizar:

Access-Control-Allow-Origin: *


como configuração definitiva.


# 56. Swagger

Utilizar:

OpenAPI


com integração ao Spring Boot.


Preferência:

springdoc-openapi


# 57. Swagger UI

Disponibilizar documentação navegável em ambiente local.


# 58. Documentação

Endpoints devem possuir:

- descrição;
- parâmetros;
- responses;
- códigos HTTP;
- exemplos quando necessário.


# 59. Frontend

Tecnologia:

Angular


# 60. Linguagem

TypeScript


# 61. Angular

Utilizar versão estável moderna.


Preferência:

Angular com standalone components.


Não utilizar NgModules sem necessidade.


# 62. Angular CLI

Utilizar Angular CLI.


# 63. Node.js

Utilizar versão LTS moderna compatível com Angular.


# 64. Package Manager

Preferência:

npm


Não utilizar npm e yarn simultaneamente.


# 65. TypeScript

Utilizar TypeScript com:

strict = true


# 66. Angular Signals

Utilizar Signals quando fizer sentido.


Não transformar tudo em Signals artificialmente.


# 67. RxJS

Utilizar RxJS quando apropriado para:

- HTTP;
- streams;
- operações assíncronas;
- eventos.


# 68. Estado

Não utilizar NgRx na V1.


Motivo:

a aplicação não possui complexidade suficiente para justificar isso inicialmente.


Angular Signals + services devem ser suficientes.


# 69. HTTP

Utilizar:

HttpClient


# 70. Interceptors

Utilizar interceptor para:

- JWT;
- tratamento de respostas;
- erros globais.


# 71. Guards

Utilizar route guards para áreas autenticadas.


# 72. Formulários

Utilizar:

Reactive Forms


# 73. Validação frontend

Utilizar validators do Angular.


A validação frontend não substitui backend.


# 74. UI

A V1 deve possuir interface moderna e responsiva.


# 75. Biblioteca de componentes

Utilizar uma biblioteca de componentes moderna.


Preferência inicial:

Angular Material


# 76. Angular Material

Utilizar somente quando agregar valor.


Não criar interface excessivamente dependente de componentes complexos.


# 77. CSS

Utilizar:

CSS


ou:

SCSS


Preferência:

SCSS


# 78. Estilos

Evitar estilos inline excessivos.


# 79. Responsividade

A aplicação deve funcionar adequadamente em:

desktop;

tablet;

mobile.


# 80. Tema

V1:

não implementar modo escuro.


# 81. Gráficos

Utilizar biblioteca compatível com Angular.


Preferência:

Apache ECharts


ou outra biblioteca moderna bem suportada.


A escolha definitiva deve considerar:

- compatibilidade;
- manutenção;
- documentação;
- simplicidade.


# 82. Gráficos

Gráficos devem ser componentes reutilizáveis.


# 83. Datas

Frontend deve tratar datas com cuidado para evitar problemas de timezone.


# 84. Valores monetários

Não utilizar floating point para lógica financeira no frontend.


Quando necessário:

representar valores monetários com precisão adequada.


# 85. Formatação monetária

Exibição:

pt-BR


Exemplo:

R$ 1.234,56


# 86. Backend e dinheiro

Backend deve utilizar:

BigDecimal


para valores financeiros.


# 87. Backend e datas

Utilizar:

LocalDate


para datas de negócio.


Utilizar:

Instant

ou:

OffsetDateTime


quando horário for necessário.


# 88. Timezone

Timezone da aplicação deve ser configurável.


Para o ambiente brasileiro:

America/Sao_Paulo


pode ser utilizado como configuração local inicial.


# 89. JSON

Datas e horários devem possuir formato consistente.


# 90. Banco

PostgreSQL deve utilizar:

NUMERIC


para valores financeiros.


# 91. UUID

Banco e backend devem utilizar UUID.


# 92. Logs

Utilizar logging estruturado do Spring.


Não utilizar:

System.out.println


no código de produção.


# 93. Logging

Nunca registrar:

senha;

JWT;

dados sensíveis.


# 94. Observabilidade

V1:

logs básicos.


Futuro:

metrics;

tracing;

monitoramento.


# 95. Testes backend

Testes automatizados são obrigatórios.


# 96. JUnit

Utilizar:

JUnit 5


# 97. Mockito

Utilizar Mockito quando necessário.


# 98. Testes de Service

As regras de negócio devem possuir testes unitários.


# 99. Testes de Controller

Endpoints principais devem possuir testes de integração.


# 100. Testes de Repository

Regras importantes de persistência devem ser testadas.


# 101. Testes de banco

Preferência:

Testcontainers


para executar PostgreSQL real durante testes de integração.


# 102. Testcontainers

Evitar utilizar banco falso para testar comportamento específico do PostgreSQL.


# 103. Testes frontend

Utilizar ferramentas oficiais/recomendadas pelo Angular.


A escolha deve considerar a versão do Angular adotada.


# 104. Testes frontend

Testar principalmente:

- componentes importantes;
- services;
- formulários;
- regras de apresentação;
- fluxos críticos.


# 105. Testes financeiros

Obrigatoriamente testar:

- parcelamento;
- arredondamento;
- pagamento parcial;
- pagamento integral;
- estorno;
- fatura;
- transferência;
- projeção;
- saldo.


# 106. Cobertura

Não estabelecer inicialmente uma meta artificial de 100%.


Priorizar cobertura das regras críticas.


# 107. Qualidade

Utilizar:

lint;

format;

testes.


# 108. ESLint

Frontend deve utilizar:

ESLint


# 109. Prettier

Frontend deve utilizar:

Prettier


# 110. Formatação

A formatação deve ser automatizada.


# 111. Backend formatting

Utilizar uma ferramenta de formatação consistente.


A ferramenta escolhida deve ser documentada.


# 112. Git

O projeto será versionado no GitHub do usuário.


# 113. Cursor

O desenvolvimento será realizado no Cursor.


# 114. VSCode

O Git será utilizado através do VSCode.


O Cursor não deve ser considerado responsável pelo versionamento.


# 115. GitHub

O projeto deve funcionar normalmente independentemente do editor utilizado.


# 116. IDE

Não criar configurações obrigatórias específicas do Cursor.


# 117. Dependência de IA

O código não deve depender de:

- Cursor;
- Copilot;
- API de IA;
- serviço externo de IA.


# 118. IA

A IA é uma ferramenta de desenvolvimento.


Não é parte da arquitetura do produto.


# 119. README

O projeto deve possuir documentação suficiente para que outra pessoa consiga:

- clonar;
- configurar;
- subir banco;
- iniciar backend;
- iniciar frontend;
- executar testes.


# 120. Scripts

Criar scripts/comandos documentados para:

instalação;

execução;

testes;

build.


# 121. Docker

Comando esperado:

docker compose up -d


para infraestrutura local.


# 122. Banco

O banco não deve depender de instalação manual do PostgreSQL.


# 123. Backend

O backend deve conseguir conectar ao PostgreSQL do Docker.


# 124. Frontend

O frontend deve conseguir consumir o backend local.


# 125. Portas

Definir portas fixas e documentadas.


Sugestão:

Angular:

4200


Spring Boot:

8080


PostgreSQL:

5432


# 126. Segurança de portas

PostgreSQL não precisa ser exposto publicamente.


A exposição local é suficiente.


# 127. Desenvolvimento

Fluxo esperado:

docker compose up -d

backend: mvn spring-boot:run

frontend: npm start


# 128. Build backend

O backend deve possuir build reproduzível.


# 129. Build frontend

O frontend deve possuir build de produção.


# 130. CI

V1:

não é obrigatório criar pipeline CI.


Mas o projeto deve estar preparado para isso.


# 131. CI futura

Possível pipeline:

lint
test
build


# 132. Dependências

Não adicionar biblioteca sem necessidade.


Antes de adicionar uma dependência:

1. verificar se a funcionalidade já existe no framework;
2. verificar manutenção;
3. verificar compatibilidade;
4. verificar segurança;
5. justificar a dependência.


# 133. Dependências frontend

Não instalar bibliotecas apenas por conveniência estética.


# 134. Dependências backend

Preferir recursos do Spring antes de adicionar bibliotecas externas.


# 135. Zod

Zod não será utilizado no backend Java.


# 136. Zod no frontend

A V1 não precisa utilizar Zod inicialmente.


Motivo:

Angular Reactive Forms + TypeScript + validação backend já fornecem estrutura suficiente.


Porém:

a arquitetura não deve impedir sua adoção futura.


# 137. TypeScript strict

O frontend deve utilizar:

strict


# 138. any

Evitar:

any


Não utilizar any como solução rápida para problemas de tipagem.


# 139. Null safety

Evitar:

valores indefinidos não tratados.


Utilizar recursos de TypeScript para representar estados corretamente.


# 140. API Contracts

Os contratos da API devem ser documentados pelo OpenAPI.


# 141. OpenAPI futuro

É possível gerar tipos/client automaticamente no frontend futuramente.


Não é obrigatório na primeira implementação.


# 142. Comunicação

Frontend e backend devem ser fracamente acoplados.


# 143. DTO

Mudanças internas nas entidades não devem quebrar automaticamente o frontend.


# 144. Banco e API

O modelo do banco não deve ser exposto diretamente pela API.


# 145. Segurança

Endpoints financeiros devem exigir autenticação.


# 146. Endpoint público

Somente:

registro;

login;

documentação local, se configurada;


podem ser públicos.


# 147. CORS

Configuração restrita ao ambiente local.


# 148. CSRF

A estratégia deve ser definida de acordo com o mecanismo de autenticação adotado.


# 149. Senhas

Não armazenar senha em logs.


# 150. Erros

Não expor stack trace ao frontend em produção.


# 151. Exceções de domínio

Criar exceções específicas quando necessário.


Exemplos:

ExpenseNotFoundException

InsufficientBalanceException

InvoiceAlreadyPaidException

InvalidInstallmentException


# 152. Regra financeira

As regras financeiras devem estar no backend.


Frontend apenas apresenta e solicita operações.


# 153. Transações

Utilizar:

@Transactional


nas operações que exigem atomicidade.


# 154. Service transacional

Preferir transações no Service.


# 155. Concorrência

Operações financeiras críticas devem considerar concorrência.


# 156. Lock

Utilizar locking somente quando necessário.


Não aplicar pessimistic locking indiscriminadamente.


# 157. Performance

Não otimizar prematuramente.


Primeiro:

correção;

depois:

performance.


# 158. Queries

Evitar:

N+1


# 159. Fetch

Não utilizar:

EAGER


indiscriminadamente.


Preferir:

LAZY


quando apropriado.


# 160. Paginação

Listagens potencialmente grandes devem utilizar paginação.


Exemplo:

despesas;

receitas;

transações;

faturas.


# 161. Ordenação

Listagens devem permitir ordenação quando fizer sentido.


# 162. Filtros

Listagens financeiras devem permitir filtros adequados.


Exemplos:

período;

categoria;

status;

cartão;

responsável.


# 163. Arquitetura não distribuída

V1:

monólito modular.


Não utilizar microserviços.


# 164. Monólito modular

Backend único.

Banco único.


Módulos organizados por domínio.


# 165. Domínios

Principais módulos:

auth

users

accounts

categories

incomes

expenses

credit_cards

invoices

installments

transfers

goals

reports

dashboard


# 166. Evolução

A arquitetura deve permitir adicionar novos módulos sem reorganização completa do projeto.


# 167. Não implementar investimentos

Investimentos serão módulo futuro.


# 168. Não implementar Open Banking

Open Banking será módulo futuro.


# 169. Não implementar notificações

Notificações serão módulo futuro.


# 170. Não implementar importação bancária

Importação será módulo futuro.


# 171. Não implementar recorrência

Recorrência automática será módulo futuro.


# 172. Não implementar cadastro de pessoas

Responsáveis continuam enum/controlados na V1.


# 173. Não implementar refinanciamento

A arquitetura deve permitir.

A funcionalidade será implementada posteriormente.


# 174. PDF

Relatórios PDF podem ser implementados no backend.


A biblioteca deve ser escolhida posteriormente com foco em:

- estabilidade;
- licença;
- compatibilidade Java;
- facilidade de manutenção.


# 175. Exportação

O endpoint de relatório deve permitir gerar o arquivo sem expor dados de outros usuários.


# 176. Nome do projeto

Nome:

Financial Control


Backend:

financial-control-api


Frontend:

financial-control-web


# 177. Estrutura raiz

Sugestão:

financial-control/
|
+-- backend/
|
+-- frontend/
|
+-- database/
|
+-- docs/
|
+-- docker/
|
+-- compose.yaml
|
+-- README.md
|
+-- AGENTS.md
|
+-- .gitignore
|
+-- .cursorignore


# 178. Database

Scripts SQL oficiais devem permanecer organizados no backend através do Flyway.


A pasta database pode ser utilizada para scripts auxiliares, documentação ou ferramentas.


# 179. Docker

Arquivos de infraestrutura devem ficar organizados e documentados.


# 180. Ambiente local

O projeto deve ser executável sem depender de serviços pagos.


# 181. Internet

O sistema deve funcionar localmente após instalação das dependências.


# 182. Dependências externas

Não depender de APIs externas para funcionalidades principais da V1.


# 183. Frontend offline

Depois de iniciado localmente:

frontend -> backend -> PostgreSQL


Não depender de serviços externos.


# 184. API documentation

Swagger/OpenAPI deve refletir o comportamento real da API.


# 185. Contratos

Alterações breaking na API devem ser discutidas antes de executadas.


# 186. Breaking changes

Não alterar silenciosamente:

nome de endpoint;

estrutura de resposta;

campo obrigatório;

semântica financeira.


# 187. Código gerado

Código gerado automaticamente deve ser identificado.


Não editar manualmente código gerado sem necessidade.


# 188. Comentários

Não comentar código óbvio.


Comentários devem explicar:

por quê.


Não apenas:

o quê.


# 189. Clean Code

Priorizar:

nomes claros;

métodos pequenos;

responsabilidades únicas;

baixo acoplamento.


# 190. SOLID

Aplicar princípios SOLID de forma pragmática.


Não criar abstrações apenas para demonstrar SOLID.


# 191. Design Patterns

Utilizar patterns quando resolverem problemas reais.


Não aplicar patterns por obrigação.


# 192. DRY

Evitar duplicação.


Mas não criar abstrações prematuras.


# 193. KISS

Preferir soluções simples quando resolverem corretamente o problema.


# 194. YAGNI

Não implementar funcionalidades que ainda não foram solicitadas.


# 195. IA

A IA deve explicar decisões arquiteturais relevantes.


# 196. IA

Quando existir mais de uma opção tecnicamente válida:

apresentar a opção escolhida;

explicar o motivo;

informar alternativas.


# 197. IA

Não trocar tecnologias previamente definidas sem autorização.


# 198. IA

Não instalar dependências automaticamente sem necessidade clara.


# 199. IA

Não modificar arquivos de configuração críticos sem explicar o motivo.


# 200. Regra final

A arquitetura da V1 deve seguir:

Java
+
Spring Boot
+
Spring Security
+
JWT
+
Spring Data JPA
+
Hibernate
+
PostgreSQL
+
Flyway
+
OpenAPI
+
JUnit
+
Mockito
+
Testcontainers
+
Angular
+
TypeScript
+
Angular Material
+
RxJS
+
Signals
+
Reactive Forms
+
ESLint
+
Prettier
+
Docker


A solução deve permanecer:

simples;

segura;

testável;

didática;

modular;

extensível;

e adequada para um projeto pessoal financeiro real.