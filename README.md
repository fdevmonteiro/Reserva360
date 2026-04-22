# Reserva360

Sistema de reserva de salas corporativas com autenticação via Active Directory, integração com Zoom e Microsoft Graph.

---

## Estrutura do repositório

```
Reserva360/
├── apps/
│   ├── reserva-frontend/   # React + Vite + TypeScript
│   └── reserva-backend/    # Spring Boot 3 + Java 21
```

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Node.js | 20.x |
| npm | 10.x |
| Java | 21 |
| Maven Wrapper | incluso (`./mvnw`) |

---

## Configuração do backend

O backend usa variáveis de ambiente para todos os valores sensíveis. O arquivo `application.properties` **não contém credenciais** e é seguro para o repositório.

### 1. Copie o arquivo de exemplo

```bash
cp apps/reserva-backend/src/main/resources/application.properties.example \
   apps/reserva-backend/src/main/resources/application.properties
```

> `application.properties` está no `.gitignore` — nunca será commitado.

### 2. Preencha as variáveis de ambiente

Defina as variáveis abaixo no seu ambiente antes de rodar a aplicação (shell, `.env`, pipeline, etc.):

#### Banco de dados (MySQL)

| Variável | Descrição | Exemplo |
|---|---|---|
| `DB_URL` | JDBC URL do MySQL | `jdbc:mysql://HOST:3306/reserva_sala?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | Usuário do banco | `user_reserva_sala` |
| `DB_PASSWORD` | Senha do banco | — |

#### E-mail (SMTP Office 365)

| Variável | Descrição |
|---|---|
| `MAIL_USERNAME` | E-mail remetente |
| `MAIL_PASSWORD` | Senha do e-mail |

#### JWT

| Variável | Descrição |
|---|---|
| `JWT_SECRET` | Chave secreta (mínimo 256 bits) |

#### LDAP / Active Directory

| Variável | Descrição | Exemplo |
|---|---|---|
| `LDAP_URL` | URL do controlador de domínio | `ldap://DC-01.dominio.com.br:389` |
| `LDAP_BASE` | Base DN | `DC=dominio,DC=com,DC=br` |
| `LDAP_USERNAME` | DN da service account | `CN=svc,OU=ServiceUsers,...` |
| `LDAP_PASSWORD` | Senha da service account | — |

#### Zoom

| Variável | Descrição |
|---|---|
| `ZOOM_ACCOUNT_ID` | Account ID da app Zoom |
| `ZOOM_CLIENT_ID` | Client ID da app Zoom |
| `ZOOM_CLIENT_SECRET` | Client Secret da app Zoom |
| `ZOOM_WEBHOOK_TOKEN` | Token de verificação do webhook (pode ser vazio) |

#### Microsoft Graph

| Variável | Descrição |
|---|---|
| `MS_TENANT_ID` | Tenant ID do Azure AD |
| `MS_CLIENT_ID` | Client ID do app registration |
| `MS_CLIENT_SECRET` | Client Secret do app registration |

#### URLs da aplicação

| Variável | Descrição | Exemplo |
|---|---|---|
| `APP_BASE_URL` | URL base do backend | `http://localhost:8080` |
| `APP_FRONTEND_URL` | URL do frontend | `http://localhost:5173` |
| `APP_CORS_ORIGINS` | Origins permitidas (separadas por vírgula) | `http://localhost:3000,http://localhost:5173` |

### 3. Rodando o backend

```bash
# Build
npm run nx -- build reserva-backend

# Rodar em desenvolvimento
npm run nx -- serve reserva-backend

# Ou diretamente com Maven
cd apps/reserva-backend
./mvnw spring-boot:run
```

A aplicação sobe na porta **8080** por padrão. O console do H2 (se configurado) fica em `/h2-console`.

---

## Configuração do frontend

```bash
# Instalar dependências
npm install

# Rodar em desenvolvimento (porta 5173)
npm run nx -- serve reserva-frontend

# Build de produção
npm run nx -- build reserva-frontend
```

---

## Rodando tudo junto

```bash
# Backend e frontend em paralelo
npm run nx -- run-many -t serve -p reserva-backend,reserva-frontend
```

---

## Pipeline (Azure DevOps)

As variáveis sensíveis devem ser cadastradas como **Secret Variables** no grupo de variáveis do pipeline e injetadas como variáveis de ambiente na etapa de execução do backend. Consulte a equipe de infraestrutura para obter os valores de produção/homologação.

---

## Segurança

- `application.properties` está no `.gitignore` — nunca commite com credenciais reais
- Use o arquivo `application.properties.example` como referência para novos ambientes
- Credenciais de produção são gerenciadas exclusivamente pelo time de infraestrutura
