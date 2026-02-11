# 📊 DIAGRAMA: Fluxo de Prevenção de Agendamentos no Passado

## 🔄 Fluxo Completo: Consulta de Horários Disponíveis

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE/FRONTEND                         │
│                                                                 │
│  GET /appointments/available-slots?                             │
│      professionalId=abc&date=2026-02-11&serviceIds=xyz         │
│                                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   APPOINTMENTS CONTROLLER                       │
│                                                                 │
│  • Extrai parâmetros da requisição                             │
│  • Obtém X-Tenant-Id do contexto                               │
│                                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   APPOINTMENTS SERVICE                          │
│                                                                 │
│  • Valida tenant existe e está ativo                           │
│  • Valida professional pertence ao tenant                      │
│  • Delega cálculo de horários                                  │
│                                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              AVAILABLE TIME SLOTS SERVICE                       │
│                                                                 │
│  1️⃣ Verifica se dia está bloqueado                              │
│     └─> BlockedDayService.isDateBlocked()                      │
│                                                                 │
│  2️⃣ Busca horário de trabalho do profissional                   │
│     └─> TenantWorkingHoursService.getWorkingHours()            │
│                                                                 │
│  3️⃣ Gera todos os slots possíveis                               │
│     └─> generateAllTimeSlots()                                 │
│                                                                 │
│  4️⃣ Busca bloqueios de horário                                  │
│     └─> BlockedTimeSlotService.getBlockedTimeSlotsForDate()    │
│                                                                 │
│  5️⃣ Busca agendamentos existentes                               │
│     └─> AppointmentsRepository.findByProfessionalIdAndDate()   │
│                                                                 │
│  6️⃣ FILTRA SLOTS (Stream Pipeline):                             │
│                                                                 │
│     allPossibleSlots.stream()                                  │
│       .filter(slot -> !isSlotBlocked(slot, blockedSlots))      │
│       .filter(slot -> !isTimeSlotInPast(date, slot, tenantId)) │ ✨ NOVO!
│       .filter(slot -> !conflictsWithAppointments(...))         │
│       .filter(slot -> !wouldEndInBlockedTime(...))             │
│       .collect(Collectors.toList())                            │
│                                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Para cada slot, verifica se está no passado
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│    isTimeSlotInPast(date, slot, tenantId)                       │
│                                                                 │
│    ┌──────────────────────────────────────────┐                │
│    │ TenantDateTimeService.isDateInPast()?   │                │
│    └────────┬──────────────┬──────────────────┘                │
│             │              │                                    │
│          SIM (passado)   NÃO                                    │
│             │              │                                    │
│             ▼              ▼                                    │
│       return TRUE    ┌─────────────────────────┐               │
│                      │ É hoje?                │               │
│                      └────┬──────────┬─────────┘               │
│                           │          │                         │
│                         SIM        NÃO (futuro)                │
│                           │          │                         │
│                           ▼          ▼                         │
│            ┌───────────────────┐  return FALSE                 │
│            │ TenantDateTime    │                               │
│            │ Service.isInPast  │                               │
│            │ (date, slot)?     │                               │
│            └────┬──────┬───────┘                               │
│                 │      │                                        │
│            SIM (passou) NÃO                                     │
│                 │      │                                        │
│                 ▼      ▼                                        │
│           return TRUE  return FALSE                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                         │
                         │ Retorna slots filtrados
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                         RESPONSE                                │
│                                                                 │
│  200 OK                                                         │
│  [                                                              │
│    "14:30:00",  ✅ (horários futuros)                           │
│    "15:00:00",                                                  │
│    "15:30:00",                                                  │
│    ...                                                          │
│  ]                                                              │
│                                                                 │
│  Não inclui:                                                    │
│  - "09:00:00" ❌ (passou)                                        │
│  - "10:00:00" ❌ (passou)                                        │
│  - "14:00:00" ❌ (passou)                                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔒 Fluxo Completo: Criação de Agendamento

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE/FRONTEND                         │
│                                                                 │
│  POST /appointments                                             │
│  {                                                              │
│    "professionalId": "abc",                                     │
│    "serviceIds": ["xyz"],                                       │
│    "date": "2026-02-11",                                        │
│    "startTime": "10:00",                                        │
│    "userName": "João",                                          │
│    "userPhone": "+5511999999999"                                │
│  }                                                              │
│                                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   APPOINTMENTS CONTROLLER                       │
│                                                                 │
│  • Valida request body (@Valid)                                │
│  • Obtém X-Tenant-Id do contexto                               │
│  • Chama service para criar agendamento                        │
│                                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│        APPOINTMENTS SERVICE - createAppointment()               │
│                                                                 │
│  1️⃣ Valida tenant existe e está ativo                           │
│     └─> TenantRepository.findByTenantKeyAndActiveTrue()        │
│                                                                 │
│  2️⃣ Valida professional pertence ao tenant                      │
│     └─> ProfessionalRepository.findActiveByIdAndTenantId()     │
│                                                                 │
│  3️⃣ ✨ VALIDAÇÃO NOVA: validateNotInPast()                       │
│     └─────────────┬─────────────────────────────────────────┐  │
│                   ▼                                         │  │
│     ┌──────────────────────────────────────────┐           │  │
│     │ TenantDateTimeService.isInPast(          │           │  │
│     │   date, startTime, tenantId              │           │  │
│     │ )                                        │           │  │
│     └────────┬──────────────┬──────────────────┘           │  │
│              │              │                              │  │
│            TRUE           FALSE                            │  │
│              │              │                              │  │
│              ▼              ▼                              │  │
│    ┌─────────────────┐  Continua                          │  │
│    │ throw Business  │  validações                        │  │
│    │ Exception       │                                    │  │
│    │ "Não é possível │                                    │  │
│    │ agendar para um │                                    │  │
│    │ horário que já  │                                    │  │
│    │ passou..."      │                                    │  │
│    └────────┬────────┘                                    │  │
│             │                                             │  │
│             ▼                                             │  │
│       400 Bad Request ❌                                   │  │
│                                                           │  │
└───────────────────────────────────────────────────────────┘  │
                         │                                     │
                         │ Se passou: para aqui ✋              │
                         │ Se futuro: continua ✅               │
                         ▼                                     │
