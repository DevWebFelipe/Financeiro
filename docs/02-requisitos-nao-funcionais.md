# Requisitos Não Funcionais — Financial Control

## 1. Objetivo

Este documento define os requisitos relacionados à qualidade, segurança, desempenho, manutenção, arquitetura, testes e operação do sistema Financial Control.

Os requisitos funcionais definem o que o sistema faz.

Os requisitos não funcionais definem como o sistema deve se comportar e quais padrões técnicos devem ser seguidos.


## 2. Arquitetura

### RNF-001 — Arquitetura modular

O backend deverá utilizar uma arquitetura modular.

A aplicação será um monólito modular na V1.

Não utilizar microsserviços neste momento.

Os módulos deverão possuir responsabilidades bem definidas e baixo acoplamento.


### RNF-002 — Separação de responsabilidades

O backend deve separar claramente:

- Controller;
- DTO;
- Service;
- Repository;
- Entity;
- regras de negócio;
- tratamento de exceções;
- configuração.

Controllers não devem conter regras de negócio complexas.

Repositories não devem conter regras de negócio.

Services devem concentrar as regras de negócio.


### RNF-003 — DTOs

Entidades JPA não devem ser retornadas diretamente pelos controllers.

A comunicação da API deve utilizar DTOs.

Isso vale tanto para requisições quanto para respostas.


### RNF-004 — Domínio financeiro

As regras financeiras devem permanecer concentradas no backend.

O frontend pode realizar validações para melhorar a experiência do usuário, mas não deve ser responsável por garantir a integridade financeira.


## 3. Banco de dados

### RNF-005 — PostgreSQL

O banco de dados oficial será PostgreSQL.

A aplicação deverá ser compatível com a versão definida no arquivo de infraestrutura do projeto.


### RNF-006 — UUID

As entidades principais devem utilizar UUID como identificador.

Evitar IDs sequenciais expostos pela API.


### RNF-007 — Valores monetários

Valores monetários devem utilizar:

Java:
BigDecimal

PostgreSQL:
NUMERIC

Nunca utilizar:

- float;
- double;

para armazenar valores financeiros.


### RNF-008 — Integridade referencial

O banco de dados deve utilizar:

- Primary Keys;
- Foreign Keys;
- Unique Constraints;
- Check Constraints quando apropriado;
- índices.

A integridade dos dados não deve depender exclusivamente do código Java.


### RNF-009 — Migrations

Todas as alterações estruturais do banco devem ser controladas por migrations.

Utilizar Flyway.

Uma migration já aplicada nunca deve ser alterada.

Uma alteração posterior deve criar uma nova migration.


### RNF-010 — Nomenclatura

A nomenclatura do banco deve seguir padrão consistente.

Preferir:

snake_case

Exemplo:

user_id
created_at
updated_at
due_date


## 4. Segurança

### RNF-011 — Senhas

Senhas nunca devem ser armazenadas em texto puro.

Utilizar algoritmo seguro de hashing apropriado para senhas.


### RNF-012 — Autenticação

A autenticação deve utilizar JWT.

Tokens devem possuir expiração.


### RNF-013 — Autorização

Toda requisição protegida deve validar o usuário autenticado.

O sistema deve impedir acesso a dados de outros usuários.


### RNF-014 — Isolamento de dados

O backend deve utilizar o usuário autenticado para determinar o proprietário dos dados.

Nunca confiar exclusivamente em um idUsuario enviado pelo frontend.


### RNF-015 — Informações sensíveis

Não armazenar no código-fonte:

- senhas;
- tokens;
- chaves secretas;
- credenciais de banco;
- chaves privadas.

Utilizar variáveis de ambiente.


### RNF-016 — Configuração

As configurações devem ser separadas por ambiente.

Inicialmente considerar:

- desenvolvimento;
- testes.

Uma configuração de produção não é necessária na V1.


## 5. API

