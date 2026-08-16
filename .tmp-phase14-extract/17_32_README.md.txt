PATH: d:\Financeiro\README.md

NEW:
| Transferência | Atômica; só `BANK_ACCOUNT`; status `ACTIVE`/`REVERSED`; não é receita/despesa; sem saldo insuficiente (criação e reversão) |
| Acerto de Saldos | Fato `BALANCE_ADJUSTMENT`; usuário informa saldo real; diferença calculada; `BANK_ACCOUNT` e `CASH` |
| Saldo | Derivado de movimentações, a partir do saldo inicial; sem `current_balance` como fonte de verdade; Fase 14 estende com transfers/acertos ACTIVE |