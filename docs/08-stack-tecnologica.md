# Stack Tecnológica — Financial Control

## 1. Objetivo

Este documento define as tecnologias oficiais utilizadas no projeto Financial Control.

A escolha das tecnologias deve considerar:

- estabilidade;
- manutenção;
- mercado;
- documentação;
- comunidade;
- segurança;
- facilidade de aprendizado;
- compatibilidade entre tecnologias;
- adequação ao projeto.

Não adicionar bibliotecas ou frameworks desnecessários apenas porque são populares.

Toda nova dependência deve possuir justificativa.


# 2. Arquitetura geral

O projeto será dividido em:

financial-control/
    backend/
    frontend/
    database/
    docs/


Arquitetura:

Angular
    |
    | HTTP/JSON
    v
Spring Boot
    |
    | JPA/Hibernate
    v
PostgreSQL


Docker será utilizado para infraestrutura local.


# 3. Backend

## 3.1 Linguagem

Java.

A versão deve ser uma versão LTS moderna.

Preferência inicial:

Java 21 LTS


Caso uma versão LTS mais recente esteja amplamente suportada por todo o ecossistema no momento da implementação, a IA poderá propor atualização.

Não alterar a versão sem justificar.


# 4. Framework backend

Utilizar:

Spring Boot


O Spring Boot será responsável por:

- inicialização da aplicação;
- configuração;
- REST;
- injeção de dependência;
- integração com banco;
- validação;
- segurança;
- observabilidade básica.


# 5. Spring Web

Utilizar:

Spring Web


Responsável pelos controllers REST.


# 6. Spring Data JPA

Utilizar:

Spring Data JPA


Responsável pela camada de persistência.


# 7. Hibernate

Utilizar:

Hibernate como implementação JPA padrão do Spring Boot.


# 8. Banco de dados

Utilizar:

PostgreSQL


O banco será executado inicialmente através do Docker.


# 9. Driver PostgreSQL

Utilizar o driver oficial JDBC do PostgreSQL compatível com a versão do Java/Spring Boot adotada.


# 10. Migrations

Utilizar:

Flyway


As alterações estruturais do banco devem ser controladas através de migrations.


# 11. Regra de migrations

Não utilizar:

spring.jpa.hibernate.ddl-auto=create

ou estratégias equivalentes para controlar o schema oficial.


Preferir:

spring.jpa.hibernate.ddl-auto=validate


A estrutura oficial do banco deve ser criada pelas migrations.


# 12. Estrutura das migrations

Exemplo:

V1__create_users.sql
V2__create_accounts.sql
V3__create_categories.sql


As migrations devem possuir nomes claros.


# 13. IDs

Entidades utilizarão:

UUID


O UUID será utilizado como identificador público e interno das entidades.


# 14. Dinheiro

Valores monetários no backend devem utilizar:

BigDecimal


Nunca utilizar:

double

float


para representar valores financeiros.


# 15. Datas

Utilizar as classes modernas do Java:

LocalDate
LocalDateTime
Instant
OffsetDateTime

conforme o significado do dado.


# 16. Regra de data

Para valores que representam apenas data:

LocalDate


Exemplo:

data de vencimento.


Para eventos que realmente exigem instante:

Instant


Exemplo:

createdAt.


# 17. Timezone

O backend deverá possuir timezone configurável.

Preferência:

UTC para timestamps técnicos.


Datas financeiras devem ser tratadas considerando o contexto local da aplicação.


# 18. Lombok

A utilização de Lombok é permitida.

Entretanto, não utilizar Lombok indiscriminadamente.

Se utilizado, priorizar anotações simples como:

@Getter
@Setter
@Builder
NoArgsConstructor
AllArgsConstructor


Evitar excesso de geração automática que dificulte entendimento para fins de aprendizado.


# 19. MapStruct

Utilizar:

MapStruct


para conversão entre:

Entity
DTO


quando a quantidade de mapeamentos justificar a dependência.


Não realizar mapeamentos complexos automaticamente sem necessidade.


# 20. Validação

Utilizar:

Jakarta Bean Validation


Exemplo:

@NotNull
@NotBlank
@Positive
@Email
@Size


As validações devem existir nos DTOs de entrada.


