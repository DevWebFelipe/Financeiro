# Roadmap — Financial Control

## 0. Hierarquia

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

A IA não deve implementar fases futuras sem autorização explícita.


## 1. Objetivo

Este documento define a ordem de desenvolvimento do Financial Control.

O projeto será desenvolvido de forma incremental.

Nenhuma fase deve tentar implementar todo o sistema de uma única vez.


# 2. Princípio

Cada etapa deve:

1. possuir objetivo claro;
2. possuir escopo limitado;
3. possuir critérios de conclusão;
4. possuir testes;
5. ser validada antes da próxima etapa.


# 3. Regra principal

A IA não deve implementar fases futuras sem autorização explícita.


# 4. Regra

Antes de iniciar uma fase:

1. ler AGENTS.md;
2. ler documentação relacionada;
3. analisar código existente;
4. verificar dependências;
5. apresentar plano da fase;
6. aguardar aprovação quando solicitado.


# 5. Fase 0 — Planejamento

Status:

CONCLUÍDA


Objetivo:

Definir requisitos e arquitetura inicial.


Documentos:

AGENTS.md

docs/20-fluxos-financeiros.md até docs/28-roadmap.md

docs/CODING_STANDARDS.md

README.md


Resultado esperado:

Base documental do projeto consolidada (hierarquia AGENTS → docs/20–28 → CODING_STANDARDS → .cursor/rules).


# 6. Fase 1 — Estrutura inicial

Objetivo:

Criar a estrutura base do projeto.


Escopo:

- backend Java;
- frontend Angular;
- Docker;
- PostgreSQL;
- Git;
- configurações;
- documentação.


Não implementar funcionalidades financeiras ainda.


# 7. Fase 1 — Backend

Criar:

- projeto Java;
- estrutura de pacotes;
- configuração;
- dependências;
- health check;
- conexão com PostgreSQL.


# 8. Fase 1 — Frontend

Criar:

- projeto Angular;
- estrutura inicial;
- configuração;
- layout base;
- roteamento.


Não implementar dashboard financeiro ainda.


# 9. Fase 1 — Docker

Criar:

- PostgreSQL;
- backend;
- frontend quando aplicável.


Objetivo:

Permitir execução local padronizada.


# 10. Fase 1 — Git

Criar:

.gitignore

.cursorignore


Garantir que arquivos sensíveis não sejam versionados.


# 11. Critério de conclusão da Fase 1

Deve ser possível:

1. subir PostgreSQL;
2. iniciar backend;
3. iniciar frontend;
4. acessar health check;
5. conectar backend ao banco;
6. executar projeto localmente.


# 12. Fase 2 — Banco de dados

Objetivo:

Criar estrutura inicial do PostgreSQL.


Bloqueios oficiais (não migrar nem implementar a parte dependente até decisão):

- `payments.type` (`docs/23` §269.1);
- edição de parcela futura × `expenses.total_amount` (`docs/23` §269.2) — não bloqueia o schema da parcela; bloqueia a regra de edição;
- pagamento parcial da fatura × status/rateio das parcelas (`docs/23` §269.3) — não bloqueia a tabela de pagamentos da fatura; bloqueia rateio e efeito sobre parcelas.

Governança: `AGENTS.md` seção 28. O modelo já consolidado permanece fonte de verdade.


Escopo:

- migrations;
- extensões necessárias;
- tabelas;
- constraints;
- índices;
- relacionamentos.


# 13. Fase 2

Prioridade:

users

accounts

categories

credit_cards

incomes

expenses

expense_installments

payments

transfers

credit_card_invoices

credit_card_invoice_payments

credit_card_invoice_installments

financial_goals

goal_contributions


# 14. Banco

Não criar tabelas desnecessárias para funcionalidades futuras.


# 15. Banco

Preparar arquitetura para evolução sem implementar funcionalidades futuras.


# 16. Fase 2 — Migrations

Utilizar ferramenta de migration apropriada ao stack escolhido.


# 17. Fase 2 — Seeds

Criar dados iniciais somente quando necessário.


# 18. Critério de conclusão

Banco deve:

- subir;
- executar migrations;
- possuir constraints;
- possuir relacionamentos;
- permitir rollback quando suportado pela estratégia;
- possuir estrutura documentada.


