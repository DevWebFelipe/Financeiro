# Stack Tecnológica — Financial Control

## 1. Objetivo

Este documento define as tecnologias oficiais utilizadas no projeto Financial Control.

A IA deve seguir esta stack durante a implementação.

Não substituir tecnologias sem justificativa técnica e sem atualização da documentação.


# 2. Princípio

A stack deve priorizar:

- tecnologias modernas;
- tecnologias estáveis;
- tecnologias amplamente utilizadas;
- boa documentação;
- facilidade de aprendizado;
- integração entre ferramentas;
- manutenção de longo prazo.


# 3. Regra de versões

As versões devem ser definidas no momento da criação do projeto.


Antes de iniciar a implementação:

verificar as versões estáveis atualmente disponíveis e compatíveis entre si.


# 4. Java

Tecnologia:

Java


Versão:

Java 21 LTS


# 5. Motivo

Java 21 será utilizado por ser uma versão LTS moderna, madura e amplamente suportada.


# 6. Backend

Framework:

Spring Boot


Utilizar a versão estável mais recente da linha compatível com Java 21 no momento da criação do projeto.


# 7. Build

Ferramenta:

Maven


# 8. Maven

O arquivo principal será:

pom.xml


# 9. Spring Web

Utilizar:

Spring Web


para criação da API REST.


# 10. Spring Data JPA

Utilizar:

Spring Data JPA


para persistência.


# 11. Hibernate

Hibernate será utilizado como implementação JPA através do Spring Boot.


# 12. Banco

PostgreSQL


# 13. Driver PostgreSQL

Utilizar o driver oficial JDBC do PostgreSQL.


# 14. Migration

Flyway


# 15. Flyway

Todas as alterações estruturais do banco devem ser feitas através de migrations.


# 16. Hibernate

Não utilizar:

ddl-auto=create


em ambiente normal.


# 17. Hibernate

Preferir:

ddl-auto=validate


após o schema estar definido pelas migrations.


# 18. Segurança

Spring Security


# 19. Autenticação

JWT


# 20. JWT

Utilizar biblioteca madura e bem mantida para criação e validação de tokens JWT.


# 21. JWT

Nunca armazenar senha do usuário em texto puro.


# 22. Senha

Utilizar:

BCrypt


ou algoritmo moderno equivalente suportado pelo Spring Security.


# 23. Bean Validation

Utilizar:

Jakarta Validation


com implementação fornecida pelo Spring Boot.


# 24. OpenAPI

Utilizar:

springdoc-openapi


# 25. Swagger

Swagger UI deve estar disponível durante o desenvolvimento.


# 26. Lombok

Lombok pode ser utilizado.


Porém:

não utilizar Lombok indiscriminadamente.


# 27. Lombok

Evitar:

@SneakyThrows


e outras anotações que escondam comportamento importante.


# 28. Lombok

Utilizar principalmente para reduzir boilerplate simples.


# 29. MapStruct

MapStruct pode ser utilizado para mapeamento:

Entity → DTO

DTO → Entity


# 30. MapStruct

Não é obrigatório utilizar MapStruct em todas as classes.


Se o mapeamento for extremamente simples:

conversão manual é aceitável.


# 31. Arquitetura

Não utilizar:

Spring Cloud


na V1.


# 32. Mensageria

Não utilizar:

Kafka

RabbitMQ


na V1.


# 33. Cache

Não utilizar:

Redis


na V1.


# 34. Busca

Não utilizar Elasticsearch na V1.


# 35. Backend

Stack principal:

Java 21

Spring Boot

Spring Web

Spring Data JPA

Hibernate

PostgreSQL

Flyway

Spring Security

JWT

Jakarta Validation

OpenAPI


# 36. Testes backend

JUnit 5


# 37. Mockito

Utilizar Mockito quando mocks forem necessários.


# 38. Testes Spring

Utilizar:

Spring Boot Test


# 39. Testes de integração

Utilizar:

Testcontainers


# 40. PostgreSQL nos testes

