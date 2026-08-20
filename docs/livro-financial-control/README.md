# Livro técnico — Financial Control

Esta pasta contém a fonte editável e o PDF A4 do livro técnico do projeto.

## Organização

- `manuscrito/`: capítulos em Markdown, mantidos em ordem pelo prefixo numérico;
- `assets/book.css`: estilos A4 e regras de impressão;
- `build/build-book.ps1`: entrada PowerShell;
- `build/render-book.cjs`: montagem, paginação e geração via Playwright;
- `dist/`: HTML, PDF, relatório de validação e imagens de inspeção.

O livro documenta o estado observado no repositório. Divergências entre código,
migrations, testes e documentação são preservadas no capítulo de auditoria, sem
alterar o sistema para escondê-las.

## Gerar o livro no Windows

Pré-requisitos: Node.js e as dependências do frontend já instaladas.

```powershell
cd D:\Financeiro
powershell.exe -ExecutionPolicy Bypass -File .\docs\livro-financial-control\build\build-book.ps1
```

Saídas:

- `dist\financial-control-livro.html`
- `dist\financial-control-livro.pdf`

O gerador utiliza o Chromium do Playwright já adotado pelo frontend. Ele não
adiciona dependências ao backend nem ao frontend.

O script localiza a raiz do repositório a partir de sua própria pasta, descobre
todos os arquivos `manuscrito/*.md`, aplica ordenação numérica pelo nome e falha
se o manuscrito ou o Playwright não estiverem disponíveis.

## O que é validado

A geração reprova quando encontra:

- PDF vazio, sem cabeçalho `%PDF` ou sem marcador final;
- diferença entre a quantidade de páginas HTML e PDF;
- página de conteúdo completamente vazia;
- conteúdo vertical ou horizontal fora da área útil;
- tabela ou bloco de código ultrapassando as margens;
- título de subseção isolado no final de uma página.

O relatório fica em `dist/validation-report.json`. Imagens renderizadas do
início, meio e final ficam em `dist/inspection/`.

O sumário é criado a partir dos títulos reais e recebe os números calculados
depois da paginação do conteúdo. O índice remissivo usa uma lista editorial de
termos e registra somente páginas em que cada termo realmente ocorre.

## Escopo atual do manuscrito

O gerador está completo e utiliza todos os capítulos existentes, mas o
manuscrito disponível nesta entrega contém atualmente dois capítulos:

1. `05-backend-spring.md`;
2. `09-banco-flyway.md`.

Portanto, o PDF atual é um volume parcial do livro mestre, não a documentação
integral originalmente planejada para todas as partes do Financial Control.

## Regra editorial

Os rótulos abaixo são usados de forma estrita:

- **IMPLEMENTADO**: existe no código atual;
- **DECIDIDO**: consta do contrato oficial, mesmo que ainda não exista no código;
- **PLANEJADO**: aparece no roadmap como trabalho futuro;
- **PENDENTE**: depende de decisão ou representa lacuna ainda não resolvida.