# 19. Fase 3 — Autenticação

Objetivo:

Implementar:

- cadastro;
- login;
- senha;
- JWT;
- autenticação;
- autorização.


# 20. Fase 3

Implementar:

POST /api/v1/auth/login


GET /api/v1/users/me


PUT /api/v1/users/me


# 21. Fase 3 — Segurança

Implementar:

- password hashing;
- JWT;
- proteção de endpoints;
- isolamento por usuário.


# 22. Fase 3 — Testes

Testar:

- login;
- senha;
- JWT;
- usuário desativado;
- acesso sem autenticação;
- acesso entre usuários.


# 23. Critério de conclusão

Usuário consegue:

1. criar conta;
2. fazer login;
3. obter token;
4. acessar perfil;
5. acessar somente seus dados.


# 24. Fase 4 — Contas

Objetivo:

Implementar contas financeiras.


# 25. Fase 4

Implementar:

- criar conta;
- editar;
- listar;
- consultar;
- ativar;
- desativar;
- saldo.


# 26. Fase 4

Tipos:

BANK_ACCOUNT

CASH


# 27. Fase 4

Implementar:

saldo inicial;

movimentações.


# 28. Testes

Testar:

- saldo;
- entrada;
- saída;
- conta desativada;
- isolamento.


# 29. Critério de conclusão

Usuário consegue cadastrar todas as contas pessoais necessárias.


# 30. Fase 5 — Categorias

Objetivo:

Implementar categorias de receitas e despesas.


# 31. Fase 5

Implementar:

- criar;
- editar;
- listar;
- desativar.


# 32. Critério

Categorias devem possuir tipo:

INCOME

EXPENSE


# 33. Critério

Categorias desativadas não podem ser utilizadas em novos lançamentos.


# 34. Fase 6 — Receitas

Objetivo:

Implementar controle de receitas.


# 35. Fase 6

Implementar:

- criação;
- edição;
- consulta;
- recebimento;
- cancelamento.


# 36. Estados

EXPECTED

RECEIVED

CANCELLED


# 37. Fase 6

Implementar vínculo com:

- categoria;
- conta;
- responsável.


# 38. Testes

Testar:

- receita prevista;
- receita recebida;
- receita cancelada;
- impacto no saldo.


# 39. Critério de conclusão

Usuário consegue lançar:

- salário;
- freelance;
- outras receitas.


# 40. Fase 7 — Despesas simples

Objetivo:

Implementar despesas sem parcelamento inicialmente.


# 41. Fase 7

Formas:

ACCOUNT

NONE


# 42. Fase 7

Implementar:

- criação;
- edição;
- consulta;
- pagamento;
- cancelamento;
- estorno.


# 43. Fase 7

Implementar:

- responsável;
- boleto;
- categoria;
- vencimento.


# 44. Estados

Status persistidos:

OPEN

PARTIALLY_PAID

PAID

CANCELLED

REFUNDED


OVERDUE: derivado (não persistido).


# 45. Testes

Testar:

- pagamento integral;
- pagamento parcial;
- múltiplos pagamentos;
- cancelamento;
- estorno.


# 46. Critério de conclusão

Usuário consegue controlar despesas simples sem cartão.


# 47. Fase 8 — Parcelamento de despesas

Objetivo:

Implementar despesas parceladas.


# 48. Fase 8

Implementar:

- quantidade de parcelas;
- geração automática;
- datas futuras;
- valores individuais.


# 49. Fase 8

Permitir editar parcelas abertas.


# 50. Fase 8

Permitir valores diferentes por parcela.


# 51. Fase 8

Garantir:

soma das parcelas = total.


# 52. Testes

Testar:

- 1 parcela;
- 3 parcelas;
- 12 parcelas;
- valores diferentes;
- arredondamento;
- edição.


# 53. Critério de conclusão

Uma compra parcelada gera automaticamente todos os compromissos futuros.


# 54. Fase 9 — Cartões

Objetivo:

Implementar cartões de crédito.


# 55. Fase 9

Implementar:

- cadastro;
- edição;
- limite;
- fechamento;
- vencimento;
- ativação;
- desativação.


# 56. Fase 9

