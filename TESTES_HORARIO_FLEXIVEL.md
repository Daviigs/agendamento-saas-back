# 🧪 Guia de Testes - Feature Horário Flexível

## ✅ Checklist de Validação

### 1. Verificação da Migration

**Objetivo**: Confirmar que a coluna foi adicionada corretamente ao banco de dados.

**Passos**:
```sql
-- 1. Verificar se a coluna existe
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'tb_tenant_working_hours'
  AND column_name = 'horario_flexivel';

-- Resultado esperado:
-- column_name: horario_flexivel
-- data_type: boolean
-- is_nullable: NO
-- column_default: false

-- 2. Verificar dados existentes (devem ter horario_flexivel = false)
SELECT tenant_id, horario_flexivel
FROM tb_tenant_working_hours;

-- Resultado esperado: Todos com horario_flexivel = false
```

**Status**: [ ] Aprovado [ ] Reprovado

---

### 2. Teste de Endpoints REST

#### 2.1. POST /working-hours (Criar com modo flexível)

**Request**:
```bash
curl -X POST "http://localhost:8080/working-hours" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test_tenant_flex" \
  -d '{
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "slotIntervalMinutes": 30,
    "horarioFlexivel": true
  }'
```

**Validações**:
- [ ] Status Code: 200 OK
- [ ] Response contém `"horarioFlexivel": true`
- [ ] Registro criado no banco com `horario_flexivel = true`

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 2.2. POST /working-hours (Criar com modo rígido)

**Request**:
```bash
curl -X POST "http://localhost:8080/working-hours" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test_tenant_rigid" \
  -d '{
    "startTime": "08:00:00",
    "endTime": "17:00:00",
    "slotIntervalMinutes": 30,
    "horarioFlexivel": false
  }'
```

**Validações**:
- [ ] Status Code: 200 OK
- [ ] Response contém `"horarioFlexivel": false`
- [ ] Registro criado no banco com `horario_flexivel = false`

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 2.3. POST /working-hours (Omitir horarioFlexivel - deve usar padrão)

**Request**:
```bash
curl -X POST "http://localhost:8080/working-hours" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test_tenant_default" \
  -d '{
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "slotIntervalMinutes": 30
  }'
```

**Validações**:
- [ ] Status Code: 200 OK
- [ ] Response contém `"horarioFlexivel": false` (padrão)
- [ ] Registro no banco tem `horario_flexivel = false`

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 2.4. PATCH /working-hours/horario-flexivel (Atualizar para flexível)

**Request**:
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: test_tenant_default"
```

**Validações**:
- [ ] Status Code: 200 OK
- [ ] Response contém `"horarioFlexivel": true`
- [ ] Outros campos permanecem inalterados
- [ ] Registro no banco foi atualizado

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 2.5. GET /working-hours (Consultar configuração)

**Request**:
```bash
curl -X GET "http://localhost:8080/working-hours" \
  -H "X-Tenant-Id: test_tenant_flex"
```

**Validações**:
- [ ] Status Code: 200 OK
- [ ] Response contém todos os campos incluindo `horarioFlexivel`

**Status**: [ ] Aprovado [ ] Reprovado

---

### 3. Testes de Lógica de Negócio

#### Setup Comum para Testes 3.1 a 3.4

```bash
# Criar tenant de teste
curl -X POST "http://localhost:8080/working-hours" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test_logic" \
  -d '{
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "slotIntervalMinutes": 30,
    "horarioFlexivel": false
  }'

# Criar profissional e associar working hours
# (Assumindo que isso já existe via migrations ou setup)

# Criar bloqueio de almoço
curl -X POST "http://localhost:8080/blocked-time-slots" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test_logic" \
  -d '{
    "startTime": "12:00:00",
    "endTime": "13:00:00",
    "specificDate": "2026-02-20"
  }'

# Criar serviço de 90 minutos
# serviceId = "test-service-90min"
```

---

#### 3.1. Modo Rígido - Bloqueio impede horários

**Request**:
```bash
# Garantir modo rígido
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=false" \
  -H "X-Tenant-Id: test_logic"

# Consultar slots disponíveis
curl -X GET "http://localhost:8080/available-slots/professional/{professionalId}?date=2026-02-20&serviceIds=test-service-90min" \
  -H "X-Tenant-Id: test_logic"
```

**Validações**:
- [ ] Horário 11:00 NÃO aparece na lista (terminaria às 12:30)
- [ ] Horário 11:30 NÃO aparece na lista (terminaria às 13:00)
- [ ] Horário 10:30 APARECE na lista (termina às 12:00 - exato)
- [ ] Horário 13:00 APARECE na lista (termina às 14:30)
- [ ] Horário 17:00 NÃO aparece na lista (terminaria às 18:30)
- [ ] Horário 16:30 APARECE na lista (termina às 18:00 - exato)

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 3.2. Modo Flexível - Bloqueio NÃO impede horários (exceto início)

**Request**:
```bash
# Ativar modo flexível
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: test_logic"

