# AGENTS.md — Financial Control

## 1. Objetivo

Este projeto é um sistema de controle financeiro pessoal multiusuário chamado Financial Control.

O sistema será desenvolvido inicialmente para execução local e terá como objetivo fornecer uma base sólida, organizada, testável e extensível para controle financeiro pessoal.

O projeto também possui finalidade educacional. As decisões técnicas devem ser explicadas quando forem relevantes, permitindo que o desenvolvedor compreenda não apenas "o que fazer", mas também "por que fazer".

---

## 2. Regra principal de desenvolvimento

O projeto DEVE ser desenvolvido por etapas.

NUNCA implementar o sistema inteiro de uma única vez.

Cada etapa deve seguir este fluxo:

1. analisar o estado atual do projeto;
2. analisar a documentação relacionada à etapa;
3. implementar somente o escopo definido;
4. executar o build;
5. executar os testes;
6. corrigir problemas encontrados;
7. atualizar a documentação quando necessário;
8. apresentar um resumo das alterações;
9. informar eventuais decisões ou problemas encontrados;
10. aguardar autorização para iniciar a próxima etapa.

Se uma decisão importante de negócio não estiver definida, a IA deve parar e perguntar antes de implementar.

---

## 3. Stack tecnológica

### Backend

- Java 25
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT
- Bean Validation
- Flyway
- PostgreSQL
- OpenAPI / Swagger
- JUnit
- Mockito
- Testcontainers

### Frontend

- Angular
- TypeScript
- Standalone Components
- Angular Router
- HttpClient
- Reactive Forms
- Signals

### Infraestrutura

- Docker
- Docker Compose
- Git
- GitHub

---

## 4. Banco de dados

O banco de dados oficial do projeto é PostgreSQL.

Regras:

- utilizar UUID como identificador das entidades;
- utilizar NUMERIC para valores monetários;
- utilizar BigDecimal no Java para valores monetários;
- utilizar Foreign Keys;
- utilizar constraints;
- criar índices quando necessários;
- utilizar Flyway para migrations;
- nunca alterar uma migration que já tenha sido executada;
- alterações posteriores devem ser feitas através de novas migrations.

---

## 5. Arquitetura

O sistema será desenvolvido como uma aplicação monolítica modular.

Não criar microsserviços na V1.

O backend deverá possuir separação clara entre:

- Controller;
- DTO;
- Service;
- regras de negócio;
- Repository;
- Entity.

O backend é a autoridade final sobre as regras de negócio.

O frontend pode realizar validações para melhorar a experiência do usuário, mas nenhuma regra importante de segurança ou negócio deve depender exclusivamente do frontend.

Entidades JPA não devem ser expostas diretamente pela API.

Utilizar DTOs na comunicação entre backend e frontend.

---

## 6. Multiusuário

O sistema será multiusuário.

Todo dado financeiro deve pertencer direta ou indiretamente a um usuário.

Um usuário nunca poderá acessar ou alterar dados pertencentes a outro usuário.

O backend deve garantir o isolamento dos dados.

Nunca confiar em um idUsuario enviado pelo frontend para determinar o proprietário de uma operação.

O usuário autenticado deve ser obtido através do contexto de segurança.

---

## 7. Segurança

Senhas nunca devem ser armazenadas em texto puro.

Utilizar mecanismo seguro de hash de senha.

A autenticação da API deverá utilizar JWT.

Segredos e credenciais não podem ser armazenados no código-fonte.

Utilizar variáveis de ambiente para informações sensíveis.

Arquivos contendo credenciais, tokens, senhas ou chaves privadas nunca devem ser versionados.

---

## 8. Valores monetários

Valores financeiros devem utilizar precisão decimal.

No Java utilizar BigDecimal.

No PostgreSQL utilizar NUMERIC.

Não utilizar float ou double para representar valores financeiros.

Arredondamentos financeiros devem ser definidos explicitamente conforme a regra de negócio.

---

## 9. Regras financeiras fundamentais

As seguintes regras são obrigatórias:

### 9.1 Receita prevista

Uma receita prevista não altera o saldo real da conta.

Ela participa das projeções futuras.

### 9.2 Receita recebida

Uma receita recebida gera uma entrada financeira.

### 9.3 Despesa pendente

Uma despesa pendente não gera saída financeira.

Ela deve aparecer nas contas a pagar e participar das projeções.

### 9.4 Despesa paga

