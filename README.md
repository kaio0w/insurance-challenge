# Insurance Challenge API

API principal responsável por gerenciar **parceiros, cotações e apólices**, integrando-se com uma API externa de seguradora.

Projeto desenvolvido em **Clojure**, utilizando **Ring + Reitit + Malli + Muuntaja**, com integração HTTP via **clj-http**.

---

## 🏗 Arquitetura

A aplicação foi estruturada em camadas:

```
src/
 ├── clients/        -> integração com seguradora
 ├── handlers/       -> camada HTTP (Ring)
 ├── services/       -> regras de negócio
 ├── middleware/     -> rate limit + security headers
 ├── schemas/        -> validação com Malli
 ├── store/          -> persistência in-memory
 └── routes.clj      -> definição das rotas
```

Separação clara de responsabilidades:

- **Handlers**: apenas traduzem HTTP ↔ Service
- **Services**: regras de negócio e integração externa
- **Clients**: chamadas HTTP à seguradora
- **Middleware**: segurança e proteção

---

## 🚀 Como executar

### Pré-requisitos

- Docker
- Docker Compose

---

### Subir tudo (API + Seguradora)

```bash
docker compose up --build
```

A aplicação ficará disponível em:

```
http://localhost:3000
```

Swagger:

```
http://localhost:3000/swagger/
```

---

## 🔑 Variáveis de ambiente

A aplicação utiliza:

```bash
INSURER_API_KEY=
INSURER_BASE_URL=http://insurer:5000
```

No `docker-compose.yml`, essas variáveis já estão configuradas.

---

## 📡 Endpoints

### Health Check

```
GET /health
```

Resposta:

```json
{ "status": "ok" }
```

---

### Criar Parceiro

```
POST /partners
```

Body:

```json
{
  "name": "XPTO",
  "cnpj": "12345678901234"
}
```

---

### Criar Cotação

```
POST /partners/{partner-id}/quotes
```

Body:

```json
{
  "age": 30,
  "sex": "m"
}
```

Integra com API da seguradora `/api/quotations`.

Retorna:

```json
{
  "id": "...",
  "partner-id": "...",
  "age": 30,
  "sex": "m",
  "price": 123.45,
  "expire-at": "2026-03-15"
}
```

---

### Criar Apólice

```
POST /partners/{partner-id}/policies
```

Body:

```json
{
  "quotation_id": "UUID_DA_COTACAO",
  "name": "Joao da Silva",
  "sex": "m",
  "date_of_birth": "1990-01-01"
}
```

Integra com API da seguradora `/api/policies`.

---

### Buscar Apólice

```
GET /partners/{partner-id}/policies/{policy-id}
```

---

## 🔐 Segurança

A API implementa:

### ✅ Variáveis de ambiente para secrets
Nenhuma chave sensível é versionada.

### ✅ Timeouts HTTP
Todas as chamadas à seguradora possuem:

- `conn-timeout: 2000ms`
- `socket-timeout: 5000ms`

Evita travamentos.

### ✅ Tratamento estruturado de erros
Mapeamento correto de:
- 400
- 404
- 502 (falha externa)

Sem vazamento de detalhes internos.

### ✅ Rate limiting
Middleware simples in-memory:
- 60 requisições por minuto por IP
- Swagger e health são ignorados

### ✅ Security headers

A aplicação retorna:

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store
```

### ✅ Default handler
Rotas inexistentes retornam 404 (evitando NPE).

---

## 🧪 Testes

Executar:

```bash
clojure -M:test
```

Cobertura inclui:

- Validações de entrada
- Fluxo completo de quote
- Fluxo completo de policy
- Mock da seguradora
- Persistência in-memory
- Mapeamento de erros externos

---

## 🐳 Docker

### Build manual

```bash
docker build -t insurance-challenge .
```

### Execução manual

```bash
docker run -p 3000:3000 insurance-challenge
```

---


## 📌 Conclusão

A API foi construída com foco em:

- Separação clara de responsabilidades
- Integração resiliente com serviço externo
- Tratamento consistente de erros
- Segurança básica aplicada
- Testabilidade
- Executável via Docker