# 21. Regra de validação

Validação de formato deve ocorrer no DTO.

Validação de regra de negócio deve ocorrer na camada de serviço.


Exemplo:

DTO:

amount > 0


Service:

cartão possui limite suficiente


# 22. Segurança

Utilizar:

Spring Security


# 23. Autenticação

A primeira versão utilizará:

JWT


O token será utilizado para autenticar requisições.


# 24. Senhas

Senhas nunca devem ser armazenadas em texto puro.

Utilizar algoritmo de hash adequado.

Preferência:

BCrypt


ou algoritmo recomendado pelo Spring Security na versão adotada.


# 25. Autorização

A V1 terá inicialmente:

usuário autenticado

e isolamento por usuário.


A arquitetura deve permitir futuramente adicionar:

roles
permissions


# 26. API REST

A API será REST.

Controllers devem possuir responsabilidade limitada.

Exemplo:

ExpenseController

deve delegar regras para:

ExpenseService


# 27. Service Layer

Regras de negócio devem ficar principalmente nos Services.

Não colocar regras financeiras complexas nos Controllers.


# 28. Repository

Persistência deve ser abstraída através de Repositories.

Exemplo:

ExpenseRepository


# 29. Entities

Entities JPA representam o modelo de persistência.

Não utilizar Entity diretamente como response da API.


# 30. DTOs

Utilizar DTOs.

Categorias:

Request DTO
Response DTO


Exemplo:

CreateExpenseRequest

ExpenseResponse


# 31. Exceções

Criar exceções de domínio quando necessário.

Exemplos:

ExpenseNotFoundException
InsufficientCreditLimitException
InvoiceAlreadyClosedException


Não criar uma exceção específica para cada situação trivial.


# 32. Tratamento global de exceções

Utilizar:

@RestControllerAdvice


para padronizar respostas de erro.


# 33. Logs

Utilizar o sistema de logging padrão do Spring Boot.

Não utilizar:

System.out.println


para logging da aplicação.


# 34. Logging

Utilizar níveis apropriados:

ERROR
WARN
INFO
DEBUG


Não registrar:

senhas
tokens
dados financeiros desnecessários


# 35. OpenAPI

Utilizar:

springdoc-openapi


para documentação da API.


# 36. Swagger UI

Disponibilizar Swagger UI durante desenvolvimento.

Exemplo conceitual:

/swagger-ui/index.html


A URL exata depende da configuração.


# 37. Testes backend

Utilizar:

JUnit 5


# 38. Testes unitários

Utilizar:

JUnit 5
Mockito


quando mock seja realmente necessário.


# 39. Testes de integração

Utilizar:

Spring Boot Test


# 40. Banco em testes

Preferência:

Testcontainers


para testes que dependem de PostgreSQL real.


Isso evita diferenças importantes entre:

PostgreSQL real

e

banco em memória.


# 41. Testcontainers

Testcontainers deve ser utilizado principalmente para:

- repositories;
- migrations;
- integração;
- regras que dependem de comportamento real do PostgreSQL.


# 42. Testes financeiros

Regras financeiras críticas devem possuir testes automatizados.

Exemplos:

- parcelas;
- fechamento;
- vencimento;
- pagamento;
- pagamento parcial;
- estorno;
- projeções.


# 43. Build

Utilizar:

Maven


O projeto deve possuir:

pom.xml


# 44. Dependências

As dependências devem ser adicionadas somente quando necessárias.

Antes de adicionar uma biblioteca:

1. verificar se o Spring já possui solução;
2. verificar se a funcionalidade pode ser implementada de forma simples;
3. avaliar manutenção da biblioteca;
4. avaliar compatibilidade.


# 45. Frontend

Utilizar:

Angular


# 46. Linguagem frontend

Utilizar:

TypeScript


A configuração deve utilizar modo estrito.


# 47. Angular

Utilizar a versão estável atual disponível no momento da implementação.

Preferir uma versão suportada oficialmente.

Não utilizar uma versão experimental.


# 48. Arquitetura Angular

Utilizar arquitetura moderna do Angular.

Preferir:

Standalone Components


em vez de depender de:

NgModules


quando não houver necessidade.


# 49. Angular Signals

