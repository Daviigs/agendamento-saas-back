# Sistema Avançado de Bloqueio de Horários

## 📋 Visão Geral

Este sistema implementa um controle avançado de bloqueio de horários para agendamentos, com suporte a:

- ✅ Horários de trabalho personalizados por profissional (tenant)
- ✅ Bloqueio de intervalos de horários em datas específicas
- ✅ Bloqueio de intervalos recorrentes por dia da semana
- ✅ Desbloqueio de horários
- ✅ Cálculo inteligente de horários disponíveis

## 🏗️ Arquitetura

### Entidades Criadas

1. **TenantWorkingHoursEntity** - Horários de trabalho por profissional
2. **BlockedTimeSlotEntity** - Bloqueios de horários (específicos ou recorrentes)

### Repositórios

1. **TenantWorkingHoursRepository** - Gerenciamento de horários de trabalho
2. **BlockedTimeSlotRepository** - Gerenciamento de bloqueios com queries otimizadas

### Serviços

1. **TenantWorkingHoursService** - Configuração de horários de trabalho
2. **BlockedTimeSlotService** - Gerenciamento de bloqueios de horários
3. **AvailableTimeSlotsService** - Cálculo de horários disponíveis

### Controllers

1. **TenantWorkingHoursController** - `/working-hours`
2. **BlockedTimeSlotController** - `/blocked-time-slots`
3. **AppointmentsController** - Atualizado para usar o novo sistema

## 🔧 Funcionalidades Implementadas

### 1. Configuração de Horários de Trabalho

Cada profissional (tenant) pode ter seu próprio horário de funcionamento.

**Endpoint:** `POST /working-hours`

**Exemplo de Request:**
```json
{
  "startTime": "09:00",
  "endTime": "18:00",
  "slotIntervalMinutes": 30
}
```

**Exemplos de Configuração:**
- **kc**: 09:00 às 18:00 (intervalos de 30 minutos)
- **mjs**: 07:00 às 16:00 (intervalos de 30 minutos)

### 2. Bloqueio de Horário Específico

Bloquear um intervalo de tempo em uma data específica.

**Endpoint:** `POST /blocked-time-slots/specific`

**Exemplo de Request:**
```json
{
  "date": "2026-01-25",
  "startTime": "14:00",
  "endTime": "16:00",
  "reason": "Reunião externa"
}
```

### 3. Bloqueio de Horário Recorrente

Bloquear um intervalo de tempo em um dia da semana de forma recorrente.

**Endpoint:** `POST /blocked-time-slots/recurring`

**Exemplo de Request:**
```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "16:00",
  "endTime": "17:00",
  "reason": "Horário de limpeza"
}
```

**Dias da Semana Válidos:**
- MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY

### 4. Desbloquear Horário

Remove um bloqueio existente, tornando o horário disponível novamente.

**Endpoint:** `DELETE /blocked-time-slots/{blockedSlotId}`

### 5. Consultar Horários Disponíveis

Retorna todos os horários disponíveis para agendamento considerando:
- Horário de trabalho do tenant
- Bloqueios específicos
- Bloqueios recorrentes
- Dias bloqueados completamente
- Agendamentos existentes

**Endpoint:** `GET /appointments/available-slots?date=2026-01-20`

**Exemplo de Response:**
```json
[
  "09:00",
  "09:30",
  "10:00",
  "10:30",
  "11:00",
  "11:30",
  "13:00",
  "13:30",
  "14:00"
]
```

## 📊 Endpoints Completos

### Horários de Trabalho

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/working-hours` | Obtém horário de trabalho do tenant |
| POST | `/working-hours` | Configura/atualiza horário de trabalho |
| DELETE | `/working-hours` | Remove configuração (volta ao padrão) |

### Bloqueios de Horários

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/blocked-time-slots/specific` | Bloqueia horário em data específica |
| POST | `/blocked-time-slots/recurring` | Bloqueia horário recorrente |
| DELETE | `/blocked-time-slots/{id}` | Remove bloqueio (desbloqueia) |
| GET | `/blocked-time-slots` | Lista todos os bloqueios |
| GET | `/blocked-time-slots/specific` | Lista apenas bloqueios específicos |
| GET | `/blocked-time-slots/recurring` | Lista apenas bloqueios recorrentes |
| GET | `/blocked-time-slots/date/{date}` | Bloqueios ativos em uma data |

### Agendamentos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/appointments/available-slots?date={date}` | Horários disponíveis |
| POST | `/appointments` | Cria agendamento (valida bloqueios) |

## 🔍 Validações Implementadas

