# API Endpoints - Sistema de Agendamento

Base URL: `http://localhost:8080`

---

## 📅 APPOINTMENTS (Agendamentos)

### 1. Criar Agendamento
**POST** `/appointments`

Cria um novo agendamento com um ou mais serviços.

**Request Body:**
```json
{
  "serviceIds": [
    "123e4567-e89b-12d3-a456-426614174000",
    "223e4567-e89b-12d3-a456-426614174001"
  ],
  "date": "2026-01-15",
  "startTime": "14:00",
  "userName": "Maria Silva",
  "userPhone": "5511999887766",
  "clienteId": "KC"
}
```

**Campos:**
- `serviceIds` (array de UUID, obrigatório): IDs dos serviços a serem agendados
- `date` (string, obrigatório): Data no formato `YYYY-MM-DD`
- `startTime` (string, obrigatório): Horário de início no formato `HH:mm`
- `userName` (string, obrigatório): Nome do cliente
- `userPhone` (string, obrigatório): Telefone com código do país (ex: `5511999887766`)
- `clienteId` (string, obrigatório): Identificador do cliente - "KC" ou "MJS"

**Response:** `201 Created`
```json
{
  "id": "323e4567-e89b-12d3-a456-426614174002",
  "tenantId": "KC",
  "date": "2026-01-15",
  "startTime": "14:00",
  "endTime": "15:30",
  "services": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Design de Sobrancelhas",
      "duration": 60,
      "price": 80.00
    },
    {
      "id": "223e4567-e89b-12d3-a456-426614174001",
      "name": "Alongamento de Cílios",
      "duration": 30,
      "price": 120.00
    }
  ],
  "userName": "Maria Silva",
  "userPhone": "5511999887766",
  "reminderSent": false
}
```

**Validações:**
- Data não pode estar bloqueada (feriado ou dia de folga)
- Horário deve estar dentro do expediente (09:00 - 18:00)
- Não pode conflitar com outros agendamentos
- EndTime é calculado automaticamente: `startTime + soma das durações dos serviços`

---

### 2. Buscar Horários Disponíveis
**GET** `/appointments/available-slots?date=2026-01-15`

Retorna todos os horários disponíveis para uma data específica.

**Query Parameters:**
- `date` (string, obrigatório): Data no formato `YYYY-MM-DD`

**Response:** `200 OK`
```json
[
  "09:00",
  "09:30",
  "10:00",
  "10:30",
  "11:00",
  "14:00",
  "14:30",
  "15:00"
]
```

**Observações:**
- Horários disponíveis vão de 09:00 às 18:00
- Intervalo de 30 em 30 minutos
- Remove horários já ocupados
- Retorna lista vazia se o dia estiver bloqueado

---

### 3. Listar Agendamentos Futuros por Telefone
**GET** `/appointments/future?userPhone=5511999887766`

Retorna todos os agendamentos futuros de um cliente (data >= hoje).

**Query Parameters:**
- `userPhone` (string, obrigatório): Número de telefone do cliente

**Response:** `200 OK`
```json
[
  {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "tenantId": "KC",
    "date": "2026-01-15",
    "startTime": "14:00",
    "endTime": "15:30",
    "services": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "name": "Design de Sobrancelhas",
        "duration": 60,
        "price": 80.00
      }
    ],
    "userName": "Maria Silva",
    "userPhone": "5511999887766",
    "reminderSent": false
  }
]
```

---

### 4. Listar Agendamentos Passados por Telefone
**GET** `/appointments/past?userPhone=5511999887766`

Retorna todos os agendamentos passados de um cliente (data < hoje).

**Query Parameters:**
- `userPhone` (string, obrigatório): Número de telefone do cliente

**Response:** `200 OK`
```json
[
  {
    "id": "423e4567-e89b-12d3-a456-426614174003",
    "tenantId": "KC",
    "date": "2025-12-20",
    "startTime": "10:00",
    "endTime": "11:00",
    "services": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "name": "Design de Sobrancelhas",
        "duration": 60,
        "price": 80.00
      }
    ],
    "userName": "Maria Silva",
    "userPhone": "5511999887766",
    "reminderSent": true
  }
]
```

