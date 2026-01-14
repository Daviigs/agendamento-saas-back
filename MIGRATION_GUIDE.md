# Guia de Migração - Sistema Avançado de Bloqueio de Horários

## 📋 Visão Geral

Este guia orienta a migração do sistema antigo (horários fixos globais) para o novo sistema avançado de bloqueio de horários com configuração por tenant.

## 🔄 Principais Mudanças

### Antes (Sistema Antigo)
- ❌ Horários fixos globais (09:00 - 18:00)
- ❌ Bloqueio apenas de dias inteiros
- ❌ Sem suporte a bloqueio de horários específicos
- ❌ Sem configuração por profissional

### Depois (Sistema Novo)
- ✅ Horários configuráveis por tenant
- ✅ Bloqueio de dias inteiros (mantido)
- ✅ Bloqueio de intervalos de horários específicos
- ✅ Bloqueio recorrente por dia da semana
- ✅ Desbloquear horários
- ✅ Cálculo inteligente de disponibilidade

## 📊 Passos de Migração

### 1. Executar Script SQL

Execute o script de criação das novas tabelas:

```bash
psql -U seu_usuario -d seu_banco -f src/main/resources/db/create_advanced_blocking_tables.sql
```

Ou execute manualmente:
- Crie a tabela `tb_tenant_working_hours`
- Crie a tabela `tb_blocked_time_slots`

### 2. Configurar Horários de Trabalho dos Tenants

Para cada tenant existente, configure o horário de trabalho:

**Exemplo para tenant 'kc':**
```bash
curl -X POST http://localhost:8080/working-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "startTime": "09:00",
    "endTime": "18:00",
    "slotIntervalMinutes": 30
  }'
```

**Exemplo para tenant 'mjs':**
```bash
curl -X POST http://localhost:8080/working-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: mjs" \
  -d '{
    "startTime": "07:00",
    "endTime": "16:00",
    "slotIntervalMinutes": 30
  }'
```

### 3. Migrar Bloqueios Existentes (Se Aplicável)

Se você tinha alguma lógica de bloqueios customizada, migre-os para o novo sistema:

**Exemplo - Bloquear horário de almoço recorrente:**
```bash
curl -X POST http://localhost:8080/blocked-time-slots/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "dayOfWeek": "MONDAY",
    "startTime": "12:00",
    "endTime": "13:00",
    "reason": "Horário de almoço"
  }'
```

### 4. Atualizar Frontend (Se Necessário)

#### Endpoints Mantidos (Compatibilidade)
Estes endpoints continuam funcionando normalmente:
- `GET /appointments/available-slots?date={date}` - Agora considera o novo sistema
- `POST /appointments` - Valida automaticamente os novos bloqueios
- `GET /blocked-days` - Sistema antigo mantido para bloqueios de dia inteiro

#### Novos Endpoints Disponíveis
- `GET /working-hours` - Consultar horário de trabalho
- `POST /working-hours` - Configurar horário de trabalho
- `POST /blocked-time-slots/specific` - Bloquear horário específico
- `POST /blocked-time-slots/recurring` - Bloquear horário recorrente
- `DELETE /blocked-time-slots/{id}` - Desbloquear horário
- `GET /blocked-time-slots` - Listar todos os bloqueios

### 5. Teste de Validação

Execute os seguintes testes para garantir que tudo funciona:

#### Teste 1: Consultar Horários Disponíveis
```bash
curl -X GET "http://localhost:8080/appointments/available-slots?date=2026-01-20" \
  -H "X-Tenant-Id: kc"
```
**Esperado:** Lista de horários respeitando configuração do tenant

#### Teste 2: Criar Bloqueio Específico
```bash
curl -X POST http://localhost:8080/blocked-time-slots/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-01-20",
    "startTime": "14:00",
    "endTime": "15:00",
    "reason": "Teste de bloqueio"
  }'
```
**Esperado:** Status 201 Created

#### Teste 3: Verificar Bloqueio Aplicado
```bash
curl -X GET "http://localhost:8080/appointments/available-slots?date=2026-01-20" \
  -H "X-Tenant-Id: kc"
```
**Esperado:** Lista não deve conter 14:00

#### Teste 4: Tentar Agendar em Horário Bloqueado
```bash
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "serviceIds": ["id-servico"],
    "date": "2026-01-20",
    "startTime": "14:00",
    "userName": "Teste",
    "userPhone": "11999999999"
  }'
```
**Esperado:** Status 422 - Erro informando horário bloqueado

