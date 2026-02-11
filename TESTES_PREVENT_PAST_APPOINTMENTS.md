# 🧪 Testes: Prevenção de Agendamentos no Passado

## 📋 Resumo

Este documento descreve os testes manuais e automatizados para validar a funcionalidade de prevenção de agendamentos no passado.

## 🎯 Funcionalidades Testadas

1. **Filtragem de horários disponíveis** - Remove horários que já passaram
2. **Validação na criação de agendamentos** - Bloqueia criação de agendamentos retroativos
3. **Timezone awareness** - Considera o timezone do tenant

## 🔬 Testes Unitários

### TenantDateTimeServiceTest

**Localização:** `src/test/java/lash_salao_kc/agendamento_back/service/TenantDateTimeServiceTest.java`

| Teste | Objetivo | Resultado Esperado |
|-------|----------|-------------------|
| `testIsInPast_DateInPast_ReturnsTrue` | Data no passado | `true` |
| `testIsInPast_TodayButTimePassed_ReturnsTrue` | Hoje, horário passado | `true` |
| `testIsInPast_FutureDate_ReturnsFalse` | Data futura | `false` |
| `testIsInPast_TodayFutureTime_ReturnsFalse` | Hoje, horário futuro | `false` |
| `testIsDateInPast_PastDate_ReturnsTrue` | Data anterior | `true` |
| `testIsDateInPast_Today_ReturnsFalse` | Data atual | `false` |
| `testIsDateInPast_FutureDate_ReturnsFalse` | Data futura | `false` |
| `testIsToday_TodayDate_ReturnsTrue` | É hoje | `true` |
| `testIsToday_PastDate_ReturnsFalse` | Não é hoje (passado) | `false` |
| `testIsToday_FutureDate_ReturnsFalse` | Não é hoje (futuro) | `false` |
| `testGetTenantZoneId_ValidTimezone_ReturnsCorrectZone` | Timezone válido | `ZoneId correto` |
| `testGetTenantZoneId_InvalidTimezone_ReturnsFallback` | Timezone inválido | `Fallback para America/Sao_Paulo` |
| `testToZonedDateTime_ConvertsCorrectly` | Conversão de data/hora | `ZonedDateTime correto` |

### Executar Testes

```bash
# Executar todos os testes
./mvnw test

# Executar apenas os testes do TenantDateTimeService
./mvnw test -Dtest=TenantDateTimeServiceTest
```

## 🧪 Testes Manuais via API

### Pré-requisitos

1. Sistema rodando: `./mvnw spring-boot:run`
2. Tenant configurado com timezone
3. Professional e serviços cadastrados

### Cenário 1: Consultar Horários Disponíveis - Data Passada

**Request:**
```http
GET http://localhost:8080/appointments/available-slots?professionalId=<UUID>&date=2026-02-10&serviceIds=<UUID>
X-Tenant-Id: kc
```

**Resultado Esperado:**
```json
[]
```

**Motivo:** Data no passado não deve retornar nenhum horário.

---

### Cenário 2: Consultar Horários Disponíveis - Hoje (antes das 14h)

**Contexto:** Executar este teste antes das 14:00

**Request:**
```http
GET http://localhost:8080/appointments/available-slots?professionalId=<UUID>&date=2026-02-11&serviceIds=<UUID>
X-Tenant-Id: kc
```

**Resultado Esperado:**
```json
[
  "14:00:00",
  "14:30:00",
  "15:00:00",
  "15:30:00",
  ...
]
```

**Observações:**
- Horários anteriores ao horário atual NÃO devem aparecer
- Apenas horários >= ao horário atual + buffer

---

### Cenário 3: Consultar Horários Disponíveis - Data Futura

**Request:**
```http
GET http://localhost:8080/appointments/available-slots?professionalId=<UUID>&date=2026-02-15&serviceIds=<UUID>
X-Tenant-Id: kc
```

**Resultado Esperado:**
```json
[
  "09:00:00",
  "09:30:00",
  "10:00:00",
  ...
]
```

**Observações:**
- Todos os horários disponíveis conforme horário de trabalho
- Nenhum filtro de horário passado aplicado

---

### Cenário 4: Tentar Criar Agendamento - Data Passada

**Request:**
```http
POST http://localhost:8080/appointments
X-Tenant-Id: kc
Content-Type: application/json

{
  "professionalId": "<UUID>",
  "serviceIds": ["<UUID>"],
  "date": "2026-02-10",
  "startTime": "10:00",
  "userName": "João Silva",
  "userPhone": "+5511999999999"
}
```

**Resultado Esperado:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Não é possível agendar para um horário que já passou. Data/hora solicitada: 10/02/2026 às 10:00, data/hora atual: 11/02/2026 às 14:30"
}
```

---

### Cenário 5: Tentar Criar Agendamento - Hoje, Horário Passado

**Contexto:** Executar às 14:00, tentar agendar para 10:00 do mesmo dia

**Request:**
```http
POST http://localhost:8080/appointments
X-Tenant-Id: kc
Content-Type: application/json