# Consultar slots disponíveis (mesmo serviço e data)
curl -X GET "http://localhost:8080/available-slots/professional/{professionalId}?date=2026-02-20&serviceIds=test-service-90min" \
  -H "X-Tenant-Id: test_logic"
```

**Validações**:
- [ ] Horário 11:00 APARECE na lista (pode atravessar bloqueio)
- [ ] Horário 11:30 APARECE na lista (pode atravessar bloqueio)
- [ ] Horário 12:00 NÃO aparece na lista (início está bloqueado)
- [ ] Horário 12:30 NÃO aparece na lista (início está bloqueado)
- [ ] Horário 13:00 APARECE na lista
- [ ] Horário 17:00 APARECE na lista (pode ultrapassar expediente)
- [ ] Horário 17:30 APARECE na lista (pode ultrapassar expediente)

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 3.3. Modo Flexível - Início bloqueado sempre impede

**Request**:
```bash
# Modo flexível ativo
# Tentar agendar no horário de almoço
curl -X POST "http://localhost:8080/appointments" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test_logic" \
  -d '{
    "professionalId": "{professionalId}",
    "date": "2026-02-20",
    "startTime": "12:00:00",
    "serviceIds": ["test-service-90min"]
  }'
```

**Validações**:
- [ ] Status Code: 400 Bad Request ou 409 Conflict
- [ ] Mensagem de erro indica que o horário está bloqueado
- [ ] Agendamento NÃO foi criado

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 3.4. Agendamentos existentes sempre impedem (ambos modos)

**Setup**:
```bash
# Criar agendamento às 10:00 (90 min - termina às 11:30)
curl -X POST "http://localhost:8080/appointments" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test_logic" \
  -d '{
    "professionalId": "{professionalId}",
    "date": "2026-02-20",
    "startTime": "10:00:00",
    "serviceIds": ["test-service-90min"]
  }'
```

**Teste Modo Rígido**:
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=false" \
  -H "X-Tenant-Id: test_logic"

curl -X GET "http://localhost:8080/available-slots/professional/{professionalId}?date=2026-02-20&serviceIds=test-service-90min" \
  -H "X-Tenant-Id: test_logic"
```

**Validações Modo Rígido**:
- [ ] Horário 10:00 NÃO aparece (já ocupado)
- [ ] Horários 09:00-09:30 podem aparecer (dependem de não conflitar com 10:00-11:30)

**Teste Modo Flexível**:
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: test_logic"

curl -X GET "http://localhost:8080/available-slots/professional/{professionalId}?date=2026-02-20&serviceIds=test-service-90min" \
  -H "X-Tenant-Id: test_logic"
```

**Validações Modo Flexível**:
- [ ] Horário 10:00 NÃO aparece (já ocupado)
- [ ] Conflitos de agendamento são respeitados igualmente

**Status**: [ ] Aprovado [ ] Reprovado

---

### 4. Testes de Logs

**Objetivo**: Verificar se os logs informativos estão sendo gerados corretamente.

**Passos**:
```bash
# 1. Consultar slots em modo rígido
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=false" \
  -H "X-Tenant-Id: test_logic"

curl -X GET "http://localhost:8080/available-slots/professional/{professionalId}?date=2026-02-20&serviceIds=test-service-90min" \
  -H "X-Tenant-Id: test_logic"

# 2. Verificar logs da aplicação
```

**Validações nos Logs**:
- [ ] Log contém: `Modo de horário: RÍGIDO (horarioFlexivel=false)`
- [ ] Log contém detalhes de bloqueios quando slots são removidos
- [ ] Log contém: `❌ BLOQUEADO: Slot X + Y min terminaria às Z`

**Passos (Modo Flexível)**:
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: test_logic"

curl -X GET "http://localhost:8080/available-slots/professional/{professionalId}?date=2026-02-20&serviceIds=test-service-90min" \
  -H "X-Tenant-Id: test_logic"
```

**Validações nos Logs**:
- [ ] Log contém: `Modo de horário: FLEXÍVEL (horarioFlexivel=true)`
- [ ] Log contém: `✅ Horário flexível ativo: Slot X permitido`

**Status**: [ ] Aprovado [ ] Reprovado

---

### 5. Testes de Edge Cases

#### 5.1. Tenant sem horário configurado

