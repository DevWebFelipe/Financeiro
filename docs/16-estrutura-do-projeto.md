# Estrutura do Projeto — Financial Control

## 1. Objetivo

Este documento define a estrutura física do projeto Financial Control.

A estrutura deve permanecer previsível durante todo o desenvolvimento.

A IA deve respeitar esta organização e não criar novas estruturas sem necessidade.


# 2. Estrutura da raiz

A raiz do projeto deve possuir:

financial-control/
|
+-- backend/
|
+-- frontend/
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
|
+-- .env.example


# 3. Backend

Estrutura:

backend/
|
+-- pom.xml
|
+-- src/
    |
    +-- main/
    |   |
    |   +-- java/
    |   |
    |   +-- resources/
    |
    +-- test/


# 4. Package principal

O package principal deve utilizar um namespace consistente.

Sugestão:

br.com.financialcontrol


A estrutura final deverá ser:

br.com.financialcontrol


Não utilizar nomes genéricos como:

com.example


# 5. Organização backend

Dentro do package principal:

br.com.financialcontrol
|
+-- FinancialControlApplication
|
+-- config
|
+-- common
|
+-- security
|
+-- auth
|
+-- user
|
+-- account
|
+-- category
|
+-- income
|
+-- expense
|
+-- creditcard
|
+-- invoice
|
+-- installment
|
+-- transfer
|
+-- goal
|
+-- dashboard
|
+-- report


# 6. Organização por domínio

Cada módulo deve possuir suas próprias responsabilidades.


Exemplo:

expense/
|
+-- controller/
|
+-- service/
|
+-- repository/
|
+-- entity/
|
+-- dto/
|
+-- mapper/
|
+-- exception/


# 7. Não utilizar arquitetura totalmente horizontal

Evitar estrutura:

controller/
service/
repository/
entity/


com todos os módulos misturados.


Preferir organização por domínio.


# 8. Exemplo de módulo

expense/


Deve conter:

expense/
|
+-- controller/
|   |
|   +-- ExpenseController
|
+-- service/
|   |
|   +-- ExpenseService
|
+-- repository/
|   |
|   +-- ExpenseRepository
|
+-- entity/
|   |
|   +-- Expense
|
+-- dto/
|   |
|   +-- CreateExpenseRequest
|   +-- UpdateExpenseRequest
|   +-- ExpenseResponse
|
+-- mapper/
|   |
|   +-- ExpenseMapper
|
+-- exception/
|   |
|   +-- ExpenseNotFoundException


# 9. Controller

Controllers devem ficar dentro do módulo correspondente.


Exemplo:

expense/controller/ExpenseController


# 10. Service

Services devem ficar dentro do módulo correspondente.


Exemplo:

expense/service/ExpenseService


# 11. Repository

Repositories devem ficar dentro do módulo correspondente.


Exemplo:

expense/repository/ExpenseRepository


# 12. Entity

Entities devem ficar dentro do módulo correspondente.


Exemplo:

expense/entity/Expense


# 13. DTO

DTOs devem ficar dentro do módulo correspondente.


# 14. Mapper

Mappers devem ficar dentro do módulo correspondente.


# 15. Exceptions

Exceções específicas do domínio devem ficar dentro do módulo.


# 16. Common

A pasta:

common/


deve conter somente componentes realmente compartilhados.


Exemplos:

- tratamento global de exceções;
- classes utilitárias realmente genéricas;
- paginação;
- respostas comuns;
- constantes globais quando necessárias.


# 17. Não transformar common em depósito

Não colocar em:

common/


classes que pertencem claramente a um domínio.


# 18. Config

A pasta:

config/


deve conter configurações da aplicação.


Exemplos:

- OpenAPI;
- CORS;
- Jackson;
- beans;
- configurações gerais.


# 19. Security

A pasta:

security/


deve conter:

- JWT;
- filtros;
- UserDetails;
- SecurityConfig;
- componentes de autenticação/autorização.