### RNF-017 — REST

A API deverá seguir princípios REST.

Utilizar corretamente os métodos HTTP:

GET
POST
PUT
PATCH
DELETE

Quando apropriado.


### RNF-018 — Versionamento

A API deverá possuir versionamento.

Prefixo inicial:

/api/v1


### RNF-019 — HTTP Status Codes

A API deve utilizar códigos HTTP apropriados.

Exemplos:

200 OK
201 CREATED
204 NO_CONTENT
400 BAD_REQUEST
401 UNAUTHORIZED
403 FORBIDDEN
404 NOT_FOUND
409 CONFLICT
422 UNPROCESSABLE_ENTITY
500 INTERNAL_SERVER_ERROR


### RNF-020 — Erros padronizados

Os erros da API devem possuir estrutura padronizada.

Uma resposta de erro deve permitir identificar pelo menos:

- código;
- mensagem;
- timestamp;
- endpoint;
- detalhes de validação quando aplicável.


### RNF-021 — Validação

Os dados recebidos pela API devem ser validados.

Utilizar Bean Validation no backend.

Exemplos:

- campos obrigatórios;
- tamanho mínimo;
- tamanho máximo;
- formato;
- valores positivos;
- datas válidas.


### RNF-022 — Paginação

Endpoints que possam retornar grande quantidade de registros devem suportar paginação.

Evitar retornar grandes quantidades de registros em uma única requisição.


### RNF-023 — Ordenação e filtros

Listagens relevantes devem permitir filtros e ordenação quando necessário.

Os filtros devem ser definidos de acordo com cada recurso.


## 6. Documentação da API

### RNF-024 — OpenAPI

A API deve possuir documentação OpenAPI.

Utilizar Swagger UI para facilitar testes e aprendizado.


### RNF-025 — Documentação dos endpoints

Endpoints públicos da API devem possuir documentação contendo:

- descrição;
- parâmetros;
- corpo da requisição;
- respostas;
- códigos HTTP;
- exemplos quando forem úteis.


## 7. Testes

### RNF-026 — Testes automatizados

Testes automatizados são obrigatórios.

Uma funcionalidade relevante não deve ser considerada concluída sem os testes correspondentes.


### RNF-027 — Testes unitários

Utilizar JUnit e Mockito.

Os testes unitários devem validar principalmente regras de negócio.


### RNF-028 — Testes de integração

Quando houver interação relevante com:

- PostgreSQL;
- JPA;
- segurança;
- API;

devem ser utilizados testes de integração quando apropriado.


### RNF-029 — Testcontainers

Utilizar Testcontainers para testes que dependam de PostgreSQL real.

Evitar substituir o comportamento real do banco por mocks quando o objetivo do teste for validar integração com PostgreSQL.


### RNF-030 — Testes de segurança

Devem existir testes para garantir que um usuário não consiga acessar dados pertencentes a outro usuário.


### RNF-031 — Testes financeiros

Devem existir testes para cenários financeiros importantes, incluindo:

- receitas;
- despesas;
- pagamentos;
- transferências;
- compras no cartão;
- fechamento de fatura;
- vencimento de fatura;
- parcelamentos;
- parcelas com valores diferentes;
- pagamento parcial;
- parcelamento de saldo;
- estornos;
- cancelamentos;
- projeções.


## 8. Frontend

### RNF-032 — Angular

O frontend deverá utilizar Angular com TypeScript.


### RNF-033 — Standalone Components

Utilizar Standalone Components.

Evitar NgModules quando não forem necessários.


### RNF-034 — Reactive Forms

Formulários complexos devem utilizar Reactive Forms.


### RNF-035 — Signals

Utilizar Signals quando forem apropriados para gerenciamento de estado local e reatividade.


### RNF-036 — Organização

O frontend deverá ser organizado por funcionalidades.

Evitar uma estrutura baseada apenas em tipos técnicos.

Preferir:

