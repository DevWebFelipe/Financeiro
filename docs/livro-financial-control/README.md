# Livro técnico — Financial Control

Esta pasta contém a fonte editável e o PDF A4 do livro técnico do projeto.

## Organização

- `manuscrito/`: capítulos em Markdown, mantidos em ordem pelo prefixo numérico;
- `assets/`: estilos e recursos editoriais;
- `build/`: gerador local do HTML paginado e do PDF;
- `dist/`: artefatos finais.

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

## Regra editorial

Os rótulos abaixo são usados de forma estrita:

- **IMPLEMENTADO**: existe no código atual;
- **DECIDIDO**: consta do contrato oficial, mesmo que ainda não exista no código;
- **PLANEJADO**: aparece no roadmap como trabalho futuro;
- **PENDENTE**: depende de decisão ou representa lacuna ainda não resolvida.