Signals podem ser utilizados para gerenciamento de estado local e reatividade quando fizer sentido.

Não utilizar Signals apenas porque estão disponíveis.

Escolher a ferramenta conforme o problema.


# 50. Gerenciamento de estado

A V1 não precisa necessariamente de uma biblioteca global como:

NgRx


Começar utilizando recursos nativos do Angular.

Se a complexidade crescer significativamente, avaliar:

NgRx
ou
outra solução moderna.


# 51. HTTP

Utilizar:

Angular HttpClient


para comunicação com backend.


# 52. Interceptors

Utilizar HTTP Interceptors para:

- adicionar JWT;
- tratar erros globais;
- comportamento comum de requests.


# 53. Guards

Utilizar route guards para proteger páginas autenticadas.


# 54. Formulários

Preferir:

Reactive Forms


para formulários complexos.


# 55. Validação frontend

O frontend deve validar:

- campos obrigatórios;
- formatos;
- valores inválidos;
- feedback visual.


Entretanto, o backend continua sendo a autoridade final.


# 56. UI

A V1 deve utilizar uma biblioteca de componentes moderna e bem mantida.

Preferência inicial:

Angular Material


Motivos:

- integração natural com Angular;
- acessibilidade;
- componentes prontos;
- documentação;
- produtividade.


Não criar componentes complexos do zero quando Angular Material já resolver o problema adequadamente.


# 57. Ícones

Utilizar:

Material Icons


ou solução integrada ao Angular Material.


# 58. CSS

Utilizar CSS moderno.

Preferir:

CSS
SCSS


quando houver benefício real de organização.


# 59. Tailwind

Tailwind CSS não será obrigatório na V1.

Não adicionar Tailwind apenas por tendência.

A decisão poderá ser revisada futuramente.


# 60. Responsividade

A aplicação deve ser responsiva.

A V1 deve funcionar adequadamente em:

- desktop;
- notebook;
- tablet.


A prioridade inicial é desktop, considerando o uso financeiro pessoal.


# 61. Acessibilidade

A interface deve considerar:

- labels;
- navegação por teclado;
- contraste;
- foco;
- semântica;
- mensagens de erro acessíveis.


# 62. Gráficos

Para gráficos financeiros, utilizar uma biblioteca Angular compatível.

Preferência inicial:

ng2-charts

utilizando:

Chart.js


A biblioteca deve ser utilizada somente quando realmente houver gráficos para exibir.


# 63. Tabelas

Para tabelas:

Angular Material Table


pode ser utilizada.


# 64. Datas no frontend

Utilizar objetos e APIs de data apropriados.

Evitar manipulação manual excessiva de strings.


# 65. Formatação monetária

Utilizar recursos de internacionalização do Angular.

Locale:

pt-BR


Exemplo:

R$ 1.500,00


O frontend não deve alterar o valor original recebido da API.


# 66. Decimal

Valores financeiros recebidos da API devem ser tratados com cuidado.

Não utilizar operações matemáticas JavaScript com precisão inadequada para cálculos financeiros críticos.


# 67. Regra importante

O frontend não é responsável por calcular a verdade financeira.

Exemplo:

Não calcular saldo bancário definitivo apenas no Angular.


O backend deve fornecer os valores oficiais.


# 68. Gerenciamento de ambiente

Angular deverá possuir configurações de ambiente para:

development
production


A URL da API não deve ficar espalhada pelo código.


# 69. Lint

Utilizar:

ESLint


para TypeScript e Angular.


# 70. Formatting

Utilizar:

Prettier


para formatação automática.


# 71. Regra ESLint + Prettier

ESLint será responsável por qualidade do código.

Prettier será responsável por formatação.

As ferramentas não devem possuir regras conflitantes.


# 72. TypeScript

Utilizar:

strict: true


Sempre que possível.


# 73. Tipagem

Evitar:

any


Sempre que houver alternativa razoável.


# 74. Interfaces

Utilizar interfaces/types para representar contratos da API.

Exemplo:

ExpenseResponse
CreateExpenseRequest


# 75. API Client

As chamadas HTTP devem ser organizadas em services.

Exemplo:

ExpenseService


Não espalhar chamadas HttpClient diretamente pelos componentes.


# 76. Componentes