---

### 5. Listar Todos os Agendamentos (ou por Data)
**GET** `/appointments` ou `/appointments?date=2026-01-15`

Lista todos os agendamentos ou filtra por data específica.

**Query Parameters:**
- `date` (string, opcional): Data no formato `YYYY-MM-DD`

**Response:** `200 OK`
```json
[
  {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "tenantId": "KC",
    "date": "2026-01-15",
    "startTime": "14:00",
    "endTime": "15:30",
    "services": [...],
    "userName": "Maria Silva",
    "userPhone": "5511999887766",
    "reminderSent": false
  }
]
```

---

### 6. Buscar Agendamento por ID
**GET** `/appointments/id/{appointmentId}`

Busca um agendamento específico pelo UUID.

**Path Parameters:**
- `appointmentId` (UUID, obrigatório): ID do agendamento

**Exemplo:** `/appointments/id/323e4567-e89b-12d3-a456-426614174002`

**Response:** `200 OK`
```json
{
  "id": "323e4567-e89b-12d3-a456-426614174002",
  "tenantId": "KC",
  "date": "2026-01-15",
  "startTime": "14:00",
  "endTime": "15:30",
  "services": [...],
  "userName": "Maria Silva",
  "userPhone": "5511999887766",
  "reminderSent": false
}
```

---

### 7. Cancelar Agendamento
**DELETE** `/appointments/{appointmentId}`

Cancela um agendamento e libera o horário.

**Path Parameters:**
- `appointmentId` (UUID, obrigatório): ID do agendamento

**Exemplo:** `/appointments/323e4567-e89b-12d3-a456-426614174002`

**Response:** `204 No Content`

---

## 💅 SERVICES (Serviços)

### 1. Criar Serviço
**POST** `/services`

Cria um novo serviço disponível para agendamento.

**Request Body:**
```json
{
  "name": "Design de Sobrancelhas",
  "duration": 60,
  "price": 80.00
}
```

**Campos:**
- `name` (string, obrigatório): Nome do serviço
- `duration` (number, obrigatório): Duração em minutos
- `price` (number, obrigatório): Preço do serviço

**Response:** `201 Created`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Design de Sobrancelhas",
  "duration": 60,
  "price": 80.00
}
```

---

### 2. Listar Todos os Serviços
**GET** `/services`

Retorna todos os serviços disponíveis.

**Response:** `200 OK`
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Design de Sobrancelhas",
    "duration": 60,
    "price": 80.00
  },
  {
    "id": "223e4567-e89b-12d3-a456-426614174001",
    "name": "Alongamento de Cílios",
    "duration": 90,
    "price": 120.00
  },
  {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "name": "Limpeza de Pele",
    "duration": 120,
    "price": 150.00
  }
]
```

---

### 3. Buscar Serviço por ID
**GET** `/services/{id}`

Busca um serviço específico pelo UUID.

**Path Parameters:**
- `id` (UUID, obrigatório): ID do serviço

**Exemplo:** `/services/123e4567-e89b-12d3-a456-426614174000`

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Design de Sobrancelhas",
  "duration": 60,
  "price": 80.00
}
```

---

### 4. Atualizar Serviço
**PUT** `/services/{id}`

Atualiza um serviço existente.

**Path Parameters:**
- `id` (UUID, obrigatório): ID do serviço

**Request Body:**
```json
{
  "name": "Design de Sobrancelhas Premium",
  "duration": 75,
  "price": 100.00
}
```

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Design de Sobrancelhas Premium",
  "duration": 75,
  "price": 100.00
}
```

---

### 5. Deletar Serviço
**DELETE** `/services/{id}`

Remove um serviço do sistema.

**Path Parameters:**
- `id` (UUID, obrigatório): ID do serviço

**Exemplo:** `/services/123e4567-e89b-12d3-a456-426614174000`

**Response:** `204 No Content`

---

## 🚫 BLOCKED DAYS (Dias Bloqueados)

### 1. Bloquear Data Específica
**POST** `/blocked-days/specific`