# 20. Auth

A pasta:

auth/


deve conter o fluxo de:

- login;
- registro;
- autenticação.


# 21. User

A pasta:

user/


deve conter:

- entidade do usuário;
- repository;
- serviços;
- DTOs;
- endpoints relacionados ao usuário.


# 22. Account

A pasta:

account/


deve conter tudo relacionado às contas financeiras.


Exemplos:

- conta corrente;
- poupança;
- carteira.


# 23. Category

A pasta:

category/


deve conter categorias de receitas e despesas.


# 24. Income

A pasta:

income/


deve conter receitas.


# 25. Expense

A pasta:

expense/


deve conter despesas.


# 26. Credit Card

O domínio de cartões utilizará:

creditcard/


e não:

credit_card/


dentro dos packages Java.


Motivo:

convenção de nomes Java.


# 27. Invoice

O domínio:

invoice/


representa faturas de cartão.


# 28. Installment

O domínio:

installment/


representa parcelamentos e parcelas.


# 29. Transfer

O domínio:

transfer/


representa transferências entre contas.


# 30. Goal

O domínio:

goal/


representa metas financeiras.


# 31. Dashboard

O domínio:

dashboard/


será responsável pelas informações agregadas para a tela inicial.


# 32. Report

O domínio:

report/


será responsável por relatórios e exportações.


# 33. Resources

Estrutura:

backend/src/main/resources/


deve possuir:

resources/
|
+-- application.yml
|
+-- application-local.yml
|
+-- application-test.yml
|
+-- db/
|   |
|   +-- migration/
|
+-- static/
|
+-- templates/


# 34. Application configuration

Preferência:

application.yml


e profiles específicos.


# 35. Secrets

Não armazenar secrets reais em:

application.yml


# 36. Migration

Flyway:

src/main/resources/db/migration/


# 37. Naming migrations

Formato:

V1__descricao.sql


Exemplo:

V1__create_users.sql


# 38. Migration

Não criar arquivos como:

migration.sql


sem versionamento.


# 39. Testes

Estrutura:

src/test/java/


deve acompanhar a estrutura dos módulos.


# 40. Exemplo de teste

expense/


deve possuir testes correspondentes ao domínio:

ExpenseServiceTest

ExpenseControllerTest

ExpenseRepositoryTest


quando aplicável.


# 41. Testes unitários

Testes unitários devem ser próximos conceitualmente ao módulo testado.


# 42. Testes de integração

Testes de integração devem possuir identificação clara.


Exemplo:

ExpenseIntegrationTest


# 43. Testcontainers

Testes que dependem do PostgreSQL real devem utilizar Testcontainers.


# 44. Frontend

Estrutura:

frontend/
|
+-- package.json
|
+-- angular.json
|
+-- tsconfig.json
|
+-- src/
    |
    +-- app/
    |
    +-- assets/
    |
    +-- environments/


# 45. Angular

Utilizar standalone components.


# 46. Estrutura Angular

Sugestão:

app/
|
+-- core/
|
+-- shared/
|
+-- features/
|
+-- layout/


# 47. Core

core/


contém funcionalidades globais da aplicação.


Exemplos:

- autenticação;
- interceptors;
- guards;
- serviços globais;
- configuração HTTP.


# 48. Core

Não colocar componentes específicos de funcionalidades financeiras em:

core/


# 49. Shared

shared/


contém elementos reutilizáveis.


Exemplos:

- componentes;
- pipes;
- directives;
- tipos;
- utilidades.


# 50. Shared

Não colocar regras específicas de:

despesas;

cartões;

receitas;


em:

shared/


# 51. Features

features/


contém os módulos funcionais da aplicação.


Estrutura:

features/
|
+-- auth/
|
+-- dashboard/
|
+-- accounts/
|
+-- categories/
|
+-- incomes/
|
+-- expenses/
|
+-- credit-cards/
|
+-- invoices/
|
+-- transfers/
|
+-- goals/
|
+-- reports/