Não implementar ainda:

- fatura completa.


Primeiro preparar o domínio do cartão.


# 57. Fase 10 — Compras no cartão

Objetivo:

Permitir despesas com cartão.


# 58. Fase 10

Implementar:

- compra;
- parcelamento;
- vínculo com cartão;
- determinação do ciclo.


# 59. Fase 10

Compra deve gerar parcelas futuras.


# 60. Fase 10

Parcelas devem ser associadas às respectivas faturas.


# 61. Fase 10

Testar:

- compra antes do fechamento (ciclo atual);
- compra depois do fechamento (próximo ciclo);
- compra no dia do fechamento (próxima fatura — RN095);
- dia 31 em mês curto (último dia do mês — RN098);
- compra parcelada.


# 62. Fase 11 — Faturas

Objetivo:

Implementar controle completo de faturas.


# 63. Fase 11

Implementar:

- geração;
- abertura;
- fechamento;
- vencimento;
- consulta;
- itens;
- total.


# 64. Fase 11

Status persistidos:

OPEN

CLOSED

PARTIALLY_PAID

PAID


OVERDUE: derivado da data de vencimento (não persistido).


# 65. Fase 11

Implementar cálculo de:

- total;
- pago;
- restante.


# 66. Fase 11

Testar fechamento e vencimento.


# 67. Critério de conclusão

Usuário consegue abrir uma fatura e visualizar exatamente o que deverá pagar.


# 68. Fase 12 — Pagamento de fatura

Objetivo:

Permitir pagamento integral ou parcial.


# 69. Fase 12

Implementar:

- pagamento;
- pagamento parcial;
- múltiplos pagamentos;
- conta utilizada.


# 70. Fase 12

Pagamento da fatura deve reduzir o saldo da conta.


# 71. Fase 12

Pagamento não deve duplicar despesas.


# 72. Testes

Testar:

- pagamento integral;
- pagamento parcial;
- pagamento múltiplo;
- saldo insuficiente;
- concorrência.


# 73. Fase 13 — Parcelamento de fatura

Objetivo:

Permitir parcelar saldo restante da fatura.


# 74. Fase 13

Implementar:

- saldo restante;
- criação de parcelas;
- valores diferentes;
- vencimentos futuros.


# 75. Fase 13

Parcelamento de fatura deve ser tratado como domínio separado de compra parcelada.


# 76. Fase 13

Criar testes específicos.


# 77. Fase 14 — Transferências

Objetivo:

Permitir transferência entre contas.


# 78. Fase 14

Implementar:

- origem;
- destino;
- valor;
- data;
- descrição.


# 79. Fase 14

Transferência deve ser atômica.


# 80. Testes

Testar:

- saldo;
- contas diferentes;
- saldo insuficiente;
- rollback;
- isolamento.


# 81. Critério de conclusão

Usuário consegue movimentar dinheiro entre suas próprias contas.


# 82. Fase 15 — Metas

Objetivo:

Implementar metas financeiras.


# 83. Fase 15

Implementar:

- criação;
- edição;
- contribuição;
- acompanhamento;
- conclusão.


# 84. Fase 15

Permitir:

valor alvo;

data alvo;

valor acumulado.


# 85. Testes

Testar:

- contribuição;
- saldo;
- conclusão;
- valores inválidos.


# 86. Fase 16 — Contas a pagar

Objetivo:

Criar visão consolidada de obrigações.


# 87. Fase 16

Exibir:

- despesas abertas;
- despesas vencidas;
- parcelas futuras;
- faturas.


# 88. Fase 16

Permitir filtros por:

- período;
- categoria;
- responsável;
- cartão;
- status.


# 89. Critério

Usuário deve conseguir responder:

"Quanto tenho para pagar?"


# 90. Fase 17 — Contas a receber

Objetivo:

Criar visão consolidada de receitas previstas.


# 91. Fase 17

Exibir:

- receitas esperadas;
- receitas vencidas;
- receitas recebidas.


# 92. Critério

Usuário deve conseguir responder:

"Quanto tenho para receber?"


# 93. Fase 18 — Projeções

Objetivo:

Criar planejamento financeiro futuro.


# 94. Fase 18

Permitir consultar:

- mês;
- trimestre;
- vários meses.