**Request**:
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: tenant_nao_existe"
```

**Validações**:
- [ ] Status Code: 404 Not Found
- [ ] Mensagem de erro apropriada

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 5.2. Valor null para horarioFlexivel

**Request**:
```bash
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel" \
  -H "X-Tenant-Id: test_logic"
```

**Validações**:
- [ ] Sistema trata null como false (padrão)
- [ ] Não causa erro

**Status**: [ ] Aprovado [ ] Reprovado

---

#### 5.3. Serviço sem duração especificada

**Request**:
```bash
curl -X GET "http://localhost:8080/available-slots/professional/{professionalId}?date=2026-02-20" \
  -H "X-Tenant-Id: test_logic"
```

**Validações**:
- [ ] Retorna todos os slots disponíveis (não considera duração)
- [ ] Flag horarioFlexivel não afeta resultado quando não há duração

**Status**: [ ] Aprovado [ ] Reprovado

---

### 6. Testes de Integração

#### 6.1. Fluxo Completo - Criar Tenant e Testar Ambos Modos

**Script de Teste**:
```bash
#!/bin/bash

TENANT="integration_test_$(date +%s)"
PROFESSIONAL_ID="test-prof-id"
SERVICE_ID="test-service-id"
DATE="2026-03-01"

echo "=== Teste de Integração - Horário Flexível ==="
echo "Tenant: $TENANT"

# 1. Criar working hours (modo rígido)
echo -e "\n1. Criando working hours..."
curl -X POST "http://localhost:8080/working-hours" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT" \
  -d '{
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "slotIntervalMinutes": 30,
    "horarioFlexivel": false
  }'

# 2. Criar bloqueio
echo -e "\n2. Criando bloqueio de almoço..."
curl -X POST "http://localhost:8080/blocked-time-slots" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT" \
  -d '{
    "startTime": "12:00:00",
    "endTime": "13:00:00",
    "specificDate": "'$DATE'"
  }'

# 3. Consultar slots (modo rígido)
echo -e "\n3. Consultando slots em modo RÍGIDO..."
curl -X GET "http://localhost:8080/available-slots/professional/$PROFESSIONAL_ID?date=$DATE&serviceIds=$SERVICE_ID" \
  -H "X-Tenant-Id: $TENANT"

# 4. Alternar para modo flexível
echo -e "\n4. Alternando para modo FLEXÍVEL..."
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" \
  -H "X-Tenant-Id: $TENANT"

# 5. Consultar slots (modo flexível)
echo -e "\n5. Consultando slots em modo FLEXÍVEL..."
curl -X GET "http://localhost:8080/available-slots/professional/$PROFESSIONAL_ID?date=$DATE&serviceIds=$SERVICE_ID" \
  -H "X-Tenant-Id: $TENANT"

# 6. Verificar diferença
echo -e "\n6. Comparar resultados - deve haver mais slots no modo flexível"

echo -e "\n=== Teste Concluído ==="
```

**Validações**:
- [ ] Script executa sem erros
- [ ] Modo rígido retorna menos slots que modo flexível
- [ ] Diferença está nos horários que atravessam bloqueios

**Status**: [ ] Aprovado [ ] Reprovado

---

## 📊 Resumo dos Testes

| Categoria | Total | Aprovado | Reprovado | Pendente |
|-----------|-------|----------|-----------|----------|
| Migration | 1 | | | ✓ |
| Endpoints REST | 5 | | | ✓ |
| Lógica de Negócio | 4 | | | ✓ |
| Logs | 1 | | | ✓ |
| Edge Cases | 3 | | | ✓ |
| Integração | 1 | | | ✓ |
| **TOTAL** | **15** | **0** | **0** | **15** |

---

## 🏆 Critérios de Aceitação

Para que a feature seja considerada completa e aprovada:

- [ ] Todos os testes de migration passaram
- [ ] Todos os 5 endpoints REST funcionam corretamente
- [ ] Lógica de negócio diferencia corretamente os dois modos
- [ ] Logs informativos estão sendo gerados
- [ ] Edge cases são tratados adequadamente
- [ ] Teste de integração completo passa

---

## 📝 Notas de Teste

_Adicione aqui observações relevantes encontradas durante os testes:_

```
Data: _________
Testador: _________

Observações:
- 
- 
- 
```

---

## 🐛 Bugs Encontrados

| ID | Descrição | Severidade | Status |
|----|-----------|------------|--------|
| 1  |           |            |        |
| 2  |           |            |        |

---

## ✅ Aprovação Final

- [ ] Todos os testes passaram
- [ ] Documentação está completa
- [ ] Código foi revisado
- [ ] Feature pronta para produção

**Aprovado por:** _________________  
**Data:** _________________