# 52. Feature

Cada feature deve possuir seus próprios componentes e serviços.


# 53. Exemplo feature expenses

expenses/
|
+-- pages/
|
+-- components/
|
+-- services/
|
+-- models/
|
+-- expense.routes.ts


# 54. Pages

pages/


contém componentes associados a páginas/rotas.


Exemplos:

expense-list-page

expense-create-page

expense-detail-page


# 55. Components

components/


contém componentes específicos da feature.


Exemplos:

expense-form

expense-table

expense-filters


# 56. Services

services/


contém serviços HTTP e lógica específica da feature.


# 57. Models

models/


contém:

interfaces;

types;

DTOs do frontend.


# 58. Routes

Cada feature pode possuir:

feature.routes.ts


# 59. Lazy Loading

Features principais devem utilizar lazy loading quando fizer sentido.


# 60. Dashboard

Dashboard deve ser uma feature independente.


# 61. Auth

Auth deve possuir:

login;

registro.


# 62. Layout

layout/


pode conter:

- header;
- sidebar;
- navigation;
- shell da aplicação.


# 63. Assets

assets/


contém:

- imagens;
- ícones;
- recursos estáticos.


# 64. Environments

Utilizar:

environment.ts

environment.development.ts


ou estrutura compatível com a versão do Angular utilizada.


# 65. URLs

URL da API deve ser configurável por environment.


Exemplo conceitual:

apiUrl:


Não hardcodar:

http://localhost:8080


em múltiplos arquivos.


# 66. CSS

Estilos globais devem ficar na estrutura definida pelo Angular.


# 67. Component styles

Componentes devem preferir estilos locais.


# 68. Componentes

Preferir componentes pequenos e reutilizáveis.


# 69. Páginas

Páginas podem orquestrar múltiplos componentes.


# 70. Estado

Services + Signals devem ser utilizados para estado compartilhado quando necessário.


# 71. Estado global

Evitar estado global desnecessário.


# 72. Backend API client

Services Angular devem ser responsáveis pela comunicação HTTP.


# 73. Não chamar HTTP diretamente em componentes

Componentes devem utilizar services.


# 74. Tipos

Interfaces de resposta da API devem ser definidas no frontend.


# 75. Contratos

Os contratos devem acompanhar a documentação OpenAPI.


# 76. Docker

Estrutura:

docker/
|
+-- postgres/


ou arquivos de infraestrutura diretamente na raiz quando isso simplificar.


# 77. Compose

Preferência:

compose.yaml


na raiz.


# 78. Dockerfile backend

Pode ficar:

backend/Dockerfile


# 79. Dockerfile frontend

Pode ficar:

frontend/Dockerfile


Não é obrigatório utilizar ambos na primeira etapa.


# 80. PostgreSQL

Configuração do PostgreSQL deve ficar no:

compose.yaml


# 81. Volumes

O PostgreSQL deve utilizar volume persistente local.


Exemplo conceitual:

postgres_data


# 82. Banco

Ao executar:

docker compose down


o banco não deve ser apagado automaticamente.


# 83. Reset do banco

Deve existir documentação para resetar o banco.


# 84. README

O README deve explicar:

- objetivo;
- tecnologias;
- requisitos;
- instalação;
- execução;
- testes;
- documentação da API.


# 85. Docs

A pasta:

docs/


deve conter a documentação técnica e funcional do projeto.


# 86. Organização docs

Sugestão:

docs/
|
+-- 01-visao-do-projeto.md
|
+-- 02-requisitos.md
|
+-- 03-escopo-v1.md
|
+-- 04-decisoes.md
|
+-- 05-fluxos.md
|
+-- 06-telas.md
|
+-- 07-api.md
|
+-- 08-seguranca.md
|
+-- 09-testes.md
|
+-- 10-docker.md
|
+-- 11-git.md
|
+-- 12-glossario.md
|
+-- 13-modelo-de-dados.md
|
+-- 14-regras-de-negocio.md
|
+-- 15-arquitetura.md
|
+-- 16-estrutura-do-projeto.md