# 95. Fase 18

Considerar:

- saldo atual;
- receitas futuras;
- despesas futuras;
- parcelas;
- faturas;
- transferências quando aplicável.


# 96. Fase 18

Não considerar:

- limite de cartão como dinheiro;
- despesas canceladas;
- receitas canceladas.


# 97. Critério

Usuário deve conseguir responder:

"Quanto dinheiro provavelmente terei em dezembro?"


# 98. Fase 19 — Dashboard

Objetivo:

Criar visão geral financeira.


# 99. Dashboard

Exibir:

- saldo total;
- receitas;
- despesas;
- faturas;
- contas a pagar;
- contas a receber;
- projeção.


# 100. Dashboard

Criar gráficos:

- despesas por categoria;
- despesas por cartão;
- despesas por responsável;
- receitas;
- fluxo de caixa.


# 101. Fase 20 — Relatórios

Objetivo:

Criar relatórios financeiros.


# 102. Relatórios

Implementar:

- fatura;
- despesas;
- receitas;
- fluxo de caixa;
- categorias;
- responsáveis;
- cartões.


# 103. Fase 20

Exportação inicial:

PDF


# 104. Fase 20

Relatório de fatura deve ser adequado para enviar ao proprietário do cartão.


# 105. Fase 21 — Frontend completo

Objetivo:

Integrar todas as funcionalidades ao Angular.


# 106. Frontend

Criar telas:

- login;
- dashboard;
- contas;
- cartões;
- categorias;
- receitas;
- despesas;
- faturas;
- transferências;
- metas;
- contas a pagar;
- contas a receber;
- projeções;
- relatórios.


# 107. Frontend

Priorizar experiência de uso.


# 108. Frontend

Formulários devem possuir:

- validação;
- mensagens claras;
- estados de carregamento;
- tratamento de erro.


# 109. Frontend

Não duplicar regras financeiras críticas.


# 110. Fase 22 — Testes E2E

Objetivo:

Validar os principais fluxos através da aplicação completa.


# 111. Fluxos prioritários

1. Login.
2. Cadastro de conta.
3. Cadastro de categoria.
4. Receita.
5. Despesa.
6. Compra parcelada.
7. Fatura.
8. Pagamento de fatura.
9. Transferência.
10. Projeção.


# 112. Fase 23 — Qualidade

Objetivo:

Revisar todo o sistema.


# 113. Revisão

Verificar:

- segurança;
- performance;
- arquitetura;
- código duplicado;
- validações;
- testes;
- documentação.


# 114. Revisão

Executar:

lint;

format;

testes;

build.


# 115. Fase 24 — Documentação

Atualizar:

README.md

AGENTS.md

docs/


# 116. Fase 25 — Preparação futura

Somente após V1 estável considerar:

- investimentos;
- importação bancária;
- notificações;
- múltiplos formatos de exportação;
- dark mode;
- PWA;
- deploy;
- CI/CD;
- auditoria avançada.


# 117. Regra

Funcionalidades futuras não devem entrar na V1 apenas porque são tecnicamente interessantes.


# 118. Prioridade

A prioridade é:

estabilidade;

correção;

segurança;

aprendizado;

experiência de uso.


# 119. IA

A IA deve trabalhar somente na fase autorizada.


# 120. IA

Não implementar automaticamente a próxima fase ao terminar a atual.


# 121. IA

Ao terminar uma fase:

1. executar testes;
2. verificar build;
3. verificar documentação;
4. apresentar resumo;
5. listar arquivos alterados;
6. listar decisões;
7. aguardar autorização.


# 122. Git

Cada fase relevante deve resultar em commits organizados.


# 123. Commits

Preferir commits pequenos e semânticos.


# 124. Exemplos

feat: add account management

feat: add expense installments

feat: add credit card invoices

test: add invoice payment tests

fix: correct installment rounding


# 125. Git

Não fazer commits gigantes com dezenas de funcionalidades sem relação.


# 126. Cursor

O desenvolvimento será realizado no Cursor.


# 127. GitHub

Os commits serão realizados pelo VSCode.


# 128. GitHub

O Cursor não precisa possuir acesso à conta pessoal do GitHub.


# 129. Fluxo