Os testes de integração que dependem do banco devem utilizar PostgreSQL real através do Testcontainers.


# 41. Regra

Não utilizar H2 como substituto padrão do PostgreSQL para testes de persistência.


# 42. AssertJ

Pode ser utilizado para assertions mais legíveis.


# 43. Testes

Priorizar:

- testes unitários;
- testes de integração;
- testes de regras financeiras.


# 44. Frontend

Tecnologia:

Angular


# 45. Angular

Utilizar a versão estável mais recente disponível no momento da criação do projeto.


# 46. Angular

Preferir:

Standalone Components


# 47. Linguagem

TypeScript


# 48. TypeScript

Utilizar configuração:

strict


# 49. Regra

Não utilizar:

any


sem justificativa.


# 50. Angular

Utilizar:

Signals


quando apropriado.


# 51. Angular

Não utilizar biblioteca global de estado na V1 sem necessidade.


# 52. Estado

Preferir:

Signals;

Services;

RxJS.


# 53. RxJS

Utilizar para:

operações assíncronas;

streams;

HTTP;


quando apropriado.


# 54. HTTP

Utilizar:

HttpClient


# 55. Forms

Utilizar:

Reactive Forms


# 56. Validação

Validações importantes também devem existir no backend.


# 57. UI

A biblioteca visual deve ser definida antes da implementação do frontend.


# 58. UI

Priorizar uma biblioteca madura e compatível com Angular.


# 59. Opção V1

Utilizar:

Angular Material


como biblioteca principal de componentes.


# 60. Motivo

Angular Material fornece:

- componentes acessíveis;
- integração com Angular;
- tabelas;
- formulários;
- dialogs;
- menus;
- date pickers;
- responsividade.


# 61. Ícones

Utilizar:

Material Icons


quando apropriado.


# 62. Gráficos

Utilizar:

Apache ECharts


através de integração compatível com Angular.


# 63. Gráficos

Os gráficos devem receber dados preparados pelo backend.


# 64. Regra

Não implementar cálculos financeiros complexos dentro da configuração dos gráficos.


# 65. CSS

Utilizar:

CSS


com organização modular.


# 66. CSS

Pode utilizar:

CSS variables


para tokens de design.


# 67. CSS

Não utilizar framework CSS adicional sem necessidade.


# 68. Bootstrap

Não utilizar Bootstrap na V1.


# 69. Tailwind

Não utilizar Tailwind na V1.


# 70. Motivo

Angular Material será suficiente para a interface inicial.


# 71. Lint

Utilizar:

ESLint


para TypeScript/Angular.


# 72. Formatter

Utilizar:

Prettier


# 73. Prettier

O projeto deve possuir configuração versionada.


# 74. ESLint

A configuração deve ser versionada.


# 75. Regra

Código deve passar por:

lint;

format;


antes de ser considerado concluído.


# 76. Husky

Não é obrigatório utilizar Husky na V1.


# 77. Pre-commit

Não criar hooks complexos apenas por antecipação.


# 78. Frontend tests

Utilizar a solução de testes recomendada pela versão do Angular escolhida.


# 79. Frontend

Testar principalmente:

services;

componentes críticos;

formulários;

regras de apresentação importantes.


# 80. E2E

Testes End-to-End podem ser adicionados posteriormente.


# 81. Node.js

Utilizar uma versão LTS moderna compatível com a versão do Angular escolhida.


# 82. Package Manager

Utilizar:

npm


# 83. Regra

Não misturar:

npm

yarn

pnpm


no mesmo projeto.


# 84. Lockfile

Versionar:

package-lock.json


# 85. Backend dependencies

Dependências devem ser mantidas no:

pom.xml


# 86. Frontend dependencies

Dependências devem ser mantidas no:

package.json


# 87. Docker

Utilizar:

Docker


# 88. Docker Compose

Utilizar:

Docker Compose


para infraestrutura local.


# 89. PostgreSQL

PostgreSQL deve rodar preferencialmente em container durante desenvolvimento.


# 90. Persistência

Utilizar:

Docker volume


para persistir os dados do PostgreSQL.


