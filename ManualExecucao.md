# 🚀 Financial Control — Manual de Execução

Este guia apresenta os passos necessários para colocar o projeto **Financial Control** em execução localmente.

---

## 1. 🐘 Subir o PostgreSQL

Na **raiz do projeto**, abra um PowerShell e execute:

```powershell
docker compose up -d
```

O comando inicia os containers definidos no `docker-compose.yml`, incluindo o PostgreSQL.

> **Importante:** o Docker deve estar em execução antes de executar este comando.

---

## 2. ⚙️ Subir o Backend

Abra **um novo terminal** e navegue até a pasta `backend`:

```powershell
cd backend
```

Depois, inicie a aplicação Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

Mantenha este terminal **aberto enquanto estiver utilizando o sistema**.

### 🔎 Testar o Backend

Com o backend iniciado, abra o navegador e acesse:

```text
http://localhost:8080/api/v1/health
```

A resposta esperada é:

```json
{
  "status": "UP"
}
```

Se essa resposta aparecer, o backend está funcionando corretamente. ✅

---

## 3. 🖥️ Subir o Frontend

Abra **outro PowerShell** e navegue até a pasta `frontend`:

```powershell
cd frontend
```

Execute o Angular:

```powershell
npm start
```

### ⚠️ Caso o Node.js não esteja no PATH

Se o comando `npm` não for reconhecido, utilize diretamente o caminho do Node.js configurado no ambiente:

```powershell
C:\Dev\node-v22.23.2-win-x64\npm.cmd start
```

Aguarde o Angular concluir a inicialização.

---

## 4. 🌐 Acessar o sistema

Depois que o frontend estiver iniciado, abra o navegador e acesse:

**http://localhost:4200**

---

# 🔄 Resumo da inicialização

Para facilitar, a ordem correta é:

```text
1. Docker / PostgreSQL
        ↓
2. Backend / Spring Boot
        ↓
3. Frontend / Angular
        ↓
4. Navegador
```

### Terminais necessários

| Terminal      | Pasta           | Comando                      |
| ------------- | --------------- | ---------------------------- |
| 🐘 PostgreSQL | Raiz do projeto | `docker compose up -d`       |
| ⚙️ Backend    | `backend`       | `.\mvnw.cmd spring-boot:run` |
| 🖥️ Frontend  | `frontend`      | `npm start`                  |

### ✅ Sistema funcionando quando:

* PostgreSQL está rodando no Docker.
* Backend responde `{"status":"UP"}` em `localhost:8080`.
* Angular está rodando em `localhost:4200`.
* O sistema abre normalmente no navegador.

> **Dica:** mantenha os terminais do backend e frontend abertos durante a utilização do sistema. Para encerrar cada aplicação, utilize `Ctrl + C` no respectivo terminal.
