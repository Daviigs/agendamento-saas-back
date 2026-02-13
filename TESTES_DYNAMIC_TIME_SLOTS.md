# 🧪 Guia de Testes - Horários Dinâmicos

## 📋 Objetivo

Validar que o sistema agora permite agendamentos em horários de término de agendamentos anteriores, não apenas na grade fixa.

## 🔧 Pré-requisitos

1. Sistema rodando e conectado ao banco de dados
2. Tenant configurado com:
   - Horário de funcionamento: 09:00 às 18:00
   - Intervalo: 30 minutos
   - Modo: Rígido (horarioFlexivel = false)

## 🧪 Cenários de Teste

### Teste 1: Agendamento com Duração Não-Padrão

**Setup:**
1. Sem agendamentos existentes
2. Criar serviço com duração de 40 minutos

**Passos:**
1. Consultar horários disponíveis para hoje
   ```
   GET /api/available-slots?professionalId={id}&date=2026-02-15
   ```
   
2. **Resultado Esperado:**
   - Deve retornar: 09:00, 09:30, 10:00, 10:30, 11:00, ...

3. Criar agendamento às 09:00 com o serviço de 40 minutos
   ```json
   POST /api/appointments
   {
     "professionalId": "{id}",
     "date": "2026-02-15",
     "startTime": "09:00",
     "serviceIds": ["{serviceId}"]
   }
   ```

4. Consultar horários disponíveis novamente

5. **Resultado Esperado:**
   - Deve retornar: **09:40**, 10:00, 10:30, 11:00, ...
   - ✅ **09:40 deve aparecer na lista!**

6. Criar um segundo agendamento às 09:40
   ```json
   POST /api/appointments
   {
     "professionalId": "{id}",
     "date": "2026-02-15",
     "startTime": "09:40",
     "serviceIds": ["{serviceId}"]
   }
   ```

7. **Resultado Esperado:**
   - ✅ Agendamento criado com sucesso
   - Horário de término: 10:20

### Teste 2: Múltiplos Agendamentos com Durações Variadas

**Setup:**
1. Sem agendamentos existentes
2. Criar serviços:
   - Serviço A: 25 minutos
   - Serviço B: 45 minutos
   - Serviço C: 50 minutos

**Passos:**
1. Criar agendamento 1: 09:00 com Serviço A (25 min) → termina 09:25
2. Criar agendamento 2: 10:00 com Serviço B (45 min) → termina 10:45
3. Criar agendamento 3: 11:30 com Serviço C (50 min) → termina 12:20

4. Consultar horários disponíveis

5. **Resultado Esperado:**
   ```
   09:25  ← Término agend. 1
   09:30
   10:45  ← Término agend. 2
   11:00
   12:20  ← Término agend. 3
   12:30
   13:00
   ...
   ```

### Teste 3: Término na Grade Fixa (Não Duplicar)

**Setup:**
1. Criar serviço com 30 minutos (exatamente o intervalo)

**Passos:**
1. Criar agendamento às 09:00 (termina 09:30)
2. Consultar horários disponíveis

**Resultado Esperado:**
- 09:30 deve aparecer apenas UMA vez
- ❌ Não deve duplicar

### Teste 4: Término Fora do Expediente (Modo Flexível)

**Setup:**
1. Alterar tenant para horarioFlexivel = true
2. Criar serviço com 90 minutos

**Passos:**
1. Criar agendamento às 17:00 (termina 18:30, após o expediente)
2. Consultar horários disponíveis

**Resultado Esperado:**
- 18:30 NÃO deve aparecer (está fora do expediente)
- Apenas horários dentro do expediente devem ser retornados

### Teste 5: Validação de Conflito

**Setup:**
1. Agendamento existente: 09:00 - 09:40

**Passos:**
1. Tentar criar agendamento às 09:20 (conflitaria com existente)

**Resultado Esperado:**
- ❌ Deve retornar erro de conflito
- Mensagem: "Horário não disponível - conflito com agendamento existente"

### Teste 6: Sequência de Agendamentos Encadeados

**Setup:**
1. Sem agendamentos

**Passos:**
1. Criar agend. 1: 09:00 - 09:35 (35 min)
2. Verificar que 09:35 aparece
3. Criar agend. 2: 09:35 - 10:10 (35 min)
4. Verificar que 10:10 aparece
5. Criar agend. 3: 10:10 - 10:45 (35 min)
6. Verificar que 10:45 aparece