# 91. Banco

O volume do PostgreSQL não deve ser commitado no Git.


# 92. Docker

O projeto deve possuir:

Dockerfile


quando a aplicação começar a ser containerizada.


# 93. Docker

O Dockerfile do backend deve utilizar imagem Java adequada.


# 94. Docker

O frontend poderá possuir Dockerfile próprio posteriormente.


# 95. Ambiente

Configurações devem ser externas ao código.


# 96. Environment

Utilizar:

application.yml


para configuração padrão.


# 97. Variáveis

Valores específicos do ambiente devem utilizar:

environment variables.


# 98. Secrets

Nunca commitados.


# 99. Git

Utilizar:

.gitignore


# 100. Cursor

Utilizar:

.cursorignore


quando necessário.


# 101. Cursor

O Cursor não deve acessar:

secrets;

tokens;

credenciais;

arquivos temporários.


# 102. GitHub

GitHub pessoal não será conectado ao Cursor.


# 103. Git

Commits serão realizados através do VSCode.


# 104. Git

O projeto deve possuir histórico Git normal e independente da IDE.


# 105. API Client

Não utilizar Postman como dependência do projeto.


Postman será ferramenta externa de testes manuais.


# 106. Documentação API

Swagger UI será a documentação interativa principal da API.


# 107. Documentação

Também devem existir:

README.md


e documentação em:

docs/


# 108. Diagrama

Quando necessário, utilizar:

Mermaid


para diagramas.


# 109. Mermaid

Diagramas podem ser utilizados para:

- arquitetura;
- banco;
- fluxos;
- sequência.


# 110. Banco

Pode utilizar:

dbdiagram.io


como ferramenta visual externa.


Não é dependência da aplicação.


# 111. Versionamento

Git


# 112. GitHub

Repositório remoto:

GitHub


# 113. Commits

Commits devem ser pequenos e relacionados a uma alteração lógica.


# 114. Branches

Na V1 pode ser utilizado:

main


e branches de feature quando necessário.


# 115. IA

A IA deve evitar commits automáticos.


# 116. Cursor

O Cursor será utilizado principalmente para:

- gerar código;
- refatorar;
- explicar;
- testar;
- documentar.


# 117. Cursor

O desenvolvedor continuará responsável por:

- revisar código;
- executar testes;
- entender decisões;
- realizar commits.


# 118. Regra de aprendizado

Sempre que a IA adicionar uma tecnologia ou conceito relevante:

explicar brevemente:

1. o que é;
2. por que foi escolhido;
3. onde está sendo utilizado.


# 119. Dependências

Antes de adicionar dependência:

verificar se:

- já existe solução no projeto;
- o framework já possui recurso equivalente;
- a biblioteca é mantida;
- possui documentação adequada;
- possui licença compatível.


# 120. Dependências

Evitar dependências abandonadas ou pouco mantidas.


# 121. Atualização

Não atualizar versões automaticamente durante o desenvolvimento de uma funcionalidade sem necessidade.


# 122. Breaking changes

Atualizações que possam gerar breaking changes devem ser analisadas antes.


# 123. Banco

PostgreSQL deve ser a única fonte de persistência da V1.


# 124. Arquivos

Não utilizar banco SQLite.


# 125. Arquivos

Não utilizar H2 como banco da aplicação.


# 126. Backend

Não utilizar Node.js.


# 127. Backend

Não utilizar Python.


# 128. Frontend

Não utilizar React.


# 129. Frontend

Não utilizar Vue.


# 130. Regra

O objetivo deste projeto é também consolidar aprendizado em:

Java;

Spring Boot;

Angular;

TypeScript;

PostgreSQL.


# 131. Zod

Zod NÃO será utilizado no backend Java.


# 132. Zod

A validação do backend será feita através de:

Jakarta Validation.


# 133. Zod

No frontend Angular, Zod também não é obrigatório.


# 134. Regra

Preferir:

Reactive Forms;

Angular Validators;

TypeScript strict.


# 135. Motivo

