# Postman

## Importação

1. Importe a Collection.
2. Importe o Environment.
3. Selecione `Financial Control - Local`.
4. Suba o backend.
5. Execute `Health`.
6. Execute `Usuários → Criar`.
7. Execute `Autenticação → Login`.

O login salva automaticamente o JWT em `accessToken`.

O cadastro de conta salva automaticamente o `accountId`.

As demais requests utilizam essas variáveis automaticamente.