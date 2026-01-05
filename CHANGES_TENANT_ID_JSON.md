# Alterações Realizadas: Tenant ID via JSON

## Resumo
Todas as APIs foram modificadas para receber o `tenantId` via JSON no corpo da requisição, ao invés de usar o header `X-Client-Id`.

## Arquivos Modificados

### 1. DTOs Atualizados

#### BlockSpecificDateRequest.java
- Adicionado campo `tenantId` obrigatório

#### BlockRecurringDayRequest.java
- Adicionado campo `tenantId` obrigatório

#### CreateAppointmentRequest.java
- Renomeado campo `clienteId` para `tenantId` para consistência

### 2. Novos DTOs Criados

#### CreateServiceRequest.java
- DTO para criação de serviços com tenantId

#### UpdateServiceRequest.java
- DTO para atualização de serviços com tenantId

#### UpdateServiceWithIdRequest.java
- DTO para atualização de serviços com ID e tenantId

#### TenantRequest.java
- DTO genérico para requisições que precisam apenas do tenantId

#### TenantIdWithId.java
- DTO para requisições que precisam de tenantId e um ID (UUID)

#### GetAvailableSlotsRequest.java
- DTO para buscar horários disponíveis com tenantId e data

#### GetAppointmentsByPhoneRequest.java
- DTO para buscar agendamentos por telefone com tenantId

#### GetAppointmentsByDateRequest.java
- DTO para buscar agendamentos por data com tenantId

#### GetAvailableDatesRequest.java
- DTO para buscar datas disponíveis com tenantId, startDate e endDate

### 3. Controllers Atualizados

#### AppointmentsController.java
Todas as operações agora usam POST e recebem tenantId no JSON:

**Endpoints modificados:**
- `POST /appointments` - Criar agendamento
  - Body: `{ "tenantId": "cliente1", "serviceIds": [...], "date": "2024-12-15", "startTime": "10:00", "userName": "João", "userPhone": "5511999999999" }`

- `POST /appointments/available-slots` - Horários disponíveis (era GET)
  - Body: `{ "tenantId": "cliente1", "date": "2024-12-15" }`

- `POST /appointments/future` - Agendamentos futuros (era GET)
  - Body: `{ "tenantId": "cliente1", "userPhone": "5511999999999" }`

- `POST /appointments/past` - Agendamentos passados (era GET)
  - Body: `{ "tenantId": "cliente1", "userPhone": "5511999999999" }`

- `POST /appointments/list` - Listar agendamentos (era GET /appointments)
  - Body: `{ "tenantId": "cliente1", "date": "2024-12-15" }` (date opcional)

- `POST /appointments/by-id` - Buscar por ID (era GET /appointments/id/{id})
  - Body: `{ "tenantId": "cliente1", "id": "uuid-do-agendamento" }`

- `POST /appointments/cancel` - Cancelar agendamento (era DELETE)
  - Body: `{ "tenantId": "cliente1", "id": "uuid-do-agendamento" }`

#### ServicesController.java
Todas as operações agora usam POST e recebem tenantId no JSON:

**Endpoints modificados:**
- `POST /services/create` - Criar serviço (era POST /services)
  - Body: `{ "tenantId": "cliente1", "name": "Extensão de Cílios", "duration": 90, "price": 150.00 }`

- `POST /services/list` - Listar serviços (era GET /services)
  - Body: `{ "tenantId": "cliente1" }`

- `POST /services/by-id` - Buscar por ID (era GET /services/{id})
  - Body: `{ "tenantId": "cliente1", "id": "uuid-do-servico" }`

- `POST /services/update` - Atualizar serviço (era PUT /services/{id})
  - Body: `{ "tenantId": "cliente1", "id": "uuid-do-servico", "name": "Nome", "duration": 60, "price": 80.00 }`

- `POST /services/delete` - Deletar serviço (era DELETE /services/{id})
  - Body: `{ "tenantId": "cliente1", "id": "uuid-do-servico" }`

#### BlockedDayController.java
Todas as operações agora recebem tenantId no JSON:

**Endpoints modificados:**
- `POST /blocked-days/specific` - Bloquear data específica
  - Body: `{ "tenantId": "cliente1", "date": "2025-12-25", "reason": "Natal" }`

- `POST /blocked-days/recurring` - Bloquear dia da semana
  - Body: `{ "tenantId": "cliente1", "dayOfWeek": "SUNDAY", "reason": "Folga semanal" }`

- `POST /blocked-days/list` - Listar todos os bloqueios (era GET)
  - Body: `{ "tenantId": "cliente1" }`

- `POST /blocked-days/specific/list` - Listar bloqueios específicos (era GET /blocked-days/specific)
  - Body: `{ "tenantId": "cliente1" }`

- `POST /blocked-days/recurring/list` - Listar bloqueios recorrentes (era GET /blocked-days/recurring)
  - Body: `{ "tenantId": "cliente1" }`

- `POST /blocked-days/available` - Datas disponíveis (era GET)
  - Body: `{ "tenantId": "cliente1", "startDate": "2025-12-27", "endDate": "2026-01-31" }`

- `POST /blocked-days/unblock` - Remover bloqueio (era DELETE)
  - Body: `{ "tenantId": "cliente1", "id": "uuid-do-bloqueio" }`

### 4. Lógica de TenantContext

Em todos os controllers, agora fazemos:
```java
TenantContext.setTenantId(request.getTenantId());
```

Isso garante que o tenant ID do JSON seja usado corretamente no contexto da aplicação.

## Migração Frontend

O frontend precisará ser atualizado para:

1. **Remover headers X-Client-Id** de todas as requisições
2. **Adicionar tenantId no body** de todas as requisições
3. **Mudar métodos HTTP**:
   - GET → POST para a maioria dos endpoints de listagem
   - DELETE → POST para endpoints de deleção
   - Alguns endpoints mudaram de caminho (ex: `/appointments` → `/appointments/list`)

## Exemplo de Migração

### Antes:
```javascript
// GET request com header
fetch('/appointments', {
  headers: {
    'X-Client-Id': 'cliente1'
  }
})
```

### Depois:
```javascript
// POST request com body
fetch('/appointments/list', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    tenantId: 'cliente1'
  })
})
```

## Retrocompatibilidade

O `TenantInterceptor` ainda existe e continua lendo o header `X-Client-Id` se fornecido, mas agora é sobrescrito pelo `tenantId` do JSON nos controllers. Isso pode ser mantido para compatibilidade ou removido se não for mais necessário.

## Validação

Todos os DTOs têm validação `@NotNull` para o campo `tenantId`, garantindo que ele seja sempre fornecido.

## Status

✅ Todos os arquivos foram modificados
✅ Nenhum erro de compilação
⚠️  Alguns warnings de "método não utilizado" são esperados (são métodos de API)
✅ Estrutura de DTOs criada e validada
✅ Controllers atualizados com nova lógica