#### Teste 5: Desbloquear Horário
```bash
curl -X DELETE "http://localhost:8080/blocked-time-slots/{id}" \
  -H "X-Tenant-Id: kc"
```
**Esperado:** Status 204 No Content

## 🔧 Configurações Recomendadas

### Para Salões de Beleza
```json
{
  "startTime": "09:00",
  "endTime": "19:00",
  "slotIntervalMinutes": 30
}
```

### Para Clínicas/Consultórios
```json
{
  "startTime": "08:00",
  "endTime": "18:00",
  "slotIntervalMinutes": 30
}
```

### Para Profissionais Autônomos
```json
{
  "startTime": "10:00",
  "endTime": "20:00",
  "slotIntervalMinutes": 60
}
```

## 📝 Bloqueios Comuns

### Horário de Almoço Diário
```bash
# Para cada dia da semana (MONDAY, TUESDAY, etc)
curl -X POST http://localhost:8080/blocked-time-slots/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "dayOfWeek": "MONDAY",
    "startTime": "12:00",
    "endTime": "13:00",
    "reason": "Horário de almoço"
  }'
```

### Folga Semanal
```bash
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "dayOfWeek": "SUNDAY",
    "reason": "Domingo - Fechado"
  }'
```

### Feriado Específico
```bash
curl -X POST http://localhost:8080/blocked-days/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-12-25",
    "reason": "Natal"
  }'
```

### Reunião Pontual
```bash
curl -X POST http://localhost:8080/blocked-time-slots/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-01-30",
    "startTime": "15:00",
    "endTime": "17:00",
    "reason": "Reunião com fornecedor"
  }'
```

## ⚠️ Pontos de Atenção

### 1. Compatibilidade com Sistema Antigo
- O sistema antigo de `BlockedDayEntity` foi **mantido**
- Bloqueios de dia inteiro continuam funcionando normalmente
- Novos bloqueios de horário são **complementares**

### 2. Prioridade de Bloqueios
1. **Dia inteiro bloqueado** (via `BlockedDayService`) - prioridade máxima
2. **Horários bloqueados** (via `BlockedTimeSlotService`)
3. **Agendamentos existentes**

### 3. Validações Automáticas
O sistema valida automaticamente:
- Horário dentro do expediente do tenant
- Não há bloqueios de horário
- Não há conflitos com agendamentos
- Não há bloqueios de dia inteiro

### 4. Comportamento Padrão
Se um tenant **não** configurar horário de trabalho:
- Usa horário padrão: **09:00 às 18:00**
- Intervalo padrão: **30 minutos**

## 🔍 Troubleshooting

### Problema: Horários não aparecem como esperado

**Solução:**
1. Verifique se o tenant tem configuração:
```bash
curl -X GET http://localhost:8080/working-hours \
  -H "X-Tenant-Id: kc"
```

2. Verifique bloqueios ativos na data:
```bash
curl -X GET http://localhost:8080/blocked-time-slots/date/2026-01-20 \
  -H "X-Tenant-Id: kc"
```

### Problema: Não consigo criar agendamento

**Possíveis Causas:**
1. Dia inteiro bloqueado
2. Horário bloqueado especificamente
3. Horário bloqueado por recorrência
4. Fora do horário de trabalho do tenant
5. Conflito com agendamento existente

**Solução:**
Consulte `/appointments/available-slots` para ver horários realmente disponíveis

### Problema: Erro ao criar bloqueio

**Causa Comum:** Horário fora do expediente configurado

**Solução:** Ajuste primeiro o horário de trabalho ou crie bloqueio dentro do expediente

## 📈 Monitoramento

Após a migração, monitore:
- Logs de erro relacionados a bloqueios
- Taxa de sucesso de criação de agendamentos
- Feedback de usuários sobre horários disponíveis

## 🎯 Checklist de Migração

- [ ] Executar script SQL de criação de tabelas
- [ ] Configurar horário de trabalho para cada tenant
- [ ] Migrar bloqueios customizados (se existirem)
- [ ] Executar testes de validação
- [ ] Atualizar documentação da API (se necessário)
- [ ] Treinar usuários administrativos
- [ ] Monitorar sistema por 1 semana

## 📞 Suporte

Em caso de dúvidas ou problemas durante a migração:
1. Consulte o arquivo `ADVANCED_BLOCKING_SYSTEM.md`
2. Execute os testes unitários
3. Verifique logs da aplicação

---

**Data do Guia:** Janeiro 2026  
**Versão do Sistema:** 1.0.0

