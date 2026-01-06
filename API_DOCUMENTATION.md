# 📚 Documentação da API - Sistema de Agendamentos Multi-Tenant

## 🔐 Autenticação
Todas as requisições devem incluir o header `X-Tenant-Id` para identificar o tenant.

```
X-Tenant-Id: cliente1
```

---

## 📅 APPOINTMENTS (Agendamentos)

### 1. Criar Agendamento
**POST** `/appointments`

Cria um novo agendamento com um ou mais serviços.

**Headers:**
```
X-Tenant-Id: cliente1
Content-Type: application/json
```

**Body:**
```json
{
  "serviceIds": ["550e8400-e29b-41d4-a716-446655440000"],
  "date": "2026-01-15",
  "startTime": "10:00",
  "userName": "João Silva",
  "userPhone": "5511999999999"
}
```

**Response:** `201 Created`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "tenantId": "cliente1",
  "date": "2026-01-15",
  "startTime": "10:00",
  "endTime": "11:30",
  "userName": "João Silva",
  "userPhone": "5511999999999",
  "services": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Extensão de Cílios",
      "duration": 90,
      "price": 150.00
    }
  ]
}
```

---

### 2. Buscar Horários Disponíveis
**GET** `/appointments/available-slots?date=2026-01-15`

Retorna horários disponíveis para uma data específica (30 em 30 minutos, 09:00-18:00).

**Headers:**
```
X-Tenant-Id: cliente1
```

**Query Parameters:**
- `date` (obrigatório): Data no formato YYYY-MM-DD

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

---

### 3. Buscar Agendamentos Futuros por Telefone
**GET** `/appointments/future?userPhone=5511999999999`

Retorna agendamentos futuros de um cliente específico.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Query Parameters:**
- `userPhone` (obrigatório): Número de telefone do cliente

**Response:** `200 OK`
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "date": "2026-01-20",
    "startTime": "14:00",
    "endTime": "15:30",
    "userName": "João Silva",
    "userPhone": "5511999999999",
    "services": [...]
  }
]
```

---

### 4. Buscar Agendamentos Passados por Telefone
**GET** `/appointments/past?userPhone=5511999999999`

Retorna agendamentos passados de um cliente específico.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Query Parameters:**
- `userPhone` (obrigatório): Número de telefone do cliente

**Response:** `200 OK`
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "date": "2025-12-10",
    "startTime": "10:00",
    "endTime": "11:30",
    "userName": "João Silva",
    "userPhone": "5511999999999",
    "services": [...]
  }
]
```

---

### 5. Listar Agendamentos
**GET** `/appointments`
**GET** `/appointments?date=2026-01-15`

Lista todos os agendamentos ou filtra por data.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Query Parameters:**
- `date` (opcional): Data no formato YYYY-MM-DD

**Response:** `200 OK`
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "date": "2026-01-15",
    "startTime": "10:00",
    "endTime": "11:30",
    "userName": "João Silva",
    "userPhone": "5511999999999",
    "services": [...]
  }
]
```

---

### 6. Buscar Agendamento por ID
**GET** `/appointments/{appointmentId}`

Busca um agendamento específico pelo ID.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Path Parameters:**
- `appointmentId` (obrigatório): UUID do agendamento

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "date": "2026-01-15",
  "startTime": "10:00",
  "endTime": "11:30",
  "userName": "João Silva",
  "userPhone": "5511999999999",
  "services": [...]
}
```

---

### 7. Cancelar Agendamento
**DELETE** `/appointments/{appointmentId}`

Cancela um agendamento existente.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Path Parameters:**
- `appointmentId` (obrigatório): UUID do agendamento

**Response:** `204 No Content`

---

## 🛠️ SERVICES (Serviços)

### 1. Criar Serviço
**POST** `/services`

Cria um novo serviço.

**Headers:**
```
X-Tenant-Id: cliente1
Content-Type: application/json
```

**Body:**
```json
{
  "name": "Extensão de Cílios",
  "duration": 90,
  "price": 150.00
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "cliente1",
  "name": "Extensão de Cílios",
  "duration": 90,
  "price": 150.00
}
```

---

### 2. Listar Serviços
**GET** `/services`

Lista todos os serviços do tenant.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "cliente1",
    "name": "Extensão de Cílios",
    "duration": 90,
    "price": 150.00
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "tenantId": "cliente1",
    "name": "Design de Sobrancelhas",
    "duration": 60,
    "price": 80.00
  }
]
```

---

### 3. Buscar Serviço por ID
**GET** `/services/{id}`

Busca um serviço específico pelo ID.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Path Parameters:**
- `id` (obrigatório): UUID do serviço

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "cliente1",
  "name": "Extensão de Cílios",
  "duration": 90,
  "price": 150.00
}
```

---

### 4. Atualizar Serviço
**PUT** `/services/{id}`

Atualiza um serviço existente.

**Headers:**
```
X-Tenant-Id: cliente1
Content-Type: application/json
```

**Path Parameters:**
- `id` (obrigatório): UUID do serviço

**Body:**
```json
{
  "name": "Extensão de Cílios Premium",
  "duration": 120,
  "price": 200.00
}
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "cliente1",
  "name": "Extensão de Cílios Premium",
  "duration": 120,
  "price": 200.00
}
```

---

### 5. Deletar Serviço
**DELETE** `/services/{id}`

Deleta um serviço.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Path Parameters:**
- `id` (obrigatório): UUID do serviço

**Response:** `204 No Content`

---

## 🚫 BLOCKED DAYS (Dias Bloqueados)

### 1. Bloquear Data Específica
**POST** `/blocked-days/specific`

Bloqueia uma data específica (ex: feriado, evento especial).

**Headers:**
```
X-Tenant-Id: cliente1
Content-Type: application/json
```

**Body:**
```json
{
  "date": "2026-12-25",
  "reason": "Natal"
}
```

**Response:** `201 Created`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440000",
  "tenantId": "cliente1",
  "specificDate": "2026-12-25",
  "reason": "Natal",
  "recurring": false,
  "dayOfWeek": null
}
```