# 87. Documentação

Documentos devem possuir numeração para facilitar leitura e referência.


# 88. Atualização

Quando uma decisão estrutural mudar:

atualizar documentação correspondente.


# 89. AGENTS.md

AGENTS.md é o documento de instruções principal para a IA.


Não duplicar regras contraditórias nos demais documentos.


# 90. Cursor

O Cursor deve conseguir compreender o projeto somente lendo:

AGENTS.md

e:

docs/


# 91. .cursorignore

Arquivos desnecessários para o contexto da IA devem ser ignorados.


# 92. Git

O Git deve versionar:

código;

documentação;

configurações;

migrations;

arquivos de exemplo.


# 93. Git

Não versionar:

secrets;

logs;

builds;

node_modules;

target;

dados locais do banco.


# 94. Frontend build

Não versionar:

dist/


# 95. Backend build

Não versionar:

target/


# 96. Node

Não versionar:

node_modules/


# 97. IDE

Arquivos específicos de IDE devem ser ignorados quando não forem necessários.


Exemplos:

.idea/

.vscode/


A decisão sobre .vscode será feita conforme necessidade do projeto.


# 98. Sistema operacional

Não versionar:

.DS_Store

Thumbs.db


# 99. Logs

Não versionar:

*.log


# 100. Environment

Não versionar:

.env


Versionar:

.env.example


# 101. Segurança

Nunca versionar:

private keys;

JWT secrets;

senhas;

tokens;

credenciais.


# 102. Arquivos temporários

Não versionar:

.tmp

.cache

arquivos temporários.


# 103. Dados financeiros reais

Não versionar dados financeiros pessoais reais.


# 104. Banco local

Não versionar:

arquivos físicos do PostgreSQL;

dumps contendo dados pessoais;

backups locais.


# 105. Testes

Dados fictícios de testes podem ser versionados.


# 106. Seed

Seeds de desenvolvimento devem utilizar dados fictícios.


# 107. Dados sensíveis

Não utilizar informações financeiras reais em:

testes;

fixtures;

documentação;

screenshots.


# 108. Nomes de arquivos

Frontend:

kebab-case


Exemplo:

expense-list.component.ts


Backend:

PascalCase para classes Java.


# 109. Angular

Componentes devem seguir convenções modernas do Angular.


# 110. Angular filenames

Exemplo:

expense-list-page.ts


ou convenção gerada pela versão do Angular utilizada.


Não misturar estilos de nomenclatura.


# 111. Backend package

Packages Java devem utilizar:

lowercase


# 112. Backend classes

Classes:

PascalCase


# 113. Backend methods

Métodos:

camelCase


# 114. Backend constants

Constantes:

UPPER_SNAKE_CASE


# 115. TypeScript

Variáveis:

camelCase


# 116. Interfaces

Interfaces devem possuir nomes claros.


Não adicionar:

I


automaticamente.


Preferir:

Expense


em vez de:

IExpense


# 117. Types

Utilizar:

type


quando representar composição/unions.


Utilizar:

interface


quando representar contratos extensíveis.


# 118. Enums

No frontend:

preferir union types quando um enum real não for necessário.


Exemplo:

type ExpenseStatus =
  | 'PENDING'
  | 'PAID'


# 119. Backend enums

Java enums podem ser utilizados quando representarem estados reais do domínio.


# 120. JSON enums

API deve utilizar valores consistentes.


Exemplo:

"PENDING"


# 121. Banco enum

Evitar PostgreSQL ENUM na V1.


Preferir:

VARCHAR

+
constraint

ou:

tabela de domínio


conforme necessidade.


# 122. Motivo

Alterações de PostgreSQL ENUM podem tornar migrations mais complexas.