Bloqueia uma data específica (ex: feriado, evento especial).

**Request Body:**
```json
{
  "date": "2026-12-25",
  "reason": "Natal"
}
```

**Campos:**
- `date` (string, obrigatório): Data no formato `YYYY-MM-DD`
- `reason` (string, obrigatório): Motivo do bloqueio

**Response:** `201 Created`
```json
{
  "id": "523e4567-e89b-12d3-a456-426614174004",
  "specificDate": "2026-12-25",
  "dayOfWeek": null,
  "reason": "Natal"
}
```

---

### 2. Bloquear Dia da Semana Recorrente
**POST** `/blocked-days/recurring`

Bloqueia um dia da semana recorrente (ex: todo domingo).

**Request Body:**
```json
{
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

**Campos:**
- `dayOfWeek` (string, obrigatório): Dia da semana em inglês
  - Valores válidos: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`
- `reason` (string, obrigatório): Motivo do bloqueio

**Response:** `201 Created`
```json
{
  "id": "623e4567-e89b-12d3-a456-426614174005",
  "specificDate": null,
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

---

### 3. Listar Todos os Bloqueios
**GET** `/blocked-days`

Retorna todos os dias bloqueados (específicos e recorrentes).

**Response:** `200 OK`
```json
[
  {
    "id": "523e4567-e89b-12d3-a456-426614174004",
    "specificDate": "2026-12-25",
    "dayOfWeek": null,
    "reason": "Natal"
  },
  {
    "id": "623e4567-e89b-12d3-a456-426614174005",
    "specificDate": null,
    "dayOfWeek": "SUNDAY",
    "reason": "Folga semanal"
  }
]
```

---

### 4. Listar Bloqueios de Datas Específicas
**GET** `/blocked-days/specific`

Retorna apenas bloqueios de datas específicas (não recorrentes).

**Response:** `200 OK`
```json
[
  {
    "id": "523e4567-e89b-12d3-a456-426614174004",
    "specificDate": "2026-12-25",
    "dayOfWeek": null,
    "reason": "Natal"
  },
  {
    "id": "723e4567-e89b-12d3-a456-426614174006",
    "specificDate": "2026-01-01",
    "dayOfWeek": null,
    "reason": "Ano Novo"
  }
]
```

---

### 5. Listar Bloqueios Recorrentes
**GET** `/blocked-days/recurring`

Retorna apenas bloqueios recorrentes (dias da semana).

**Response:** `200 OK`
```json
[
  {
    "id": "623e4567-e89b-12d3-a456-426614174005",
    "specificDate": null,
    "dayOfWeek": "SUNDAY",
    "reason": "Folga semanal"
  }
]
```

---

### 6. Listar Datas Disponíveis em um Período
**GET** `/blocked-days/available?startDate=2026-01-01&endDate=2026-01-31`

Retorna lista de datas disponíveis (não bloqueadas) dentro de um período.

**Query Parameters:**
- `startDate` (string, obrigatório): Data inicial no formato `YYYY-MM-DD`
- `endDate` (string, obrigatório): Data final no formato `YYYY-MM-DD`

**Response:** `200 OK`
```json
[
  "2026-01-02",
  "2026-01-03",
  "2026-01-06",
  "2026-01-07",
  "2026-01-08",
  "2026-01-09",
  "2026-01-10"
]
```

**Observações:**
- Remove domingos (se configurado como bloqueio recorrente)
- Remove datas específicas bloqueadas (feriados, etc)

---

### 7. Desbloquear Dia
**DELETE** `/blocked-days/{blockedDayId}`

Remove um bloqueio e libera o dia para agendamentos.

**Path Parameters:**
- `blockedDayId` (UUID, obrigatório): ID do bloqueio

**Exemplo:** `/blocked-days/523e4567-e89b-12d3-a456-426614174004`

**Response:** `204 No Content`

---

## 🔐 Multi-Tenant

O sistema suporta múltiplos clientes (tenants) usando o campo `clienteId`:

- **KC**: Cliente KC
- **MJS**: Cliente MJS

O `clienteId` deve ser enviado:
1. No **header** `X-Tenant-ID` em todas as requisições (configurado no interceptor)
2. No **body** ao criar agendamentos (`clienteId`)

---

## 📱 Integração WhatsApp

Ao criar um agendamento, o sistema envia automaticamente uma mensagem via WhatsApp para:
- **URL:** `http://localhost:3001/whatsapp/agendamento`
- **Método:** POST