{
  "professionalId": "<UUID>",
  "serviceIds": ["<UUID>"],
  "date": "2026-02-11",
  "startTime": "10:00",
  "userName": "Maria Santos",
  "userPhone": "+5511988888888"
}
```

**Resultado Esperado:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Não é possível agendar para um horário que já passou. Data/hora solicitada: 11/02/2026 às 10:00, data/hora atual: 11/02/2026 às 14:00"
}
```

---

### Cenário 6: Criar Agendamento - Hoje, Horário Futuro

**Contexto:** Executar às 14:00, agendar para 16:00 do mesmo dia

**Request:**
```http
POST http://localhost:8080/appointments
X-Tenant-Id: kc
Content-Type: application/json

{
  "professionalId": "<UUID>",
  "serviceIds": ["<UUID>"],
  "date": "2026-02-11",
  "startTime": "16:00",
  "userName": "Ana Costa",
  "userPhone": "+5511977777777"
}
```

**Resultado Esperado:**
```json
{
  "id": "<UUID>",
  "professionalId": "<UUID>",
  "date": "2026-02-11",
  "startTime": "16:00",
  "endTime": "17:00",
  "userName": "Ana Costa",
  "userPhone": "+5511977777777",
  ...
}
```

**Status:** `201 Created`

---

### Cenário 7: Criar Agendamento - Data Futura

**Request:**
```http
POST http://localhost:8080/appointments
X-Tenant-Id: kc
Content-Type: application/json

{
  "professionalId": "<UUID>",
  "serviceIds": ["<UUID>"],
  "date": "2026-02-15",
  "startTime": "10:00",
  "userName": "Pedro Oliveira",
  "userPhone": "+5511966666666"
}
```

**Resultado Esperado:**
```json
{
  "id": "<UUID>",
  "professionalId": "<UUID>",
  "date": "2026-02-15",
  "startTime": "10:00",
  "endTime": "11:00",
  "userName": "Pedro Oliveira",
  "userPhone": "+5511966666666",
  ...
}
```

**Status:** `201 Created`

---

## 🌍 Testes de Timezone

### Cenário 8: Tenant com Timezone Diferente

**Setup:**
```sql
UPDATE tb_tenants SET timezone = 'America/New_York' WHERE tenant_key = 'ny-salon';
```

**Request:**
```http
GET http://localhost:8080/appointments/available-slots?professionalId=<UUID>&date=2026-02-11&serviceIds=<UUID>
X-Tenant-Id: ny-salon
```

**Validação:**
- O sistema deve usar o horário de New York (UTC-5)
- Se for 14:00 em São Paulo (UTC-3), em New York são 12:00
- Os horários filtrados devem considerar o timezone correto

---

## ✅ Checklist de Validação

- [ ] Data passada retorna lista vazia de horários
- [ ] Hoje retorna apenas horários futuros
- [ ] Data futura retorna todos os horários normais
- [ ] Criação com data passada é rejeitada (400)
- [ ] Criação hoje com horário passado é rejeitada (400)
- [ ] Criação hoje com horário futuro funciona (201)
- [ ] Criação com data futura funciona (201)
- [ ] Mensagem de erro é clara e informativa
- [ ] Timezone do tenant é respeitado
- [ ] Logs mostram filtragem de horários passados

---

## 📊 Validação de Logs

Ao executar os testes, verifique os logs:

```
✅ Logs esperados (data futura):
INFO  - Calculando horários disponíveis para profissional <UUID> na data 2026-02-15
INFO  - Encontrados 20 horários disponíveis de 20 possíveis

✅ Logs esperados (hoje com horários passados):
INFO  - Calculando horários disponíveis para profissional <UUID> na data 2026-02-11
DEBUG - ⏱️ Slot 09:00 na data 2026-02-11 está no passado (horário já passou)
DEBUG - ⏱️ Slot 09:30 na data 2026-02-11 está no passado (horário já passou)
DEBUG - ⏱️ Slot 10:00 na data 2026-02-11 está no passado (horário já passou)
INFO  - Encontrados 10 horários disponíveis de 20 possíveis

✅ Logs esperados (data passada):
INFO  - Calculando horários disponíveis para profissional <UUID> na data 2026-02-10
DEBUG - ⏱️ Slot 09:00 na data 2026-02-10 está no passado (data anterior)
INFO  - Encontrados 0 horários disponíveis de 20 possíveis
```

---

## 🐛 Casos de Borda

### Meia-noite
- Teste agendar para `00:00` do dia seguinte
- Deve funcionar normalmente

### Último minuto do dia
- Teste agendar para `23:59`
- Se estiver configurado no horário de trabalho, deve funcionar

### Mudança de horário de verão
- O Java lida automaticamente via ZoneId
- Não requer testes especiais

---

## 📝 Observações

1. **Cache de data/hora:** Os testes consideram o horário real do servidor
2. **Precisão:** Comparações incluem segundos
3. **Timezone padrão:** `America/Sao_Paulo` se não configurado
4. **Fallback:** Se timezone inválido, usa `America/Sao_Paulo`