**Resultado Esperado:**
- Cada término deve aparecer como próximo horário disponível
- Permite criar sequência completa sem "buracos"

### Teste 7: Horários com Bloqueios

**Setup:**
1. Bloqueio configurado: 12:00 - 13:00 (almoço)
2. Criar serviço de 40 minutos

**Passos:**
1. Criar agendamento às 11:00 (terminaria 11:40)
2. Consultar horários disponíveis

**Resultado Esperado:**
- 11:40 deve aparecer
- 12:00 NÃO deve aparecer (bloqueado)
- Próximo após bloqueio: 13:00

### Teste 8: Data Passada

**Passos:**
1. Consultar horários para data passada (ex: ontem)

**Resultado Esperado:**
- Lista vazia
- Não deve retornar nenhum horário

### Teste 9: Horário Atual (Hoje)

**Setup:**
1. Hora atual: 11:30
2. Agendamento existente: 10:00 - 10:35

**Passos:**
1. Consultar horários disponíveis para hoje

**Resultado Esperado:**
- 10:35 NÃO deve aparecer (está no passado)
- 11:00 NÃO deve aparecer (está no passado)
- 11:30 NÃO deve aparecer (é agora)
- 12:00 deve aparecer
- 12:30 deve aparecer
- etc.

## 📊 Checklist de Validação

- [ ] Teste 1: Agendamento com duração não-padrão
- [ ] Teste 2: Múltiplos agendamentos variados
- [ ] Teste 3: Não duplica horários da grade fixa
- [ ] Teste 4: Não adiciona horários fora do expediente
- [ ] Teste 5: Validação de conflito funciona
- [ ] Teste 6: Sequência encadeada funciona
- [ ] Teste 7: Respeita bloqueios
- [ ] Teste 8: Não retorna horários de datas passadas
- [ ] Teste 9: Filtra horários que já passaram (hoje)

## 🔍 Verificação de Logs

Durante os testes, verificar os logs para:

### Log Esperado - Horário Adicionado
```
➕ Adicionado horário 09:40 (término do agendamento 09:00)
✅ Gerados 20 horários possíveis (18 da grade fixa + 2 de términos de agendamentos)
```

### Log Esperado - Slot Disponível
```
✅ Slot 09:40 OK (termina às 10:20)
```

### Log Esperado - Conflito
```
❌ Slot 09:20 removido (conflitaria com agendamento existente)
```

## 🐛 Problemas Comuns e Soluções

### Problema 1: Horário não aparece
**Causa:** Horário de término está após o expediente
**Solução:** Verificar configuração do horário de trabalho

### Problema 2: Horário duplicado
**Causa:** Bug na lógica de verificação
**Solução:** Revisar método `generateAllTimeSlotsWithAppointmentEndTimes()`

### Problema 3: Permite conflito
**Causa:** Validação não está sendo aplicada
**Solução:** Verificar método `wouldConflictWithAppointments()`

## 📝 Queries SQL Úteis para Validação

### Verificar agendamentos do dia
```sql
SELECT 
    start_time, 
    end_time,
    TIMESTAMPDIFF(MINUTE, start_time, end_time) as duration
FROM appointments
WHERE date = '2026-02-15'
ORDER BY start_time;
```

### Verificar configuração do tenant
```sql
SELECT 
    start_time,
    end_time,
    slot_interval_minutes,
    horario_flexivel
FROM tenant_working_hours
WHERE tenant_id = '{tenantId}';
```

### Verificar bloqueios ativos
```sql
SELECT 
    start_time,
    end_time,
    recorrente
FROM blocked_time_slots
WHERE date = '2026-02-15' OR recorrente = true;
```

## ✅ Critérios de Sucesso

A funcionalidade está correta quando:

1. ✅ Horários de término de agendamentos aparecem como opções
2. ✅ Não há duplicação de horários
3. ✅ Validações de conflito funcionam corretamente
4. ✅ Respeita bloqueios e horário de trabalho
5. ✅ Filtra horários passados corretamente
6. ✅ Funciona em modo rígido e flexível
7. ✅ Performance aceitável (< 500ms para consulta)
8. ✅ Logs informativos aparecem

## 🎯 Teste de Aceitação Final

**Cenário Real:**
1. Cliente agenda serviço de 40 min às 09:00
2. Sistema deve permitir próximo cliente às 09:40
3. Não deve ter intervalo "morto" entre 09:40 e 10:00
4. Agenda deve estar totalmente otimizada

**Se todos os critérios forem atendidos, a funcionalidade está pronta para produção! 🚀**

