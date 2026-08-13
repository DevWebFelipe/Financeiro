# 🚀 Financial Control — Manual de Execução

Este guia apresenta os passos necessários para colocar o projeto **Financial Control** em execução localmente, realizar os testes e, ao finalizar, encerrar todos os serviços corretamente.

---

# ▶️ Iniciando o sistema

## 1. 🐘 Subir o PostgreSQL

Na **raiz do projeto**, abra um PowerShell e execute:

```powershell
docker compose up -d
```

O comando inicia os containers definidos no `docker-compose.yml`, incluindo o PostgreSQL.

> **Importante:** o Docker Desktop deve estar em execução antes de executar este comando.

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

Se a aplicação abrir normalmente, o ambiente está funcionando. ✅

---

# 🧪 Realizando os testes

Com os três componentes em execução:

```text
🐘 PostgreSQL
      ↓
⚙️ Backend
      ↓
🖥️ Frontend
      ↓
🌐 Navegador
```

Você pode realizar os testes funcionais da aplicação.

Durante os testes, mantenha os terminais do **Backend** e **Frontend** abertos.

---

# ⏹️ Encerrando o sistema

Depois de terminar os testes, é recomendado encerrar os serviços de forma organizada.

## 5. 🖥️ Parar o Frontend

No terminal onde o Angular está rodando, pressione:

```text
Ctrl + C
```

Isso encerra o servidor de desenvolvimento do Angular.

---

## 6. ⚙️ Parar o Backend

No terminal onde o Spring Boot está rodando, pressione:

```text
Ctrl + C
```

Isso encerra a aplicação Spring Boot.

---

## 7. 🐘 Parar o PostgreSQL / Docker

Volte para um terminal na **raiz do projeto** e execute:

```powershell
docker compose down
```

Isso encerra e remove os containers criados pelo `docker compose`.

### ⚠️ Importante sobre os dados do banco

Use:

```powershell
docker compose down
```

e **não**:

```powershell
docker compose down -v
```

O comando `down -v` também remove os volumes associados ao Docker e pode apagar os dados persistidos do PostgreSQL.

Portanto, para o uso normal durante o desenvolvimento, utilize apenas:

```powershell
docker compose down
```

---

# 🔄 Iniciar novamente depois

Quando quiser voltar a trabalhar no projeto, basta repetir o processo.

### 1. PostgreSQL

Na raiz:

```powershell
docker compose up -d
```

### 2. Backend

Em outro terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### 3. Frontend

Em outro terminal:

```powershell
cd frontend
npm start
```

### 4. Sistema

Abrir:

**http://localhost:4200**

---

# 📋 Resumo completo

## ▶️ Para iniciar

| Ordem | Componente    | Local      | Comando                      |
| ----- | ------------- | ---------- | ---------------------------- |
| 1     | 🐘 PostgreSQL | Raiz       | `docker compose up -d`       |
| 2     | ⚙️ Backend    | `backend`  | `.\mvnw.cmd spring-boot:run` |
| 3     | 🖥️ Frontend  | `frontend` | `npm start`                  |
| 4     | 🌐 Sistema    | Navegador  | `http://localhost:4200`      |

## ⏹️ Para encerrar

| Ordem | Componente    | Ação                  |
| ----- | ------------- | --------------------- |
| 1     | 🖥️ Frontend  | `Ctrl + C`            |
| 2     | ⚙️ Backend    | `Ctrl + C`            |
| 3     | 🐘 PostgreSQL | `docker compose down` |

---

# 🧠 Fluxo para o dia a dia

```text
                 INICIAR
                    │
                    ▼
          docker compose up -d
                    │
                    ▼
             Subir Backend
                    │
                    ▼
            Subir Frontend
                    │
                    ▼
          http://localhost:4200
                    │
                    ▼
                🧪 TESTAR
                    │
                    ▼
             Ctrl + C Frontend
                    │
                    ▼
              Ctrl + C Backend
                    │
                    ▼
          docker compose down
                    │
                    ▼
                 FINALIZADO
```

> **Regra prática:** sempre que terminar de trabalhar, pare primeiro o **Frontend**, depois o **Backend** e, por último, execute `docker compose down` para o PostgreSQL.

> **Regra de segurança:** evite `docker compose down -v` durante o desenvolvimento, a menos que você tenha certeza de que deseja remover os volumes e os dados persistidos do banco.
