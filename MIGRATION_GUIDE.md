# Guia de Migração - API com Tenant ID via JSON

## 📋 Resumo Executivo

Todas as APIs foram **MIGRADAS** de um modelo baseado em **headers HTTP** (`X-Client-Id`) para um modelo onde o **tenant ID é enviado no corpo JSON** de cada requisição.

---

## 🎯 Principais Mudanças

### 1. Métodos HTTP Alterados

| Operação Antiga | Operação Nova | Motivo |
|----------------|---------------|---------|
| `GET /appointments` | `POST /appointments/list` | Necessita tenantId no body |
| `GET /appointments/available-slots?date=X` | `POST /appointments/available-slots` | Necessita tenantId no body |
| `GET /appointments/future?userPhone=X` | `POST /appointments/future` | Necessita tenantId no body |
| `GET /appointments/past?userPhone=X` | `POST /appointments/past` | Necessita tenantId no body |
| `GET /appointments/id/{id}` | `POST /appointments/by-id` | Necessita tenantId no body |
| `DELETE /appointments/{id}` | `POST /appointments/cancel` | Necessita tenantId no body |
| `GET /services` | `POST /services/list` | Necessita tenantId no body |
| `GET /services/{id}` | `POST /services/by-id` | Necessita tenantId no body |
| `POST /services` | `POST /services/create` | Apenas renomeado |
| `PUT /services/{id}` | `POST /services/update` | Necessita tenantId no body |
| `DELETE /services/{id}` | `POST /services/delete` | Necessita tenantId no body |
| `GET /blocked-days` | `POST /blocked-days/list` | Necessita tenantId no body |
| `GET /blocked-days/specific` | `POST /blocked-days/specific/list` | Necessita tenantId no body |
| `GET /blocked-days/recurring` | `POST /blocked-days/recurring/list` | Necessita tenantId no body |
| `GET /blocked-days/available?startDate=X&endDate=Y` | `POST /blocked-days/available` | Necessita tenantId no body |
| `DELETE /blocked-days/{id}` | `POST /blocked-days/unblock` | Necessita tenantId no body |

---

## 📝 Exemplos de Requisições

### **APPOINTMENTS**

#### 1. Criar Agendamento
```http
POST /appointments
Content-Type: application/json

{
  "tenantId": "cliente1",
  "serviceIds": ["uuid-servico-1", "uuid-servico-2"],
  "date": "2026-01-15",
  "startTime": "10:00",
  "userName": "João Silva",
  "userPhone": "5511999999999"
}
```

#### 2. Buscar Horários Disponíveis
```http
POST /appointments/available-slots
Content-Type: application/json

{
  "tenantId": "cliente1",
  "date": "2026-01-15"
}
```

#### 3. Buscar Agendamentos Futuros de um Cliente
```http
POST /appointments/future
Content-Type: application/json

{
  "tenantId": "cliente1",
  "userPhone": "5511999999999"
}
```

#### 4. Buscar Agendamentos Passados de um Cliente
```http
POST /appointments/past
Content-Type: application/json

{
  "tenantId": "cliente1",
  "userPhone": "5511999999999"
}
```

#### 5. Listar Todos os Agendamentos (ou por Data)
```http
POST /appointments/list
Content-Type: application/json

{
  "tenantId": "cliente1",
  "date": "2026-01-15"  // opcional
}
```

#### 6. Buscar Agendamento por ID
```http
POST /appointments/by-id
Content-Type: application/json

{
  "tenantId": "cliente1",
  "id": "uuid-do-agendamento"
}
```

#### 7. Cancelar Agendamento
```http
POST /appointments/cancel
Content-Type: application/json

{
  "tenantId": "cliente1",
  "id": "uuid-do-agendamento"
}
```

---

### **SERVICES**

#### 1. Criar Serviço
```http
POST /services/create
Content-Type: application/json

{
  "tenantId": "cliente1",
  "name": "Extensão de Cílios",
  "duration": 90,
  "price": 150.00
}
```

#### 2. Listar Serviços
```http
POST /services/list
Content-Type: application/json

{
  "tenantId": "cliente1"
}
```

#### 3. Buscar Serviço por ID
```http
POST /services/by-id
Content-Type: application/json

{
  "tenantId": "cliente1",
  "id": "uuid-do-servico"
}
```

#### 4. Atualizar Serviço
```http
POST /services/update
Content-Type: application/json

{
  "tenantId": "cliente1",
  "id": "uuid-do-servico",
  "name": "Design de Sobrancelhas",
  "duration": 60,
  "price": 80.00
}
```

#### 5. Deletar Serviço
```http
POST /services/delete
Content-Type: application/json

{
  "tenantId": "cliente1",
  "id": "uuid-do-servico"
}
```

---

### **BLOCKED DAYS**

#### 1. Bloquear Data Específica
```http
POST /blocked-days/specific
Content-Type: application/json

{
  "tenantId": "cliente1",
  "date": "2026-12-25",
  "reason": "Natal"
}
```