Componentes devem evitar concentrar:

- regra financeira;
- chamadas HTTP;
- transformação complexa;
- lógica de negócio.


Essas responsabilidades devem ser separadas.


# 77. Estado

Estado de tela pode permanecer no componente ou em services apropriados.

Não criar uma arquitetura global complexa antes de existir necessidade.


# 78. Banco local

Não utilizar SQLite ou banco local como banco principal.

O banco oficial é:

PostgreSQL.


# 79. Docker

Docker será utilizado desde o início.

Objetivo:

facilitar configuração do ambiente.


# 80. Docker Compose

Utilizar:

Docker Compose


para subir inicialmente:

PostgreSQL


Futuramente poderá incluir:

backend
frontend


se isso fizer sentido.


# 81. PostgreSQL Docker

O container PostgreSQL deve possuir:

- volume persistente;
- usuário;
- senha;
- database;
- porta configurável.


Credenciais não devem ser commitadas.


# 82. Environment variables

Utilizar:

.env


para configurações locais quando apropriado.


Nunca commitar:

senhas reais;
tokens;
credenciais.


# 83. .env.example

O projeto deve possuir:

.env.example


contendo apenas exemplos.


# 84. Git

O projeto utilizará Git.

O repositório será hospedado no GitHub.


# 85. GitHub

O Cursor não será necessariamente conectado ao GitHub pessoal do usuário.

Os commits poderão ser realizados pelo VSCode.


# 86. Regra para IA

A IA não deve assumir que possui acesso ao GitHub.

Não executar automaticamente:

git push


Não criar credenciais de GitHub.


# 87. Git

A IA pode executar comandos locais como:

git status
git diff
git log


quando solicitado ou necessário.


# 88. Commits

Os commits serão realizados pelo usuário através do VSCode.

A IA pode sugerir mensagens de commit.

Não realizar commit automaticamente sem autorização explícita.


# 89. Cursor

O Cursor será utilizado como ferramenta principal de desenvolvimento assistido por IA.

A IA deverá respeitar:

AGENTS.md

docs/


# 90. VSCode

O VSCode será utilizado para:

- Git;
- commits;
- revisão;
- execução;
- depuração;

quando conveniente.


# 91. Banco

O projeto deve funcionar localmente sem depender de serviços externos.

Inicialmente:

Docker
+
PostgreSQL


# 92. Ambiente mínimo

Para executar o projeto localmente, o desenvolvedor deverá precisar apenas das ferramentas documentadas no README.

O projeto deve documentar:

- Java;
- Maven;
- Node.js;
- npm;
- Docker;
- Docker Compose.


# 93. Node.js

Utilizar uma versão LTS atual.

A versão deve ser documentada.

Preferir uma versão suportada pelo Angular adotado.


# 94. Gerenciador de pacotes

Utilizar:

npm


Não utilizar simultaneamente:

npm
yarn
pnpm


sem justificativa.


# 95. Zod

Zod não será obrigatório na V1.

Motivo:

A API possui backend Java com validação própria através de Jakarta Bean Validation.

O Angular também possui validação através de Reactive Forms.

Adicionar Zod somente se surgir uma necessidade real de validação/runtime schema no frontend.


# 96. OpenAPI no frontend

A possibilidade de gerar tipos TypeScript a partir do contrato OpenAPI poderá ser considerada futuramente.

Na V1 não é obrigatório.


# 97. Segurança de dependências

Dependências devem ser mantidas atualizadas dentro de versões compatíveis.

Evitar dependências abandonadas.


# 98. Licenças

Antes de adicionar bibliotecas importantes, verificar se a licença é compatível com o projeto.


# 99. Arquitetura limpa

O projeto não deve implementar Clean Architecture, Hexagonal Architecture ou DDD completo apenas por moda.

A arquitetura deve permanecer simples.

Entretanto, deve possuir separação clara entre:

Controller
Service
Repository
Entity
DTO


# 100. Domínio financeiro

Regras financeiras importantes devem estar isoladas o suficiente para serem testadas sem depender diretamente do Angular.


# 101. Complexidade

Princípio:

"Simples primeiro."

Não implementar infraestrutura que não tenha benefício concreto na V1.


# 102. Observabilidade