features/accounts
features/expenses
features/cards

em vez de concentrar todos os componentes em uma única pasta.


### RNF-037 — Comunicação com API

A comunicação com o backend deve ser centralizada em serviços.

Componentes não devem realizar chamadas HTTP diretamente quando isso puder ser evitado.


### RNF-038 — Tratamento de erros

O frontend deve possuir tratamento consistente para erros da API.

Mensagens técnicas não devem ser exibidas diretamente ao usuário final.


## 9. Bibliotecas

### RNF-039 — Dependências

Evitar dependências desnecessárias.

Antes de adicionar uma biblioteca externa relevante, avaliar:

- necessidade;
- maturidade;
- manutenção;
- documentação;
- comunidade;
- impacto no bundle;
- complexidade adicionada;
- possibilidade de utilizar recursos nativos.


### RNF-040 — Zod

Zod não deve ser adicionado automaticamente.

Antes de utilizar Zod no frontend, avaliar se:

- as validações do Angular Reactive Forms são suficientes;
- existe necessidade de validação adicional de dados vindos da API;
- a biblioteca realmente melhora a arquitetura.

A decisão deve ser documentada antes da adoção.


### RNF-041 — Gerenciamento de estado

Não utilizar NgRx ou outra biblioteca global de estado automaticamente.

Na V1, utilizar:

- Signals;
- serviços;
- estado local;

sempre que forem suficientes.

Uma solução global somente deve ser adotada quando a complexidade do projeto justificar.


## 10. Performance

### RNF-042 — Consultas

Evitar consultas desnecessárias ao banco.

Observar especialmente:

- N+1 queries;
- joins desnecessários;
- carregamento excessivo de relacionamentos;
- consultas sem filtros.


### RNF-043 — Paginação

Listagens potencialmente grandes devem utilizar paginação.

### RNF-044 — Frontend

Evitar:

- renderizações desnecessárias;
- chamadas HTTP duplicadas;
- carregamento de dados que não são utilizados;
- componentes excessivamente complexos.


## 11. Observabilidade

### RNF-045 — Logs

O backend deve possuir logs estruturados e úteis para diagnóstico.

Não registrar informações sensíveis.

Nunca registrar:

- senha;
- token JWT;
- credenciais;
- dados financeiros desnecessários.


### RNF-046 — Tratamento de exceções

Exceções inesperadas devem ser tratadas de maneira centralizada.

O backend não deve expor stack traces para o usuário final.


## 12. Docker

### RNF-047 — Docker Compose

O projeto deverá possuir configuração Docker Compose para facilitar a execução local.

Inicialmente deverá existir pelo menos:

- PostgreSQL.

A inclusão do backend e frontend no Docker poderá ser feita conforme a evolução do projeto.


### RNF-048 — Ambiente local

O objetivo do Docker na V1 é facilitar a configuração do ambiente e reduzir dependências instaladas diretamente na máquina.


## 13. Configuração do ambiente

### RNF-049 — Variáveis de ambiente

Configurações como:

- banco;
- usuário do banco;
- senha;
- JWT secret;
- portas;

devem poder ser configuradas através de variáveis de ambiente.


### RNF-050 — Arquivo de exemplo

O projeto deve possuir um arquivo de exemplo de configuração.

Exemplo:

.env.example

Esse arquivo não deve conter credenciais reais.


## 14. Git

### RNF-051 — Controle de versão

O projeto deverá utilizar Git.

A IA não deve realizar push para o GitHub.

O desenvolvedor será responsável pelos commits e pushes através do VSCode.


### RNF-052 — Commits

Os commits devem ser pequenos e relacionados a uma alteração lógica.

Evitar commits gigantes contendo várias funcionalidades não relacionadas.


### RNF-053 — Arquivos ignorados

Arquivos temporários e arquivos contendo informações sensíveis devem estar no .gitignore.


## 15. Manutenibilidade

