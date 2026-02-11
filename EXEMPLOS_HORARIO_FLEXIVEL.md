# 📚 Exemplos de Uso da API - Horário Flexível

## 🎯 Cenários de Uso

### Cenário 1: Salão de Beleza (Agenda Flexível)

**Contexto:**
- Salão permite que atendimentos ultrapassem o horário de almoço
- Última cliente pode começar às 17:30 mesmo que termine após as 18:00
- Flexibilidade é importante para não perder clientes

**Configuração:**

```bash
# 1. Configurar horário de trabalho com modo flexível
curl -X POST "http://localhost:8080/working-hours" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: salon_kc" \
  -d '{
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "slotIntervalMinutes": 30,
    "horarioFlexivel": true
  }'
```

**Resultado:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "salon_kc",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotIntervalMinutes": 30,
  "horarioFlexivel": true,
  "active": true
}
```

**Criar Bloqueio de Almoço:**
```bash
curl -X POST "http://localhost:8080/blocked-time-slots" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: salon_kc" \
  -d '{
    "startTime": "12:00:00",
    "endTime": "13:00:00",
    "recurring": true,
    "dayOfWeek": "MONDAY"
  }'
```

**Consultar Horários Disponíveis (Serviço de 90 minutos):**
```bash
curl -X GET "http://localhost:8080/available-slots/professional/550e8400-e29b-41d4-a716-446655440000?date=2026-02-16&serviceIds=abc123" \
  -H "X-Tenant-Id: salon_kc"
```

**Resposta:**
```json
[
  "09:00", "09:30", "10:00", "10:30",
  "11:00", "11:30",  ✅ Permite atravessar o almoço
  "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
  "17:00", "17:30"   ✅ Permite ultrapassar o expediente
]
```

---

### Cenário 2: Clínica Médica (Agenda Rígida)

**Contexto:**
- Clínica não pode agendar consultas que ultrapassem o expediente
- Horário de almoço é sagrado e não pode ser invadido
- Precisão e pontualidade são essenciais

**Configuração:**

```bash
# 1. Configurar horário de trabalho com modo rígido
curl -X POST "http://localhost:8080/working-hours" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: clinic_medica" \
  -d '{
    "startTime": "08:00:00",
    "endTime": "17:00:00",
    "slotIntervalMinutes": 30,
    "horarioFlexivel": false
  }'
```

**Criar Bloqueio de Almoço:**
```bash
curl -X POST "http://localhost:8080/blocked-time-slots" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: clinic_medica" \
  -d '{
    "startTime": "12:00:00",
    "endTime": "13:00:00",
    "recurring": true,
    "dayOfWeek": "MONDAY"
  }'
```

**Consultar Horários Disponíveis (Consulta de 60 minutos):**
```bash
curl -X GET "http://localhost:8080/available-slots/professional/660e8400-e29b-41d4-a716-446655440000?date=2026-02-16&serviceIds=def456" \
  -H "X-Tenant-Id: clinic_medica"
```

**Resposta:**
```json
[
  "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00",
  ❌ "11:30" bloqueado: terminaria às 12:30 (invade almoço)
  "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
  "16:00"
  ❌ "16:30" bloqueado: terminaria às 17:30 (ultrapassa expediente)
]
```

---

### Cenário 3: Alternar Entre Modos

**Mudar de Rígido para Flexível:**
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: salon_kc"
```

**Mudar de Flexível para Rígido:**
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=false" \
  -H "X-Tenant-Id: salon_kc"
```

**Verificar Configuração Atual:**
```bash
curl -X GET "http://localhost:8080/working-hours" \
  -H "X-Tenant-Id: salon_kc"
```

---

## 🧪 Testes Comparativos

### Setup Comum
```bash
# Horário de funcionamento
POST /working-hours
{
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "slotIntervalMinutes": 30
}

# Bloqueio de almoço
POST /blocked-time-slots
{
  "startTime": "12:00:00",
  "endTime": "13:00:00",
  "recurring": true,
  "dayOfWeek": "MONDAY"
}

# Serviço de 90 minutos
serviceId = "123e4567"
```

### Teste 1: Horário 11:00 (Serviço de 90min termina às 12:30)

**Modo Rígido:**
```bash
PATCH /working-hours/horario-flexivel?flexivel=false
GET /available-slots?...