Cursor:

desenvolvimento.


VSCode:

revisão;

commit;

push.


# 130. Regra

Antes do commit:

1. revisar diff;
2. executar testes;
3. verificar arquivos sensíveis;
4. verificar documentação;
5. confirmar escopo.


# 131. Consumo de IA

Evitar prompts gigantes durante implementação.


# 132. Estratégia

Solicitar:

uma funcionalidade;

um domínio;

ou uma etapa pequena.


# 133. Exemplo

Bom:

"Implemente o CRUD de contas seguindo AGENTS.md, docs de regras e testes."


Ruim:

"Implemente todo o sistema financeiro."


# 134. IA

Antes de gerar código:

a IA deve analisar a documentação existente.


# 135. IA

Se encontrar inconsistência:

não inventar solução silenciosamente.


# 136. IA

Deve informar:

- problema;
- impacto;
- alternativas;
- recomendação.


# 137. Aprovação

Mudanças arquiteturais relevantes exigem aprovação.


# 138. Arquitetura

Não alterar stack sem aprovação explícita.


# 139. Tecnologias

Não adicionar bibliotecas importantes sem justificar.


# 140. Dependências

Toda nova dependência deve responder:

1. Por que precisamos?
2. Existe alternativa nativa?
3. Qual o custo?
4. Qual o impacto?
5. É mantida?


# 141. Frontend

Não adicionar biblioteca somente para resolver problema simples que Angular já resolve.


# 142. Backend

Não adicionar framework adicional sem necessidade.


# 143. Banco

Não adicionar banco secundário na V1.


# 144. Arquitetura

V1:

Angular

↓

Java

↓

PostgreSQL


# 145. Docker

Docker deve facilitar o ambiente, não esconder problemas arquiteturais.


# 146. Desenvolvimento

A aplicação deve continuar compreensível para o desenvolvedor humano.


# 147. IA

Código gerado por IA deve ser revisável.


# 148. IA

Não aceitar código que o desenvolvedor não consiga explicar.


# 149. Aprendizado

Sempre que uma tecnologia importante for introduzida:

a IA deve explicar brevemente:

- o que é;
- por que está sendo usada;
- como funciona no projeto.


# 150. Aprendizado

A documentação não deve ser excessivamente acadêmica.


# 151. Objetivo

O projeto também será utilizado como projeto de aprendizado.


# 152. V1

V1 deve ser suficientemente completa para uso pessoal real.


# 153. V1

V1 não precisa ser uma plataforma financeira empresarial.


# 154. Critério de sucesso

A V1 deve permitir:

1. cadastrar usuários;
2. cadastrar contas;
3. cadastrar cartões;
4. cadastrar categorias;
5. cadastrar receitas;
6. cadastrar despesas;
7. parcelar despesas;
8. editar parcelas;
9. controlar faturas;
10. pagar faturas;
11. pagar parcialmente;
12. parcelar saldo de fatura;
13. realizar transferências;
14. cadastrar metas;
15. consultar contas a pagar;
16. consultar contas a receber;
17. projetar meses futuros;
18. visualizar dashboard;
19. gerar relatórios;
20. exportar fatura em PDF.


# 155. Critério de sucesso

O sistema deve responder claramente:

Quanto tenho?

Quanto vou receber?

Quanto tenho para pagar?

Quanto devo nos cartões?

Quanto terei para pagar nos próximos meses?

Quanto posso gastar?


# 156. Regra final

A V1 deve priorizar controle financeiro confiável.


# 157. Regra final

É preferível uma V1 menor e correta do que uma V1 grande e instável.


# 158. Regra final

Não avançar de fase sem validar a anterior.


# 159. Regra final

Documentação, código e testes devem evoluir juntos.


# 160. Regra final

O roadmap pode ser alterado conforme o projeto evoluir.


# 161. Regra

Alterações significativas no roadmap devem ser documentadas.


# 162. Fim da V1

Após a Fase 24:

V1 CONCLUÍDA


# 163. Pós-V1

Somente após a V1 estar estável avaliar novas funcionalidades.


# 164. Próxima etapa

Após este documento:

configurar arquivos do projeto;

validar arquitetura;

criar estrutura inicial;

iniciar Fase 1.