### RNF-054 — Código legível

O código deve priorizar legibilidade.

Evitar abstrações prematuras.

Não criar padrões complexos apenas porque são considerados "enterprise".


### RNF-055 — Nomes

Variáveis, métodos, classes e componentes devem possuir nomes claros.

Evitar abreviações desnecessárias.


### RNF-056 — Métodos

Métodos devem possuir responsabilidades claras.

Métodos excessivamente grandes devem ser divididos.


### RNF-057 — Comentários

Comentários devem explicar o motivo de uma decisão quando isso não for evidente pelo código.

Evitar comentários que apenas repetem o código.


## 16. Internacionalização e idioma

### RNF-058 — Idioma da interface

A interface inicialmente será em português do Brasil.

### RNF-059 — Código

O código deverá utilizar preferencialmente nomes em inglês.

Exemplos:

User
Expense
Income
Account
CreditCard
Invoice
Payment

Textos apresentados ao usuário podem permanecer em português.


## 17. Datas

### RNF-060 — Datas sem horário

Datas que representam somente um dia devem utilizar tipos apropriados para data sem horário.

Exemplo:

LocalDate no Java.

### RNF-061 — Data e horário

Operações que realmente necessitem de horário devem utilizar tipos apropriados para data e hora.

Evitar armazenar tudo como timestamp sem necessidade.


## 18. Fuso horário

### RNF-062 — Timezone

O sistema deve considerar inicialmente o fuso horário do Brasil.

A implementação deve evitar decisões que impeçam futuramente o suporte a múltiplos fusos.


## 19. Responsividade

### RNF-063 — Interface responsiva

O frontend deve funcionar adequadamente em:

- desktop;
- notebook;
- tablet;
- celular.

A V1 não precisa ser uma aplicação mobile nativa.


## 20. Acessibilidade

### RNF-064 — Acessibilidade básica

O frontend deve seguir boas práticas básicas de acessibilidade.

Considerar:

- labels em formulários;
- navegação por teclado;
- contraste adequado;
- textos alternativos;
- estados de foco;
- mensagens de erro compreensíveis.


## 21. Interface

### RNF-065 — Simplicidade

A interface deve priorizar clareza.

O sistema é financeiro e deve facilitar a tomada de decisão.

Evitar excesso de elementos visuais desnecessários.


### RNF-066 — Tema

A V1 terá somente modo claro.

Suporte a modo escuro poderá ser adicionado futuramente.


## 22. Relatórios

### RNF-067 — Exportação

Relatórios exportados devem possuir dados suficientes para identificação.

Exemplo:

- usuário;
- cartão;
- fatura;
- período;
- responsável;
- data de geração.

O formato definitivo será escolhido durante a implementação.


## 23. Evolução futura

### RNF-068 — Extensibilidade

A arquitetura deve permitir futura implementação de:

- investimentos;
- importação de extratos;
- integrações bancárias;
- notificações;
- compartilhamento;
- contas compartilhadas;
- múltiplas moedas;
- relatórios avançados.

Essas funcionalidades não devem ser implementadas na V1.


## 24. Prioridade

Quando houver conflito entre requisitos, utilizar a seguinte prioridade:

1. segurança;
2. integridade financeira;
3. correção dos dados;
4. regras de negócio;
5. testes;
6. manutenibilidade;
7. performance;
8. experiência do usuário;
9. estética.


## 25. Regra de decisão tecnológica

Nenhuma tecnologia adicional relevante deve ser adicionada simplesmente por ser popular ou moderna.

A tecnologia deve ser escolhida considerando:

- problema que resolve;
- maturidade;
- compatibilidade com o projeto;
- curva de aprendizado;
- manutenção;
- benefício real.

O projeto possui finalidade educacional.

Portanto, tecnologias modernas são desejáveis, mas não devem ser utilizadas apenas para aumentar a quantidade de ferramentas utilizadas.