#### 2. Bloquear Dia da Semana (Recorrente)
```http
POST /blocked-days/recurring
Content-Type: application/json

{
  "tenantId": "cliente1",
  "dayOfWeek": "SUNDAY",
  "reason": "Folga semanal"
}
```

#### 3. Listar Todos os Bloqueios
```http
POST /blocked-days/list
Content-Type: application/json

{
  "tenantId": "cliente1"
}
```

#### 4. Listar Apenas Bloqueios Específicos
```http
POST /blocked-days/specific/list
Content-Type: application/json

{
  "tenantId": "cliente1"
}
```

#### 5. Listar Apenas Bloqueios Recorrentes
```http
POST /blocked-days/recurring/list
Content-Type: application/json

{
  "tenantId": "cliente1"
}
```

#### 6. Buscar Datas Disponíveis em um Período
```http
POST /blocked-days/available
Content-Type: application/json

{
  "tenantId": "cliente1",
  "startDate": "2026-01-01",
  "endDate": "2026-01-31"
}
```

#### 7. Remover Bloqueio
```http
POST /blocked-days/unblock
Content-Type: application/json

{
  "tenantId": "cliente1",
  "id": "uuid-do-bloqueio"
}
```

---

## 🔧 Migração Frontend - JavaScript/TypeScript

### Exemplo com Fetch API

```javascript
// ❌ ANTES (com header)
const fetchAppointments = async () => {
  const response = await fetch('/appointments', {
    method: 'GET',
    headers: {
      'X-Client-Id': 'cliente1'
    }
  });
  return response.json();
};

// ✅ DEPOIS (com body JSON)
const fetchAppointments = async () => {
  const response = await fetch('/appointments/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      tenantId: 'cliente1',
      date: null  // opcional
    })
  });
  return response.json();
};
```

### Exemplo com Axios

```javascript
// ❌ ANTES
const appointments = await axios.get('/appointments', {
  headers: { 'X-Client-Id': 'cliente1' }
});

// ✅ DEPOIS
const appointments = await axios.post('/appointments/list', {
  tenantId: 'cliente1'
});
```

---

## 🧪 Testando com cURL

```bash
# Criar agendamento
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "cliente1",
    "serviceIds": ["uuid-servico"],
    "date": "2026-01-15",
    "startTime": "10:00",
    "userName": "João Silva",
    "userPhone": "5511999999999"
  }'

# Listar serviços
curl -X POST http://localhost:8080/services/list \
  -H "Content-Type: application/json" \
  -d '{"tenantId": "cliente1"}'

# Buscar horários disponíveis
curl -X POST http://localhost:8080/appointments/available-slots \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "cliente1",
    "date": "2026-01-15"
  }'
```

---

## 📦 Arquivos Criados/Modificados

### Novos DTOs
- ✅ `CreateServiceRequest.java`
- ✅ `UpdateServiceRequest.java`
- ✅ `UpdateServiceWithIdRequest.java`
- ✅ `TenantRequest.java`
- ✅ `TenantIdWithId.java`
- ✅ `GetAvailableSlotsRequest.java`
- ✅ `GetAppointmentsByPhoneRequest.java`
- ✅ `GetAppointmentsByDateRequest.java`
- ✅ `GetAvailableDatesRequest.java`

### DTOs Modificados
- ✅ `BlockSpecificDateRequest.java` - adicionado `tenantId`
- ✅ `BlockRecurringDayRequest.java` - adicionado `tenantId`
- ✅ `CreateAppointmentRequest.java` - renomeado `clienteId` → `tenantId`

### Controllers Modificados
- ✅ `AppointmentsController.java` - todos os endpoints migrados
- ✅ `ServicesController.java` - todos os endpoints migrados
- ✅ `BlockedDayController.java` - todos os endpoints migrados

### Services Modificados
- ✅ `AppointmentsService.java` - parâmetro `clienteId` renomeado para `tenantId`

---

## ⚠️ Notas Importantes

1. **Validação**: Todos os DTOs têm `@NotNull` no campo `tenantId` - requisições sem esse campo retornarão erro 400.

2. **TenantContext**: Cada controller seta o tenant no contexto: `TenantContext.setTenantId(request.getTenantId())`

3. **Retrocompatibilidade**: O `TenantInterceptor` ainda existe, mas agora é sobrescrito pelos controllers.

4. **Whatsapp**: O campo `clienteId` no DTO `Whats.java` ainda existe e recebe o `tenantId` corretamente.

---

## ✅ Status Final

- ✅ Compilação sem erros
- ✅ Todos os endpoints migrados
- ✅ DTOs criados e validados
- ✅ Documentação completa
- ✅ Pronto para uso

---

## 📞 Suporte

Se houver dúvidas sobre algum endpoint específico, consulte:
- `API_ENDPOINTS.md` (se existir na raiz do projeto)
- Documentação inline nos controllers
- Este arquivo de migração

