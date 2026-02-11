# 🚫 Feature: Prevenção de Agendamentos no Passado

## 📋 Contexto

Atualmente, o sistema permite:
- Retornar horários que já passaram quando a consulta é para o dia atual
- Criar agendamentos em horários retroativos

Isso causa problemas de UX e permite dados inconsistentes no sistema.

## 🎯 Objetivo

Implementar validação para:
1. **Filtrar horários passados** na listagem de horários disponíveis do dia atual
2. **Bloquear criação de agendamentos** em datas/horários que já passaram

## 📌 Regras de Negócio

### 1. Timezone
- Cada tenant possui seu próprio timezone (padrão: `America/Sao_Paulo`)
- Todas as comparações de data/hora consideram o timezone do tenant

### 2. Listagem de Horários Disponíveis

Para uma data `D` solicitada:

| Condição | Comportamento |
|----------|--------------|
| D < hoje | ❌ Retornar lista vazia |
| D = hoje | ✅ Retornar apenas horários > horário atual |
| D > hoje | ✅ Retornar todos os horários normalmente |

### 3. Criação de Agendamento

Validação defensiva:
- Se `date < dataAtual` → rejeitar
- Se `date = dataAtual` AND `startTime <= horaAtual` → rejeitar

### 4. Comparação de Data/Hora

```java
LocalDateTime agendamentoDateTime = LocalDateTime.of(date, startTime);
ZonedDateTime agendamentoZoned = agendamentoDateTime.atZone(tenantZoneId);
ZonedDateTime agora = ZonedDateTime.now(tenantZoneId);

if (agendamentoZoned.isBefore(agora) || agendamentoZoned.isEqual(agora)) {
    // REJEITAR
}
```

## 🧪 Casos de Teste

### Cenário 1: Data Passada
```
Hoje: 2026-02-15
Consulta: 2026-02-10

Resultado: []
```

### Cenário 2: Dia Atual - Horários Passados
```
Hoje: 2026-02-15 11:30
Consulta: 2026-02-15

Horários disponíveis originais:
- 10:00 ❌
- 10:30 ❌
- 11:00 ❌
- 11:30 ❌ (igual ao atual)
- 12:00 ✅
- 12:30 ✅
- 13:00 ✅

Resultado: [12:00, 12:30, 13:00, ...]
```

### Cenário 3: Dia Futuro
```
Hoje: 2026-02-15 11:30
Consulta: 2026-02-16

Resultado: todos os horários conforme regras normais
```

### Cenário 4: Tentativa de Criar Agendamento no Passado
```
Hoje: 2026-02-15 14:00

POST /appointments
{
  "date": "2026-02-15",
  "startTime": "13:00"
}

Resultado: 400 Bad Request
"Não é possível agendar para um horário que já passou"
```

## 🛠️ Implementação

### 1. Adicionar Timezone ao Tenant

**TenantEntity.java**
```java
@Column(name = "timezone", length = 50)
private String timezone = "America/Sao_Paulo";
```

**Migration SQL**
```sql
ALTER TABLE tb_tenants ADD COLUMN timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo';
UPDATE tb_tenants SET timezone = 'America/Sao_Paulo' WHERE timezone IS NULL;
```

### 2. Criar DateTimeService

Serviço utilitário para obter data/hora no timezone do tenant.

### 3. Modificar AvailableTimeSlotsService

Adicionar filtro para remover horários passados quando `date = hoje`.

### 4. Modificar AppointmentsService

Adicionar validação `validateNotInPast(date, startTime)`.

## ✅ Checklist de Implementação

- [ ] Adicionar campo `timezone` em `TenantEntity`
- [ ] Criar migration para adicionar coluna `timezone`
- [ ] Criar `DateTimeService` para obter data/hora do tenant
- [ ] Adicionar método de validação em `AvailableTimeSlotsService`
- [ ] Adicionar filtro de horários passados em `getAvailableTimeSlotsForProfessional`
- [ ] Adicionar validação `validateNotInPast` em `AppointmentsService.createAppointment`
- [ ] Testar com diferentes timezones
- [ ] Testar cenários de borda (meia-noite, mudança de dia)

## 🚨 Observações Importantes

1. **Frontend**: O backend é a fonte da verdade. O frontend pode fazer validações de UX, mas não deve confiar apenas nelas.

2. **Timezone**: Por padrão, usamos `America/Sao_Paulo`. Isso pode ser configurado por tenant no futuro.

3. **Performance**: A validação de horários passados é feita em memória após buscar os slots, não impacta performance do banco.

4. **Exceções e Bloqueios**: Esta regra é independente de bloqueios recorrentes, pontuais ou exceções. Sempre valida se o horário já passou.

5. **Precisão**: A comparação considera data + hora, não apenas a data.

## 📊 Fluxo de Validação

```
┌─────────────────────────┐
│ GET /available-slots    │
│ date = D                │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Obter timezone tenant   │
│ Calcular now()          │
└───────────┬─────────────┘
            │
            ▼
      ┌─────┴─────┐
      │ D < hoje? │
      └─────┬─────┘
            │
     ┌──────┴──────┐
     │             │
    SIM           NÃO
     │             │
     ▼             ▼
  retorna []   ┌─────────┐
               │D = hoje?│
               └────┬────┘
                    │
            ┌───────┴────────┐
            │                │
           SIM              NÃO
            │                │
            ▼                ▼
    filtra apenas      retorna todos
    horários > now     os horários
```

## 📝 Mensagens de Erro

```java
// Data no passado
"Não é possível consultar horários disponíveis para uma data que já passou"

// Horário no passado (criação)
"Não é possível agendar para um horário que já passou. Data/hora solicitada: {datetime}, data/hora atual: {now}"
```