Uma despesa paga gera uma saída financeira.

### 9.5 Compra no cartão

Uma compra no cartão representa um compromisso com a fatura.

Ela não deve reduzir imediatamente o saldo bancário.

### 9.6 Pagamento da fatura

O pagamento da fatura gera a saída financeira na conta escolhida.

### 9.7 Transferência

Uma transferência entre contas:

- reduz a conta origem;
- aumenta a conta destino;
- não é receita;
- não é despesa.

### 9.8 Estorno

Um estorno não deve apagar o registro original.

O histórico da operação deve permanecer.

### 9.9 Cancelamento

Um cancelamento não deve apagar fisicamente o registro.

O registro deve permanecer disponível para histórico e auditoria básica.

---

## 10. Responsáveis pelas despesas

Na V1, despesas poderão possuir um responsável.

Os responsáveis são:

0 - Meu
1 - Giulia
2 - Ederson
3 - Elisiane

Esses responsáveis NÃO são usuários do sistema.

Não criar usuários automaticamente para eles.

O campo representa apenas a quem aquela despesa pertence para fins de organização e relatórios.

---

## 11. Cartões de crédito

Cartões devem possuir:

- nome/apelido;
- titular textual;
- limite;
- dia de fechamento;
- dia de vencimento;
- status;
- usuário.

O titular do cartão é textual.

Não é necessário criar uma conta de usuário para o titular.

Um cartão poderá ser utilizado para despesas de diferentes responsáveis.

---

## 12. Faturas

O sistema deve possuir controle real de faturas.

Uma fatura deve possuir:

- cartão;
- período;
- data de fechamento;
- data de vencimento;
- valor total;
- valor pago;
- saldo restante;
- status.

Uma fatura poderá ser:

- aberta;
- fechada;
- paga;
- parcialmente paga;
- vencida.

A implementação definitiva dos estados deve seguir as regras de negócio documentadas.

---

## 13. Parcelamentos

Parcelamentos são uma funcionalidade fundamental da V1.

Compras parceladas devem gerar automaticamente as parcelas futuras.

Cada parcela deve possuir:

- número;
- valor;
- vencimento;
- status;
- referência ao parcelamento;
- referência à fatura quando aplicável.

O sistema NÃO deve assumir que todas as parcelas possuem o mesmo valor.

O usuário deve poder editar individualmente o valor de cada parcela.

---

## 14. Parcelamento de fatura

O usuário poderá pagar apenas parte de uma fatura.

Exemplo:

Fatura: R$ 2.000,00

Pagamento: R$ 1.000,00

Saldo restante: R$ 1.000,00

O sistema deve manter a fatura parcialmente paga.

Posteriormente, o saldo poderá ser transformado em um parcelamento.

O parcelamento da fatura não deve apagar as despesas originais.

---

## 15. Despesas e status

Despesas devem possuir estados que permitam preservar o histórico.

Estados previstos:

- PENDENTE
- PAGA
- ESTORNADA
- CANCELADA

Não utilizar exclusão física para remover despesas financeiras efetivadas.

---

## 16. Número do boleto

Despesas podem possuir número de boleto.

O campo deve ser opcional.

O objetivo é permitir que o usuário copie o número do boleto diretamente do sistema quando for realizar o pagamento.

O sistema não precisa gerar boletos.

---

## 17. Testes

Testes automatizados são obrigatórios.

Devem existir testes principalmente para:

- autenticação;
- isolamento de usuários;
- contas;
- receitas;
- despesas;
- pagamentos;
- cartões;
- faturas;
- parcelamentos;
- pagamentos parciais;
- transferências;
- estornos;
- cancelamentos;
- metas;
- projeções;
- endpoints críticos.

Utilizar:

- JUnit;
- Mockito;
- Testcontainers quando testes de integração com PostgreSQL forem necessários.

---

## 18. API

A API deverá utilizar REST.

Prefixo inicial:

/api/v1

A API deve possuir documentação OpenAPI / Swagger.

Utilizar:

- DTOs;
- validação;
- HTTP status codes apropriados;
- respostas de erro padronizadas;
- autenticação;
- autorização.

Endpoints devem ser definidos conforme cada módulo for implementado.

Não criar todos os endpoints antecipadamente sem necessidade.

---

## 19. Frontend

O frontend será desenvolvido em Angular com TypeScript.

Utilizar Standalone Components.

