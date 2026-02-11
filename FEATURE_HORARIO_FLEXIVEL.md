# 🕐 Feature: Horário Flexível

## 📋 Resumo

Esta funcionalidade adiciona uma flag booleana `horarioFlexivel` aos horários de trabalho dos tenants, permitindo controlar como o sistema valida agendamentos em relação a bloqueios de horários e limites de funcionamento.

## 🎯 Objetivo

Permitir que o sistema atenda:
- **Negócios com agenda rígida** (clínicas, consultórios) - onde bloqueios são barreiras absolutas
- **Negócios com agenda flexível** (salões, prestadores autônomos) - onde bloqueios não impedem a continuidade do atendimento

## 📌 Regras de Negócio

### ✅ `horarioFlexivel = true` (Modo Flexível)

**Comportamento:**
- Agendamentos PODEM ultrapassar o horário final de funcionamento
- Agendamentos PODEM atravessar intervalos de horários bloqueados
- Apenas o horário de INÍCIO deve estar disponível (não bloqueado)
- O sistema permite que o horário final do agendamento avance por cima de horários bloqueados

**Exemplo:**
```
Horário de funcionamento: 09:00 - 18:00
Horário bloqueado: 12:00 - 13:00 (almoço)
Serviço: 90 minutos

Horários disponíveis incluem:
- 11:00 ✅ (termina às 12:30, atravessa o bloqueio de almoço)
- 12:00 ❌ (início está bloqueado)
- 13:00 ✅ (termina às 14:30, após o horário de trabalho se necessário)
- 17:00 ✅ (termina às 18:30, ultrapassa o expediente)
```

### ❌ `horarioFlexivel = false` (Modo Rígido) - PADRÃO

**Comportamento:**
- Agendamentos NÃO PODEM ultrapassar o horário final de funcionamento
- Agendamentos NÃO PODEM invadir qualquer intervalo de horário bloqueado
- Apenas horários que comportem INTEGRALMENTE a duração do serviço são exibidos
- Horários bloqueados são barreiras absolutas

**Exemplo:**
```
Horário de funcionamento: 09:00 - 18:00
Horário bloqueado: 12:00 - 13:00 (almoço)
Serviço: 90 minutos

Horários disponíveis incluem:
- 09:00 ✅ (termina às 10:30, não conflita)
- 10:30 ✅ (termina às 12:00, exatamente no início do bloqueio)
- 11:00 ❌ (termina às 12:30, invade o horário de almoço)
- 12:00 ❌ (início está bloqueado)
- 13:00 ✅ (termina às 14:30, não conflita)
- 16:30 ✅ (termina às 18:00, exatamente no fim do expediente)
- 17:00 ❌ (termina às 18:30, ultrapassa o expediente)
```

## 🏗️ Arquitetura da Implementação

### 1. Migration SQL (V4)

```sql
-- V4__add_horario_flexivel_column.sql
ALTER TABLE tb_tenant_working_hours 
ADD COLUMN horario_flexivel BOOLEAN NOT NULL DEFAULT false;
```

**Localização:** `src/main/resources/db/migration/V4__add_horario_flexivel_column.sql`

### 2. Entidade

**Arquivo:** `TenantWorkingHoursEntity.java`

**Campo adicionado:**
```java
@NotNull
@Column(name = "horario_flexivel", nullable = false)
private Boolean horarioFlexivel = false;
```

### 3. DTO

**Arquivo:** `TenantWorkingHoursRequest.java`

**Campo adicionado:**
```java
private Boolean horarioFlexivel = false;
```

### 4. Service Layer

**Arquivo:** `TenantWorkingHoursService.java`

**Métodos alterados/adicionados:**

1. `configureWorkingHours()` - Agora aceita o parâmetro `horarioFlexivel`
2. `updateHorarioFlexivel()` - Novo método para atualizar apenas a flag

**Arquivo:** `AvailableTimeSlotsService.java`

**Métodos alterados:**

1. `wouldEndTimeConflictWithBlockedSlots()` - Aplica lógica condicional baseada na flag
2. `isTimeSlotAvailable()` - Valida disponibilidade considerando o modo flexível/rígido
3. `getAvailableTimeSlotsForProfessional()` - Adiciona logs informativos
4. `getAvailableTimeSlots()` - Adiciona logs informativos

### 5. Controller Layer

**Arquivo:** `TenantWorkingHoursController.java`

**Endpoints:**

1. `POST /working-hours` - Atualizado para aceitar `horarioFlexivel`
2. `PATCH /working-hours/horario-flexivel` - Novo endpoint para atualizar apenas a flag

## 🔌 API Endpoints

### 1. Configurar/Atualizar Horário de Trabalho

```http
POST /working-hours
Content-Type: application/json
X-Tenant-Id: kc

{
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotIntervalMinutes": 30,
  "horarioFlexivel": true
}
```