**Body enviado:**
```json
{
  "telefone": "5511999887766",
  "nome": "Maria Silva",
  "data": "15/01/2026",
  "hora": "14:00",
  "servico": "Design de Sobrancelhas, Alongamento de Cílios",
  "clienteId": "KC"
}
```

---

## ⚠️ Códigos de Erro

- **400 Bad Request**: Dados inválidos no request
- **404 Not Found**: Recurso não encontrado
- **500 Internal Server Error**: Erro no servidor (ex: conflito de horário, validação falhou)

**Exemplo de erro:**
```json
{
  "timestamp": "2026-01-02T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Horário selecionado (14:00 - 15:30) conflita com agendamento existente (14:00 - 15:00) de João Santos",
  "path": "/appointments"
}
```

---

## 🕐 Regras de Negócio

### Horário de Funcionamento
- **Abertura:** 09:00
- **Fechamento:** 18:00
- **Intervalo de slots:** 30 minutos

### Validações de Agendamento
1. ✅ Data não pode estar bloqueada
2. ✅ Horário deve estar dentro do expediente
3. ✅ Não pode conflitar com outros agendamentos
4. ✅ EndTime calculado automaticamente: `startTime + soma das durações`

### Multi-Serviço
- É possível agendar múltiplos serviços em um único agendamento
- A duração total é a soma das durações de todos os serviços
- Exemplo: Design (60min) + Alongamento (90min) = 150min total

---

## 📋 Exemplos Completos de Fluxo

### Fluxo 1: Cliente agendando um serviço

1. **Listar serviços disponíveis**
```http
GET /services
```

2. **Verificar datas disponíveis no mês**
```http
GET /blocked-days/available?startDate=2026-01-01&endDate=2026-01-31
```

3. **Verificar horários disponíveis em uma data**
```http
GET /appointments/available-slots?date=2026-01-15
```

4. **Criar agendamento**
```http
POST /appointments
Content-Type: application/json

{
  "serviceIds": ["123e4567-e89b-12d3-a456-426614174000"],
  "date": "2026-01-15",
  "startTime": "14:00",
  "userName": "Maria Silva",
  "userPhone": "5511999887766",
  "clienteId": "KC"
}
```

---

### Fluxo 2: Cliente verificando seus agendamentos

1. **Ver agendamentos futuros**
```http
GET /appointments/future?userPhone=5511999887766
```

2. **Ver histórico (agendamentos passados)**
```http
GET /appointments/past?userPhone=5511999887766
```

3. **Cancelar um agendamento**
```http
DELETE /appointments/323e4567-e89b-12d3-a456-426614174002
```

---

### Fluxo 3: Admin gerenciando bloqueios

1. **Bloquear todos os domingos**
```http
POST /blocked-days/recurring
Content-Type: application/json

{
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

2. **Bloquear feriado específico**
```http
POST /blocked-days/specific
Content-Type: application/json

{
  "date": "2026-12-25",
  "reason": "Natal"
}
```

3. **Ver todos os bloqueios**
```http
GET /blocked-days
```

---

## 🚀 Testando a API

### Usando cURL

```bash
# Criar agendamento
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "serviceIds": ["123e4567-e89b-12d3-a456-426614174000"],
    "date": "2026-01-15",
    "startTime": "14:00",
    "userName": "Maria Silva",
    "userPhone": "5511999887766",
    "clienteId": "KC"
  }'

# Listar serviços
curl http://localhost:8080/services

# Ver horários disponíveis
curl "http://localhost:8080/appointments/available-slots?date=2026-01-15"
```

### Usando Postman

Importe a collection disponível em: `postman_collection_multi_tenant.json`

---

**Última atualização:** 02/01/2026

