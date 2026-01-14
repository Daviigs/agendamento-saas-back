# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [1.0.0] - 2026-01-14

### ✨ Adicionado

#### Sistema Avançado de Bloqueio de Horários

##### Novas Entidades
- **TenantWorkingHoursEntity**: Gerenciamento de horários de trabalho por tenant
- **BlockedTimeSlotEntity**: Bloqueios de horários específicos e recorrentes

##### Novos Repositórios
- **TenantWorkingHoursRepository**: CRUD de horários de trabalho
- **BlockedTimeSlotRepository**: CRUD de bloqueios com queries otimizadas

##### Novos Serviços
- **TenantWorkingHoursService**: Gestão de horários de trabalho personalizados
- **BlockedTimeSlotService**: Gestão de bloqueios de horários
- **AvailableTimeSlotsService**: Cálculo inteligente de horários disponíveis

##### Novos Controllers/Endpoints
- **TenantWorkingHoursController** (`/working-hours`)
  - `GET /working-hours` - Consultar horário de trabalho
  - `POST /working-hours` - Configurar/atualizar horário de trabalho
  - `DELETE /working-hours` - Remover configuração

- **BlockedTimeSlotController** (`/blocked-time-slots`)
  - `POST /blocked-time-slots/specific` - Bloquear horário específico
  - `POST /blocked-time-slots/recurring` - Bloquear horário recorrente
  - `DELETE /blocked-time-slots/{id}` - Desbloquear horário
  - `GET /blocked-time-slots` - Listar todos os bloqueios
  - `GET /blocked-time-slots/specific` - Listar bloqueios específicos
  - `GET /blocked-time-slots/recurring` - Listar bloqueios recorrentes
  - `GET /blocked-time-slots/date/{date}` - Listar bloqueios de uma data

##### Novos DTOs
- **TenantWorkingHoursRequest**: Configuração de horários de trabalho
- **BlockSpecificTimeSlotRequest**: Bloqueio de horário em data específica
- **BlockRecurringTimeSlotRequest**: Bloqueio de horário recorrente

##### Banco de Dados
- Nova tabela: `tb_tenant_working_hours`
- Nova tabela: `tb_blocked_time_slots`
- Índices otimizados para performance
- Script SQL: `create_advanced_blocking_tables.sql`

##### Documentação
- **ADVANCED_BLOCKING_SYSTEM.md**: Documentação completa do sistema
- **MIGRATION_GUIDE.md**: Guia de migração passo a passo
- **ARCHITECTURE_DIAGRAM.md**: Diagramas de arquitetura
- **API_EXAMPLES.json**: Collection de exemplos
- **IMPLEMENTATION_SUMMARY.md**: Resumo da implementação

##### Testes
- **AdvancedBlockingSystemTest**: Suite de testes unitários

### 🔄 Modificado

#### AppointmentsService
- Atualizado para usar `AvailableTimeSlotsService` no cálculo de horários disponíveis
- Removidas constantes de horário fixo global (`BUSINESS_START`, `BUSINESS_END`)
- Adicionada validação de bloqueios de horário na criação de agendamentos
- Validação de horário de trabalho agora considera configuração por tenant
- Método `getAvailableTimeSlots()` delegado para novo serviço

#### Validações de Agendamento
Agora inclui 5 validações:
1. ✅ Data não bloqueada completamente (mantido)
2. ✅ Horário dentro do expediente do tenant (novo - personalizado)
3. ✅ Sem bloqueios de horário no período (novo)
4. ✅ Sem conflitos com agendamentos (mantido - melhorado)
5. ✅ Serviços válidos (mantido)

### 📊 Funcionalidades

#### Configuração de Horários por Tenant
- Cada tenant pode ter horário de trabalho personalizado
- Configuração de intervalo entre slots
- Horário padrão (09:00-18:00) se não configurado