**Resposta (200 OK):**
```json
{
  "id": "uuid",
  "tenantId": "kc",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotIntervalMinutes": 30,
  "horarioFlexivel": true,
  "active": true
}
```

### 2. Atualizar Apenas Modo Flexível

```http
PATCH /working-hours/horario-flexivel?flexivel=true
X-Tenant-Id: kc
```

**Resposta (200 OK):**
```json
{
  "id": "uuid",
  "tenantId": "kc",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotIntervalMinutes": 30,
  "horarioFlexivel": true,
  "active": true
}
```

### 3. Obter Horário de Trabalho Atual

```http
GET /working-hours
X-Tenant-Id: kc
```

## 🧪 Casos de Teste

### Cenário 1: Modo Rígido (padrão)

```java
// Setup
TenantWorkingHours: 09:00 - 18:00, horarioFlexivel = false
Bloqueio: 12:00 - 13:00
Serviço: 90 minutos (1h30)

// Teste
GET /available-slots?date=2026-02-15&professionalId=uuid&serviceIds=uuid

// Resultado Esperado
[
  "09:00", "09:30", "10:00", "10:30",  // OK: terminam antes do bloqueio
  // "11:00" ❌ removido: terminaria às 12:30 (invade bloqueio)
  // "11:30" ❌ removido: terminaria às 13:00 (invade bloqueio)
  "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30"
  // "17:00" ❌ removido: terminaria às 18:30 (ultrapassa expediente)
]
```

### Cenário 2: Modo Flexível

```java
// Setup
TenantWorkingHours: 09:00 - 18:00, horarioFlexivel = true
Bloqueio: 12:00 - 13:00
Serviço: 90 minutos (1h30)

// Teste
GET /available-slots?date=2026-02-15&professionalId=uuid&serviceIds=uuid

// Resultado Esperado
[
  "09:00", "09:30", "10:00", "10:30",
  "11:00", "11:30",  // ✅ Incluídos: podem atravessar o bloqueio
  // "12:00" ❌ removido: início está bloqueado
  // "12:30" ❌ removido: início está bloqueado
  "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
  "17:00", "17:30"  // ✅ Incluídos: podem ultrapassar o expediente
]
```

## 📊 Logs e Monitoramento

O sistema agora registra o modo de horário em uso:

```
INFO: Modo de horário do profissional 123e4567-e89b-12d3-a456-426614174000: FLEXÍVEL (horarioFlexivel=true)
INFO: Calculando horários disponíveis para profissional...
DEBUG: ✅ Horário flexível ativo: Slot 17:00 permitido (mesmo que termine às 18:30 após expediente/bloqueios)
```

ou

```
INFO: Modo de horário do profissional 123e4567-e89b-12d3-a456-426614174000: RÍGIDO (horarioFlexivel=false)
DEBUG: ❌ BLOQUEADO: Slot 11:00 + 90 min terminaria às 12:30 (bloqueio: 12:00 - 13:00)
```

## 🔄 Migração de Dados Existentes

Todos os horários de trabalho existentes terão `horarioFlexivel = false` por padrão após a execução da migration, garantindo que o comportamento atual seja preservado (mais restritivo e seguro).

## ✅ Checklist de Implementação

- [x] Migration SQL criada (V4)
- [x] Campo adicionado na entidade `TenantWorkingHoursEntity`
- [x] Campo adicionado no DTO `TenantWorkingHoursRequest`
- [x] Service `TenantWorkingHoursService` atualizado
- [x] Service `AvailableTimeSlotsService` atualizado com lógica condicional
- [x] Controller atualizado com novo endpoint PATCH
- [x] Logs informativos adicionados
- [x] Documentação criada

## 🚀 Como Usar

### Para Salões e Prestadores Autônomos (Agenda Flexível)

```bash
# Ativar modo flexível
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: kc"
```

### Para Clínicas e Consultórios (Agenda Rígida)

```bash
# Manter ou ativar modo rígido
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=false" \
  -H "X-Tenant-Id: kc"
```

## 📝 Notas Importantes

1. **Compatibilidade**: O código existente continua funcionando sem alterações
2. **Padrão Seguro**: Novos tenants usam modo rígido por padrão
3. **Validações**: Agendamentos existentes sempre validam conflitos entre si, independente do modo
4. **Bloqueios de Dia Inteiro**: Sempre impedem agendamentos, independente do modo
5. **Horário de Início**: Mesmo no modo flexível, o horário de início não pode estar bloqueado

## 🔗 Arquivos Relacionados

- `V4__add_horario_flexivel_column.sql`
- `TenantWorkingHoursEntity.java`
- `TenantWorkingHoursRequest.java`
- `TenantWorkingHoursService.java`
- `AvailableTimeSlotsService.java`
- `TenantWorkingHoursController.java`