### Na Criação de Agendamento:

1. ✅ Verifica se o dia não está bloqueado completamente
2. ✅ Valida se o horário está dentro do expediente do tenant
3. ✅ Verifica se não há bloqueios de horário no período
4. ✅ Valida conflitos com agendamentos existentes

### Na Criação de Bloqueio:

1. ✅ Valida se o intervalo é válido (início < término)
2. ✅ Verifica se está dentro do horário de trabalho
3. ✅ Impede conflitos com bloqueios existentes

## 💾 Banco de Dados

### Tabelas Criadas:

1. **tb_tenant_working_hours** - Horários de trabalho
2. **tb_blocked_time_slots** - Bloqueios de horários

### Script SQL:

Execute o script: `src/main/resources/db/create_advanced_blocking_tables.sql`

## 🎯 Exemplos de Uso

### Cenário 1: Configurar Horário de Trabalho

```bash
# Tenant: kc trabalha das 09:00 às 18:00
curl -X POST http://localhost:8080/working-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "startTime": "09:00",
    "endTime": "18:00",
    "slotIntervalMinutes": 30
  }'
```

### Cenário 2: Bloquear Horário de Almoço em Data Específica

```bash
# Bloquear das 12:00 às 13:00 no dia 25/01/2026
curl -X POST http://localhost:8080/blocked-time-slots/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-01-25",
    "startTime": "12:00",
    "endTime": "13:00",
    "reason": "Almoço"
  }'
```

### Cenário 3: Bloquear Horário de Limpeza Toda Segunda

```bash
# Bloquear das 16:00 às 17:00 todas as segundas-feiras
curl -X POST http://localhost:8080/blocked-time-slots/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "dayOfWeek": "MONDAY",
    "startTime": "16:00",
    "endTime": "17:00",
    "reason": "Horário de limpeza semanal"
  }'
```

### Cenário 4: Consultar Horários Disponíveis

```bash
# Ver horários disponíveis para 20/01/2026
curl -X GET "http://localhost:8080/appointments/available-slots?date=2026-01-20" \
  -H "X-Tenant-Id: kc"
```

### Cenário 5: Desbloquear Horário

```bash
# Remover um bloqueio (usar ID retornado na criação)
curl -X DELETE http://localhost:8080/blocked-time-slots/{blockedSlotId} \
  -H "X-Tenant-Id: kc"
```

## 🧪 Testes

### Validar Fluxo Completo:

1. Configure horário de trabalho do tenant
2. Crie bloqueios específicos e recorrentes
3. Consulte horários disponíveis
4. Tente criar agendamento em horário bloqueado (deve falhar)
5. Tente criar agendamento em horário disponível (deve funcionar)
6. Desbloqueie um horário
7. Verifique que o horário aparece como disponível

## 📈 Melhorias Futuras

- [ ] Bloqueio de horários com duração variável por serviço
- [ ] Suporte a múltiplos profissionais por agendamento
- [ ] Configuração de buffers entre agendamentos
- [ ] Notificações de bloqueios ao cliente
- [ ] Interface administrativa para gestão visual de bloqueios
- [ ] Relatórios de ocupação por período
- [ ] Bloqueios temporários (com data de expiração)

## 🔐 Segurança

- Todos os endpoints requerem `X-Tenant-Id` no header
- Validação automática via `TenantInterceptor`
- Isolamento de dados por tenant
- Validação de permissões na remoção de bloqueios

## 🐛 Tratamento de Erros

### Erros Comuns:

- **400 Bad Request**: Dados inválidos na requisição
- **404 Not Found**: Bloqueio ou configuração não encontrada
- **409 Conflict**: Conflito de horários ou duplicação
- **422 Business Exception**: Violação de regra de negócio

## 📝 Observações Importantes

1. **Horários Padrão**: Se um tenant não tiver configuração, usa 09:00-18:00
2. **Prioridade**: Bloqueios de dia inteiro têm prioridade sobre bloqueios de horário
3. **Sobreposição**: Não é permitido criar bloqueios que se sobreponham
4. **Tenant Context**: Todas as operações respeitam o contexto do tenant atual

## 🤝 Contribuição

Este sistema foi desenvolvido seguindo princípios de:
- **Clean Code**: Código limpo e legível
- **SOLID**: Separação de responsabilidades
- **DDD**: Domain-Driven Design
- **RESTful**: Arquitetura REST padronizada

---

**Desenvolvido para:** Sistema de Agendamento Lash Salão KC  
**Data:** Janeiro 2026  
**Versão:** 1.0.0