A arquitetura deverá ficar preparada para futuramente adicionar:

- métricas;
- tracing;
- monitoramento.


Não é necessário implementar observabilidade avançada na V1.


# 103. Cache

Não utilizar Redis na V1.

Cache será avaliado somente quando existir uma necessidade real.


# 104. Mensageria

Não utilizar Kafka, RabbitMQ ou similares na V1.

O sistema é inicialmente monolítico e síncrono.


# 105. Microservices

Não utilizar microservices.

O backend será:

Monólito modular.


# 106. Arquitetura backend

Estrutura conceitual:

backend/
    src/
        main/
            java/
            resources/
        test/


Dentro do código:

config/
controller/
service/
repository/
entity/
dto/
exception/
security/


A organização poderá evoluir para organização por domínio caso isso melhore a manutenção.


# 107. Arquitetura frontend

Estrutura conceitual:

frontend/
    src/
        app/
            core/
            shared/
            features/


Features podem incluir:

dashboard/
accounts/
expenses/
incomes/
credit-cards/
invoices/
goals/


# 108. Core frontend

core/ deverá conter funcionalidades globais como:

- autenticação;
- interceptors;
- guards;
- serviços globais.


# 109. Shared frontend

shared/ deverá conter:

- componentes reutilizáveis;
- pipes;
- diretivas;
- utilitários.


Não transformar shared em depósito de código sem organização.


# 110. Features frontend

Cada feature deve concentrar sua própria lógica.

Exemplo:

expenses/

deve conter os componentes e serviços específicos de despesas.


# 111. Testes frontend

Utilizar o framework de testes recomendado pela versão do Angular adotada.

A estratégia exata deverá ser definida conforme o Angular escolhido.

Testar principalmente:

- services;
- componentes críticos;
- formulários;
- regras de apresentação.


# 112. E2E

Testes End-to-End poderão ser adicionados após a V1 funcional.

Preferência futura:

Playwright


Não é obrigatório implementar E2E imediatamente.


# 113. CI/CD

Não será obrigatório na V1.

A arquitetura deve permitir adicionar GitHub Actions posteriormente.


# 114. Deploy

Não haverá deploy inicialmente.

O sistema será executado localmente.


# 115. Regra para novas tecnologias

Antes de adicionar uma nova tecnologia:

1. identificar o problema;
2. verificar se a stack atual resolve;
3. avaliar complexidade;
4. avaliar manutenção;
5. justificar a inclusão;
6. documentar a decisão.


# 116. ADR

Decisões arquiteturais importantes poderão ser registradas futuramente em:

docs/adr/


Exemplo:

001-choice-of-authentication.md


Não criar ADR para decisões triviais.


# 117. Prioridade

A prioridade tecnológica é:

1. correção;
2. segurança;
3. simplicidade;
4. testabilidade;
5. manutenção;
6. produtividade;
7. performance.


# 118. Performance

Não otimizar prematuramente.

Primeiro:

funcionar corretamente.

Depois:

medir.

Depois:

otimizar.


# 119. Regra financeira

Nenhuma tecnologia escolhida pode comprometer a precisão financeira do sistema.

Precisão e consistência são mais importantes que conveniência.


# 120. Stack oficial inicial

Backend:

Java 21 LTS
Spring Boot
Spring Web
Spring Data JPA
Hibernate
Spring Security
JWT
Jakarta Validation
MapStruct
Maven
JUnit 5
Mockito
Testcontainers
Flyway
springdoc-openapi


Database:

PostgreSQL


Frontend:

Angular
TypeScript
Angular Material
Reactive Forms
HttpClient
ESLint
Prettier
Chart.js
ng2-charts


Infraestrutura:

Docker
Docker Compose


Controle de versão:

Git
GitHub


IDE:

Cursor
VSCode


# 121. Regra final

A stack deve permanecer pequena e coerente.

Não adicionar:

Redis
Kafka
RabbitMQ
Kubernetes
Microservices
NgRx
Zod
Tailwind
GraphQL

na V1 sem uma necessidade concreta e uma decisão documentada.

O objetivo é construir um sistema financeiro real, sólido e didático, utilizando tecnologias modernas sem transformar o projeto em uma coleção de ferramentas.