---

### 2. Bloquear Dia da Semana Recorrente
**POST** `/blocked-days/recurring`

Bloqueia um dia da semana recorrente (ex: todo domingo).

**Headers:**
```
X-Tenant-Id: cliente1
Content-Type: application/json
```

**Body:**
```json
{
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

**Dias da semana válidos:** 
`MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

**Response:** `201 Created`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "tenantId": "cliente1",
  "specificDate": null,
  "reason": "Folga semanal",
  "recurring": true,
  "dayOfWeek": "SUNDAY"
}
```

---

### 3. Listar Todos os Dias Bloqueados
**GET** `/blocked-days`

Lista todos os dias bloqueados (específicos e recorrentes).

**Headers:**
```
X-Tenant-Id: cliente1
```

**Response:** `200 OK`
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "tenantId": "cliente1",
    "specificDate": "2026-12-25",
    "reason": "Natal",
    "recurring": false,
    "dayOfWeek": null
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "tenantId": "cliente1",
    "specificDate": null,
    "reason": "Folga semanal",
    "recurring": true,
    "dayOfWeek": "SUNDAY"
  }
]
```

---

### 4. Listar Apenas Bloqueios Específicos
**GET** `/blocked-days/specific`

Lista apenas bloqueios de datas específicas.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Response:** `200 OK`
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "tenantId": "cliente1",
    "specificDate": "2026-12-25",
    "reason": "Natal",
    "recurring": false,
    "dayOfWeek": null
  }
]
```

---

### 5. Listar Apenas Bloqueios Recorrentes
**GET** `/blocked-days/recurring`

Lista apenas bloqueios recorrentes (dias da semana).

**Headers:**
```
X-Tenant-Id: cliente1
```

**Response:** `200 OK`
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "tenantId": "cliente1",
    "specificDate": null,
    "reason": "Folga semanal",
    "recurring": true,
    "dayOfWeek": "SUNDAY"
  }
]
```

---

### 6. Buscar Datas Disponíveis em um Período
**GET** `/blocked-days/available?startDate=2026-01-01&endDate=2026-01-31`

Retorna lista de datas disponíveis (não bloqueadas) dentro de um período.

**Headers:**
```
X-Tenant-Id: cliente1
```

**Query Parameters:**
- `startDate` (obrigatório): Data inicial no formato YYYY-MM-DD
- `endDate` (obrigatório): Data final no formato YYYY-MM-DD

**Response:** `200 OK`
```json
[
  "2026-01-02",
  "2026-01-03",
  "2026-01-05",
  "2026-01-06",
  "2026-01-07",
  "2026-01-08",
  "2026-01-09"
]
```

---

### 7. Desbloquear Dia
**DELETE** `/blocked-days/{blockedDayId}`

Remove um bloqueio (libera o dia).

**Headers:**
```
X-Tenant-Id: cliente1
```

**Path Parameters:**
- `blockedDayId` (obrigatório): UUID do bloqueio

**Response:** `204 No Content`

---

## 📋 Resumo dos Endpoints

### Appointments
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/appointments` | Criar agendamento |
| GET | `/appointments/available-slots?date={date}` | Horários disponíveis |
| GET | `/appointments/future?userPhone={phone}` | Agendamentos futuros |
| GET | `/appointments/past?userPhone={phone}` | Agendamentos passados |
| GET | `/appointments` | Listar todos agendamentos |
| GET | `/appointments?date={date}` | Listar agendamentos por data |
| GET | `/appointments/{id}` | Buscar agendamento por ID |
| DELETE | `/appointments/{id}` | Cancelar agendamento |

### Services
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/services` | Criar serviço |
| GET | `/services` | Listar serviços |
| GET | `/services/{id}` | Buscar serviço por ID |
| PUT | `/services/{id}` | Atualizar serviço |
| DELETE | `/services/{id}` | Deletar serviço |

### Blocked Days
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/blocked-days/specific` | Bloquear data específica |
| POST | `/blocked-days/recurring` | Bloquear dia da semana |
| GET | `/blocked-days` | Listar todos bloqueios |
| GET | `/blocked-days/specific` | Listar bloqueios específicos |
| GET | `/blocked-days/recurring` | Listar bloqueios recorrentes |
| GET | `/blocked-days/available?startDate={start}&endDate={end}` | Datas disponíveis |
| DELETE | `/blocked-days/{id}` | Desbloquear dia |

---

## 🔄 Códigos de Status HTTP

- `200 OK` - Requisição bem-sucedida
- `201 Created` - Recurso criado com sucesso
- `204 No Content` - Operação bem-sucedida, sem conteúdo de retorno
- `400 Bad Request` - Dados inválidos na requisição
- `404 Not Found` - Recurso não encontrado
- `500 Internal Server Error` - Erro no servidor

---

## 💡 Observações Importantes

1. **Tenant ID obrigatório**: Todas as requisições devem incluir o header `X-Tenant-Id`
2. **Formato de datas**: Sempre usar o formato `YYYY-MM-DD` (ex: 2026-01-15)
3. **Formato de horários**: Sempre usar o formato `HH:mm` (ex: 14:30)
4. **UUIDs**: Todos os IDs são UUIDs no formato padrão
5. **Horários de funcionamento**: 09:00 às 18:00
6. **Intervalo de horários**: 30 em 30 minutos
7. **Dias da semana**: Usar valores em inglês maiúsculo (MONDAY, TUESDAY, etc.)