# 123. Arquivos grandes

Evitar arquivos excessivamente grandes.


# 124. Componentes grandes

Se um componente Angular ultrapassar significativamente uma responsabilidade:

avaliar divisão.


# 125. Services grandes

Se um Service Java acumular muitos domínios:

avaliar separação.


# 126. Regra de domínio

Não criar um:

FinancialService


gigante.


Preferir:

ExpenseService

InvoiceService

TransferService


etc.


# 127. Dashboard

Dashboard pode consumir serviços especializados.


Não duplicar regras financeiras no DashboardService.


# 128. Relatórios

Reports devem reutilizar regras e consultas do domínio quando possível.


# 129. Queries complexas

Consultas complexas podem possuir:

Repository custom;

Specification;

QueryDSL;


ou tecnologia equivalente.


A escolha deve ser baseada na necessidade real.


# 130. QueryDSL

Não adicionar inicialmente sem necessidade.


# 131. Specifications

Spring Data Specifications podem ser utilizadas para filtros dinâmicos.


# 132. Paginação

Preferir:

Pageable


do Spring Data.


# 133. API pagination

Resposta paginada deve possuir estrutura consistente.


# 134. Frontend pagination

Componentes devem consumir a paginação do backend.


# 135. Ordenação

Ordenação enviada pelo frontend deve ser validada pelo backend.


Não concatenar SQL diretamente com entrada do usuário.


# 136. Segurança SQL

Nunca montar SQL com concatenação de strings contendo entrada do usuário.


# 137. Repository

Preferir queries parametrizadas.


# 138. Native SQL

Native SQL pode ser utilizado quando houver justificativa.


Não utilizar por padrão.


# 139. Performance

Otimizações devem ser justificadas por problema real.


# 140. Transações

Toda operação que alterar múltiplas tabelas deve possuir transação apropriada.


# 141. Atomicidade

Exemplo:

criar despesa parcelada


deve realizar:

despesa
+
plano
+
parcelas


na mesma transação.


# 142. Atomicidade

Pagamento de fatura deve atualizar:

pagamento
+
movimentação financeira


atomicamente.


# 143. Atomicidade

Transferência deve atualizar:

origem
+
destino
+
movimentações


atomicamente.


# 144. Erro parcial

Nunca deixar:

transferência debitada

sem crédito correspondente.


# 145. Integridade

O banco deve possuir foreign keys.


# 146. Cascades

Não utilizar cascades destrutivos em histórico financeiro.


# 147. Orphan removal

Evitar:

orphanRemoval = true


em entidades financeiras sem análise cuidadosa.


# 148. Lazy loading

Evitar retornar entidades JPA diretamente.


# 149. DTO

DTOs devem controlar exatamente o que a API expõe.


# 150. API Response

Não retornar:

password_hash;

segredos;

informações internas;


# 151. Paginação

Listagens devem possuir limites razoáveis.


Não permitir:

pageSize = 1.000.000


# 152. Uploads

V1:

não possuir upload de arquivos.


# 153. Relatórios

PDFs devem ser gerados sob demanda.


# 154. Arquivos temporários

Arquivos gerados não devem ser persistidos permanentemente sem necessidade.


# 155. Timezone

Frontend e backend devem possuir tratamento consistente de datas.


# 156. Datas

Evitar conversões implícitas entre:

DATE

e:

DATETIME


# 157. Money

Valores monetários nunca devem ser tratados como texto para cálculos.


# 158. Frontend money

Input monetário deve permitir:

vírgula decimal no formato brasileiro.


A conversão para API deve ser controlada.


# 159. Backend money

BigDecimal.


# 160. PostgreSQL money

Não utilizar tipo:

MONEY


do PostgreSQL.


Preferir:

NUMERIC(15,2)


# 161. IDs

UUID deve ser enviado como string no JSON.


# 162. UUID

Frontend não deve tentar gerar IDs para entidades persistidas, salvo necessidade explícita.