Resultado: 11:00 NÃO APARECE ❌
Motivo: Termina às 12:30, invade o bloqueio de almoço
```

**Modo Flexível:**
```bash
PATCH /working-hours/horario-flexivel?flexivel=true
GET /available-slots?...

Resultado: 11:00 APARECE ✅
Motivo: Pode atravessar o bloqueio de almoço
```

### Teste 2: Horário 17:00 (Serviço de 90min termina às 18:30)

**Modo Rígido:**
```bash
PATCH /working-hours/horario-flexivel?flexivel=false
GET /available-slots?...

Resultado: 17:00 NÃO APARECE ❌
Motivo: Termina às 18:30, ultrapassa o expediente (18:00)
```

**Modo Flexível:**
```bash
PATCH /working-hours/horario-flexivel?flexivel=true
GET /available-slots?...

Resultado: 17:00 APARECE ✅
Motivo: Pode ultrapassar o expediente
```

### Teste 3: Horário 12:00 (Dentro do bloqueio)

**Modo Rígido:**
```bash
Resultado: 12:00 NÃO APARECE ❌
Motivo: Horário de início está bloqueado
```

**Modo Flexível:**
```bash
Resultado: 12:00 NÃO APARECE ❌
Motivo: Mesmo no modo flexível, o horário de INÍCIO não pode estar bloqueado
```

---

## 📊 Tabela de Comparação

| Horário de Início | Término (90min) | Modo Rígido | Modo Flexível | Observação |
|-------------------|----------------|-------------|---------------|------------|
| 09:00 | 10:30 | ✅ | ✅ | Não conflita |
| 10:30 | 12:00 | ✅ | ✅ | Termina exatamente no bloqueio |
| 11:00 | 12:30 | ❌ | ✅ | Invade bloqueio de almoço |
| 11:30 | 13:00 | ❌ | ✅ | Atravessa todo o bloqueio |
| 12:00 | 13:30 | ❌ | ❌ | Início está bloqueado |
| 12:30 | 14:00 | ❌ | ❌ | Início está bloqueado |
| 13:00 | 14:30 | ✅ | ✅ | Não conflita |
| 16:30 | 18:00 | ✅ | ✅ | Termina exatamente no expediente |
| 17:00 | 18:30 | ❌ | ✅ | Ultrapassa expediente |
| 17:30 | 19:00 | ❌ | ✅ | Ultrapassa expediente |

---

## 🔄 Scripts de Migração para Tenants Existentes

### Script 1: Ativar modo flexível para todos os salões

```bash
# Lista de tenants de salões
tenants=("salon_kc" "salon_mjs" "salon_beauty")

for tenant in "${tenants[@]}"
do
  echo "Ativando modo flexível para: $tenant"
  curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
    -H "X-Tenant-Id: $tenant"
done
```

### Script 2: Manter modo rígido para clínicas

```bash
# Lista de tenants de clínicas
tenants=("clinic_med" "clinic_dent" "clinic_fisio")

for tenant in "${tenants[@]}"
do
  echo "Mantendo modo rígido para: $tenant"
  curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=false" \
    -H "X-Tenant-Id: $tenant"
done
```

---

## 🎓 Boas Práticas

### ✅ Quando usar Modo Flexível (true)

1. **Salões de beleza** - Clientes podem ficar além do horário
2. **Prestadores autônomos** - Flexibilidade é importante para maximizar receita
3. **Serviços com duração variável** - Onde o tempo pode se estender naturalmente
4. **Profissionais com agenda própria** - Que controlam seus próprios horários

### ✅ Quando usar Modo Rígido (false)

1. **Clínicas médicas** - Pontualidade e horários precisos são essenciais
2. **Consultórios** - Agenda fechada e controlada
3. **Serviços com equipe** - Onde ultrapassar o expediente afeta múltiplas pessoas
4. **Estabelecimentos com horário fixo** - Shoppings, centros comerciais

---

## ⚠️ Observações Importantes

1. **Bloqueios de dia inteiro**: Sempre impedem agendamentos, independente do modo
2. **Conflitos de agendamento**: Sempre validados, independente do modo
3. **Horário de início bloqueado**: Nunca permitido, mesmo no modo flexível
4. **Padrão seguro**: Novos tenants iniciam no modo rígido (false)

---

## 📞 Suporte

Para dúvidas ou problemas, consulte:
- Documentação principal: `FEATURE_HORARIO_FLEXIVEL.md`
- Logs do sistema: Procure por "Modo de horário" nos logs da aplicação