│  4️⃣ validateDateNotBlocked()                                  │
│     └─> BlockedDayService.isDateBlocked()                    │
│                                                              │
│  5️⃣ Busca serviços e calcula duração total                    │
│                                                              │
│  6️⃣ validateProfessionalExecutesServices()                    │
│                                                              │
│  7️⃣ validateBusinessHours()                                   │
│                                                              │
│  8️⃣ validateNoTimeSlotBlocks()                                │
│                                                              │
│  9️⃣ validateNoConflicts()                                     │
│                                                              │
│  🔟 Cria entidade de agendamento                              │
│                                                              │
│  1️⃣1️⃣ Salva no banco                                           │
│                                                              │
│  1️⃣2️⃣ Envia WhatsApp notification                              │
│                                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                         RESPONSE                                │
│                                                                 │
│  ✅ Se futuro: 201 Created                                      │
│  {                                                              │
│    "id": "...",                                                 │
│    "date": "2026-02-15",                                        │
│    "startTime": "10:00",                                        │
│    ...                                                          │
│  }                                                              │
│                                                                 │
│  ❌ Se passado: 400 Bad Request                                 │
│  {                                                              │
│    "status": 400,                                               │
│    "message": "Não é possível agendar para um horário que já   │
│                passou. Data/hora solicitada: 11/02/2026 às     │
│                10:00, data/hora atual: 11/02/2026 às 14:30"    │
│  }                                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## ⚙️ Componente: TenantDateTimeService