Preferir recursos nativos do Angular quando forem suficientes.

Utilizar:

- Angular Router;
- HttpClient;
- Reactive Forms;
- Signals.

A arquitetura deve ser organizada por funcionalidades.

Exemplo:

core
shared
features/auth
features/dashboard
features/accounts
features/categories
features/incomes
features/expenses
features/cards
features/invoices
features/installments
features/goals
features/projections

---

## 20. Bibliotecas adicionais

Não adicionar bibliotecas sem necessidade.

Antes de adicionar uma biblioteca, explicar:

1. qual problema ela resolve;
2. se o Angular ou Java já possui solução nativa;
3. alternativas;
4. vantagens;
5. desvantagens;
6. impacto na manutenção;
7. recomendação.

Isso vale especialmente para tecnologias como:

- Zod;
- NgRx;
- bibliotecas de UI;
- bibliotecas de gráficos;
- bibliotecas de formulários;
- bibliotecas de validação.

A decisão deve priorizar aprendizado, simplicidade e boas práticas modernas.

---

## 21. Docker

O projeto deve ser preparado para execução local através de Docker.

O PostgreSQL deverá preferencialmente ser executado através do Docker Compose.

A aplicação deverá possuir configuração adequada para ambiente de desenvolvimento.

Não criar configuração de produção desnecessariamente complexa na V1.

---

## 22. Git e GitHub

O desenvolvimento será realizado no Cursor.

Os commits e pushes serão realizados manualmente pelo desenvolvedor através do VSCode.

A IA NÃO deve presumir que possui acesso ao GitHub.

A IA NÃO deve executar push.

A IA pode criar e modificar arquivos do projeto normalmente.

---

## 23. Gitignore

Nunca versionar:

- credenciais;
- arquivos .env;
- senhas;
- tokens;
- certificados privados;
- node_modules;
- target;
- dist;
- arquivos temporários;
- logs;
- arquivos gerados;
- configurações pessoais da IDE.

---

## 24. Documentação

A documentação principal deverá ficar dentro da pasta:

docs/

A documentação deve conter, conforme o projeto evoluir:

- requisitos;
- regras de negócio;
- arquitetura;
- modelo de domínio;
- API;
- stack tecnológica;
- roadmap;
- decisões arquiteturais.

---

## 25. Finalidade educacional

Este projeto também tem como objetivo aprendizado.

Sempre que uma decisão técnica importante for tomada, explicar de forma objetiva:

- o que está sendo feito;
- por que está sendo feito;
- quais alternativas existem;
- por que a solução escolhida é adequada ao projeto.

Não transformar cada alteração em uma aula extensa.

A explicação deve ser proporcional à complexidade da decisão.

---

## 26. Escopo da V1

A V1 deverá contemplar:

- usuários;
- autenticação;
- contas;
- categorias;
- receitas;
- despesas;
- cartões;
- faturas;
- parcelamentos;
- pagamentos;
- pagamentos parciais;
- transferências;
- estornos;
- cancelamentos;
- metas;
- projeções;
- dashboard;
- gráficos;
- relatórios;
- exportação de relatório de fatura;
- testes;
- Docker;
- PostgreSQL;
- migrations;
- Swagger/OpenAPI.

---

## 27. Fora da V1

Não implementar neste momento:

- investimentos;
- importação automática de extratos;
- integração bancária;
- notificações;
- deploy em produção;
- compartilhamento familiar;
- contas compartilhadas;
- automações bancárias;
- integrações externas.

A arquitetura pode ser preparada para futuras expansões, mas essas funcionalidades não devem ser implementadas agora.

---

## 28. Regra de parada

A IA deve parar e solicitar orientação quando:

- existir uma decisão de negócio não definida;
- houver conflito entre requisitos;
- uma alteração mudar significativamente o escopo;
- houver risco de perda de dados;
- uma regra financeira estiver ambígua;
- os testes não puderem ser corrigidos com segurança;
- uma biblioteca ou tecnologia adicional for necessária e ainda não tiver sido aprovada.

Não assumir decisões importantes de negócio.

---

## 29. Regra final

O objetivo não é apenas fazer o sistema funcionar.

O objetivo é construir uma aplicação:

- organizada;
- segura;
- testável;
- compreensível;
- moderna;
- extensível;
- adequada para aprendizado;
- com regras financeiras confiáveis.

Priorizar qualidade e clareza em vez de velocidade de implementação.