#### Bloqueio de Horários Específicos
- Bloquear intervalo de tempo em data específica
- Exemplo: 14:00-16:00 no dia 25/01/2026
- Validação de conflitos automática
- Desbloquear removendo o registro

#### Bloqueio de Horários Recorrentes
- Bloquear intervalo de tempo por dia da semana
- Exemplo: 12:00-13:00 todas as segundas-feiras
- Aplica-se indefinidamente até remoção
- Validação de conflitos com outros recorrentes

#### Cálculo Inteligente de Disponibilidade
Considera múltiplos fatores:
- Horário de trabalho do tenant
- Bloqueios de dia inteiro (sistema antigo)
- Bloqueios de horários específicos (novo)
- Bloqueios recorrentes (novo)
- Agendamentos existentes

### ⚡ Performance

#### Índices de Banco de Dados
- Índice para busca por tenant em horários de trabalho
- Índice composto para bloqueios específicos
- Índice parcial para bloqueios recorrentes
- Índice para verificação de conflitos

#### Queries Otimizadas
- Query JPQL para detecção de conflitos em datas específicas
- Query JPQL para detecção de conflitos em bloqueios recorrentes
- Uso eficiente de filtros WHERE em índices parciais

### 🔒 Segurança
- Validação de tenant em todos os novos endpoints
- Isolamento de dados por tenant mantido
- Validação de permissões ao remover bloqueios
- Input validation com Bean Validation

### 🎓 Boas Práticas
- Clean Code: Código bem estruturado e documentado
- SOLID: Separação clara de responsabilidades
- DDD: Entidades e serviços bem definidos
- RESTful: API padronizada
- Javadoc: Documentação completa em todos os métodos públicos

### 🔙 Retrocompatibilidade
- ✅ Sistema antigo de `BlockedDayEntity` mantido
- ✅ Endpoints existentes funcionam normalmente
- ✅ Sem breaking changes
- ✅ Migração opcional e gradual

### 📝 Exemplos de Uso

#### Configurar horário de trabalho
```bash
POST /working-hours
X-Tenant-Id: kc
{
  "startTime": "09:00",
  "endTime": "18:00",
  "slotIntervalMinutes": 30
}
```

#### Bloquear horário específico
```bash
POST /blocked-time-slots/specific
X-Tenant-Id: kc
{
  "date": "2026-01-25",
  "startTime": "14:00",
  "endTime": "16:00",
  "reason": "Reunião externa"
}
```

#### Bloquear horário recorrente
```bash
POST /blocked-time-slots/recurring
X-Tenant-Id: kc
{
  "dayOfWeek": "MONDAY",
  "startTime": "12:00",
  "endTime": "13:00",
  "reason": "Almoço"
}
```

### 🐛 Corrigido
- Horários fixos globais substituídos por configuração por tenant
- Flexibilidade na gestão de disponibilidade
- Impossibilidade de bloquear apenas parte de um dia (agora possível)

### 🗑️ Deprecated
- Nenhuma funcionalidade foi depreciada (retrocompatibilidade total)

---

## [0.0.1] - 2025-12-XX

### Adicionado
- Implementação inicial do sistema de agendamentos
- Sistema de multi-tenancy
- Gestão de serviços
- Bloqueio de dias inteiros
- Notificações via WhatsApp
- Lembretes automáticos

---

## Tipos de Mudanças

- `Adicionado` - Novas funcionalidades
- `Modificado` - Mudanças em funcionalidades existentes
- `Deprecated` - Funcionalidades que serão removidas em breve
- `Removido` - Funcionalidades removidas
- `Corrigido` - Correções de bugs
- `Segurança` - Correções de vulnerabilidades

---

**Formato**: [Major.Minor.Patch]
- **Major**: Mudanças incompatíveis com versões anteriores
- **Minor**: Novas funcionalidades compatíveis com versões anteriores
- **Patch**: Correções de bugs compatíveis com versões anteriores