```
┌─────────────────────────────────────────────────────────────────┐
│                  TenantDateTimeService                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  📦 Dependências:                                               │
│  • TenantRepository                                             │
│                                                                 │
│  🔧 Métodos Principais:                                         │
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │ now(tenantId): ZonedDateTime                          │     │
│  │ ↳ Retorna data/hora atual no timezone do tenant      │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │ isInPast(date, time, tenantId): boolean               │     │
│  │ ↳ Verifica se data/hora está no passado              │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │ isDateInPast(date, tenantId): boolean                 │     │
│  │ ↳ Verifica se data (sem hora) está no passado        │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │ isToday(date, tenantId): boolean                      │     │
│  │ ↳ Verifica se data é hoje                            │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │ getTenantZoneId(tenantId): ZoneId                     │     │
│  │ ↳ Obtém ZoneId do tenant (com fallback seguro)       │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
│  🛡️ Fallback:                                                   │
│  • Se timezone NULL → America/Sao_Paulo                         │
│  • Se timezone inválido → America/Sao_Paulo                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Modelo de Dados

```
┌─────────────────────────────────────────┐
│           tb_tenants                    │
├─────────────────────────────────────────┤
│ tenant_id UUID PK                       │
│ tenant_key VARCHAR(50) UNIQUE           │
│ business_name VARCHAR                   │
│ contact_email VARCHAR                   │
│ contact_phone VARCHAR(20)               │
│ active BOOLEAN                          │
│ timezone VARCHAR(50) ✨ NOVO!           │  ← Padrão: 'America/Sao_Paulo'
│ created_at TIMESTAMP                    │
│ updated_at TIMESTAMP                    │
└─────────────────────────────────────────┘
          │
          │ 1:N
          ▼
┌─────────────────────────────────────────┐
│        tb_professionals                 │
├─────────────────────────────────────────┤
│ professional_id UUID PK                 │
│ tenant FK → tb_tenants                  │
│ name VARCHAR                            │
│ ...                                     │
└─────────────────────────────────────────┘
          │
          │ 1:N
          ▼
┌─────────────────────────────────────────┐
│        tb_appointments                  │
├─────────────────────────────────────────┤
│ appointment_id UUID PK                  │
│ professional FK → tb_professionals      │
│ date DATE                               │
│ start_time TIME                         │
│ end_time TIME                           │
│ ...                                     │
└─────────────────────────────────────────┘
```

---

## 🎯 Decisão: Quando Filtrar?

```
┌────────────────────────────────────────────────────┐
│            Data Solicitada = D                     │
└───────────────────┬────────────────────────────────┘
                    │
                    ▼
            ┌───────────────┐
            │ D < hoje?     │
            └───┬───────┬───┘
                │       │
              SIM      NÃO
                │       │
                ▼       ▼
         ┌──────────┐  ┌───────────┐
         │ return []│  │ D = hoje? │
         └──────────┘  └─┬───────┬─┘
                         │       │
                       SIM      NÃO
                         │       │
                         ▼       ▼
                  ┌────────────────┐  ┌──────────────┐
                  │ Filtra apenas  │  │ Retorna todos│
                  │ slots onde     │  │ os slots     │
                  │ time > now()   │  │ normalmente  │
                  └────────────────┘  └──────────────┘
```

---

## 🔐 Ordem de Validações (Criação de Agendamento)

```
1. ✅ Tenant existe e está ativo
2. ✅ Professional pertence ao tenant
3. ✨ Não está no passado                 ← NOVA!
4. ✅ Data não está bloqueada
5. ✅ Serviços existem
6. ✅ Professional executa todos serviços
7. ✅ Horário dentro do expediente
8. ✅ Sem bloqueios de horário
9. ✅ Sem conflitos com outros agendamentos
10. ✅ Salvar no banco
11. ✅ Enviar WhatsApp
```

**Ordem é importante!** Validação de "passado" é feita CEDO para falhar rápido.

---

## 📊 Matriz de Decisão

| Data | Hora | Resultado |
|------|------|-----------|
| Passado | Qualquer | ❌ Rejeitado |
| Hoje | < Agora | ❌ Rejeitado |
| Hoje | = Agora | ❌ Rejeitado |
| Hoje | > Agora | ✅ Permitido |
| Futuro | Qualquer | ✅ Permitido |

---

Diagrama criado para facilitar o entendimento da implementação! 🎨