Adicionar Zod neste momento duplicaria parte das validações sem necessidade clara.


# 136. Futuro

Zod poderá ser reconsiderado caso o frontend passe a consumir estruturas complexas que justifiquem validação runtime adicional.


# 137. API contracts

DTOs do backend serão a principal definição dos contratos da API.


# 138. OpenAPI

OpenAPI deve documentar os contratos.


# 139. Type generation

A geração automática de tipos TypeScript a partir do OpenAPI pode ser adotada futuramente.


# 140. V1

Não tornar a geração automática de clientes uma dependência obrigatória da primeira etapa.


# 141. Qualidade

Backend:

Maven test


Frontend:

npm test


e:

npm run lint


# 142. Build

Backend deve ser capaz de executar:

mvn clean verify


# 143. Frontend

Frontend deve ser capaz de executar:

npm run build


# 144. Formatação

Frontend deve possuir comando:

npm run format


ou equivalente.


# 145. Lint

Frontend deve possuir comando:

npm run lint


# 146. Testes

Backend:

mvn test


# 147. Integração

Testes com PostgreSQL:

Testcontainers


# 148. Desenvolvimento local

Infraestrutura mínima:

Docker

PostgreSQL


# 149. Execução

Backend:

Java + Spring Boot


# 150. Execução

Frontend:

Angular CLI


# 151. Angular CLI

Utilizar:

ng


para tarefas do Angular.


# 152. API

Backend padrão:

porta definida pelo projeto.


# 153. Frontend

Angular padrão:

porta 4200


quando não houver necessidade diferente.


# 154. PostgreSQL

Porta padrão:

5432


quando não houver conflito.


# 155. Swagger

Disponível em endpoint padrão definido pelo springdoc.


# 156. Configuração

As portas devem ser configuráveis.


# 157. Ambiente local

Exemplo conceitual:

Angular

localhost:4200


Spring Boot

localhost:8080


PostgreSQL

localhost:5432


# 158. CORS

Configurar CORS apenas para origens necessárias durante desenvolvimento.


# 159. Segurança

Não utilizar:

allow all origins


em ambiente real.


# 160. CORS

A configuração deve ser externa/configurável.


# 161. Banco

Usuário do banco da aplicação não deve utilizar:

postgres


como usuário da aplicação em ambiente configurado.


# 162. Docker

Criar usuário/database específicos para a aplicação.


# 163. Timezone

Definir timezone padrão do sistema explicitamente.


# 164. Datas financeiras

Operações que representam somente uma data devem utilizar:

LocalDate


# 165. Timestamps

Eventos que realmente representam instante devem utilizar:

Instant


# 166. Auditoria

Campos de auditoria devem ser considerados nas entidades principais.


Exemplo:

createdAt

updatedAt


# 167. Auditoria

Para operações financeiras importantes:

createdBy


pode ser considerado quando necessário.


# 168. Soft delete

Não implementar soft delete universal automaticamente em todas as entidades.


# 169. Soft delete

Utilizar status/active quando houver necessidade de preservar histórico.


# 170. Arquitetura

Não utilizar:

event sourcing


na V1.


# 171. Arquitetura

Não utilizar:

CQRS


na V1.


# 172. Arquitetura

Não utilizar:

DDD excessivamente complexo.


# 173. Arquitetura

Aplicar conceitos de domínio somente onde trouxerem benefício real.


# 174. Regra final

A stack oficial da V1 é:

Java 21

Spring Boot

Maven

Spring Web

Spring Data JPA

Hibernate

PostgreSQL

Flyway

Spring Security

JWT

Jakarta Validation

OpenAPI

JUnit

Mockito

Spring Boot Test

Testcontainers

Angular

TypeScript

Angular Material

RxJS

Signals

Apache ECharts

ESLint

Prettier

Node.js LTS

npm

Docker

Docker Compose

Git


# 175. Regra final

Se a IA quiser alterar qualquer item desta stack:

1. explicar o motivo;
2. apresentar a alternativa;
3. avaliar impacto;
4. atualizar este documento;
5. somente então implementar.