Backend/database são responsáveis pela identidade.


# 163. REST resource IDs

IDs devem aparecer na URL quando necessário.


# 164. Soft delete

Quando houver necessidade de manter histórico:

preferir status/active/deleted_at.


# 165. Exclusão

DELETE não deve ser utilizado para operações financeiras históricas sem regra explícita.


# 166. Cancelamento

Preferir endpoint específico quando representar evento de negócio.


Exemplo:

POST /expenses/{id}/cancel


# 167. Estorno

Preferir:

POST /expenses/{id}/refunds


# 168. Pagamento

Preferir:

POST /expenses/{id}/payments


# 169. Transferência

Preferir:

POST /transfers


# 170. Fatura

Pagamento:

POST /credit-card-invoices/{id}/payments


# 171. Arquitetura

O frontend nunca deve implementar regras financeiras críticas por conta própria.


# 172. Backend

Backend deve ser fonte de verdade.


# 173. Banco

Banco deve garantir integridade estrutural.


# 174. Frontend

Frontend deve garantir boa experiência.


# 175. Testes

Testes devem garantir que as regras permaneçam corretas.


# 176. Documentação

Documentação deve explicar decisões não óbvias.


# 177. IA

Antes de criar uma nova pasta:

verificar se existe uma pasta apropriada.


# 178. IA

Antes de criar uma nova biblioteca:

verificar se já existe solução no projeto.


# 179. IA

Antes de alterar arquitetura:

consultar AGENTS.md e docs.


# 180. IA

Não criar arquivos duplicados com nomes semelhantes.


Exemplo proibido:

ExpenseService

ExpenseServiceImpl

ExpenseBusinessService

ExpenseManager


sem justificativa arquitetural.


# 181. IA

Preferir simplicidade.


# 182. IA

Não gerar código especulativo para funcionalidades futuras.


# 183. IA

Não criar endpoints que não tenham requisito.


# 184. IA

Não criar tabelas sem necessidade.


# 185. IA

Não criar abstrações para "futuro" sem justificativa.


# 186. Desenvolvimento incremental

Cada etapa deve:

1. alterar o mínimo necessário;
2. compilar;
3. testar;
4. documentar;
5. somente então avançar.


# 187. Critério de conclusão

Uma etapa só está concluída quando:

- código compila;
- testes passam;
- migrations funcionam;
- aplicação inicia;
- documentação está atualizada.


# 188. Commit

O usuário realizará commits pelo VSCode.


A IA não deve presumir acesso ao GitHub.


# 189. Git

A IA pode preparar arquivos.


O usuário realizará:

git add

git commit

git push


# 190. GitHub

Não armazenar credenciais do GitHub no projeto.


# 191. Cursor

O Cursor é somente ambiente de desenvolvimento.


# 192. VSCode

O VSCode é o ambiente utilizado para versionamento Git.


# 193. Projeto local

A aplicação deve funcionar integralmente na máquina local.


# 194. Empresa

O desenvolvimento inicial poderá ocorrer na máquina da empresa utilizando Cursor.


# 195. Casa

O projeto deve continuar funcionando em outra máquina após clonar o repositório.


# 196. Reprodutibilidade

Uma máquina nova deve conseguir configurar o projeto seguindo o README.


# 197. Dependências do sistema

O objetivo é reduzir dependências instaladas manualmente.


Docker deverá fornecer:

PostgreSQL.


Java e Node podem ser instalados localmente inicialmente.


# 198. Futuro

É possível posteriormente executar tudo via Docker.


# 199. Não fazer agora

Não implementar:

Kubernetes;

microserviços;

service mesh;

cloud;

CI/CD complexo;

monitoramento distribuído.


# 200. Regra final

A estrutura do projeto deve favorecer:

clareza;

aprendizado;

manutenção;

testabilidade;

evolução.


A IA deve respeitar a estrutura existente antes de propor novas abstrações.