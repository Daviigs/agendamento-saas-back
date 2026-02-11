# 🧪 Guia de Testes - Exceções de Bloqueios Recorrentes

## 🎯 Objetivo

Validar que o sistema permite liberar datas específicas de bloqueios recorrentes, respeitando a ordem de prioridade correta.

---

## 📋 Cenários de Teste

### ✅ Cenário 1: Exceção Libera Domingo Bloqueado

**Setup:**
1. Bloquear todos os domingos (recorrente)
2. Criar exceção para domingo 15/02/2026

**Resultado Esperado:**
- ✅ 15/02/2026 (domingo) - LIBERADO
- ❌ 22/02/2026 (domingo) - BLOQUEADO
- ❌ 08/02/2026 (domingo) - BLOQUEADO

**Comandos:**
```bash
# 1. Bloquear todos os domingos
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "dayOfWeek": "SUNDAY",
    "reason": "Folga semanal"
  }'

# 2. Criar exceção para domingo específico
curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "exceptionDate": "2026-02-15",
    "reason": "Trabalho extra"
  }'

# 3. Verificar datas disponíveis em fevereiro
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-01&endDate=2026-02-28" \
  -H "X-Tenant-Id: kc"

# Deve incluir 15/02 mas não outros domingos
```

---

### ✅ Cenário 2: Bloqueio Específico Tem Prioridade sobre Exceção

**Setup:**
1. Criar exceção para 15/02/2026
2. Criar bloqueio específico para 15/02/2026

**Resultado Esperado:**
- ❌ 15/02/2026 - BLOQUEADO (bloqueio específico vence)

**Comandos:**
```bash
# 1. Criar exceção primeiro
curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "exceptionDate": "2026-02-15",
    "reason": "Trabalho extra"
  }'

# 2. Depois criar bloqueio específico para mesma data
curl -X POST http://localhost:8080/blocked-days/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-02-15",
    "reason": "Emergência - Salão fechado"
  }'

# 3. Verificar que a data está bloqueada
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-15&endDate=2026-02-15" \
  -H "X-Tenant-Id: kc"

# Não deve incluir 15/02
```

---

### ❌ Cenário 3: Não Permite Exceção em Data com Bloqueio Específico

**Setup:**
1. Criar bloqueio específico para 25/12/2026 (Natal)
2. Tentar criar exceção para 25/12/2026

**Resultado Esperado:**
- ❌ Erro: "Esta data possui um bloqueio específico..."

**Comandos:**
```bash
# 1. Criar bloqueio específico
curl -X POST http://localhost:8080/blocked-days/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-12-25",
    "reason": "Natal"
  }'

# 2. Tentar criar exceção (deve falhar)
curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "exceptionDate": "2026-12-25",
    "reason": "Quero trabalhar no Natal"
  }'

# Deve retornar erro 400/409
```

---

### ✅ Cenário 4: Múltiplas Exceções em Dias Diferentes

**Setup:**
1. Bloquear todos os sábados e domingos
2. Criar exceções para 3 domingos específicos

**Resultado Esperado:**
- ✅ Domingos com exceção - LIBERADOS
- ❌ Outros sábados e domingos - BLOQUEADOS

**Comandos:**
```bash
# 1. Bloquear sábados e domingos
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"dayOfWeek": "SATURDAY", "reason": "Folga"}'

curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"dayOfWeek": "SUNDAY", "reason": "Folga"}'

# 2. Criar exceções para 3 domingos
curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"exceptionDate": "2026-02-15", "reason": "Evento especial 1"}'

curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"exceptionDate": "2026-03-08", "reason": "Evento especial 2"}'

curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"exceptionDate": "2026-04-12", "reason": "Evento especial 3"}'

# 3. Listar todas as exceções
curl -X GET http://localhost:8080/blocked-days/exceptions \
  -H "X-Tenant-Id: kc"
```

---

### ✅ Cenário 5: Remover Exceção Volta a Bloquear

**Setup:**
1. Criar exceção para domingo 15/02/2026
2. Verificar que está liberado
3. Remover exceção
4. Verificar que voltou a ser bloqueado

**Comandos:**
```bash
# 1. Bloquear domingos
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"dayOfWeek": "SUNDAY", "reason": "Folga"}'

# 2. Criar exceção
EXCEPTION_RESPONSE=$(curl -s -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"exceptionDate": "2026-02-15", "reason": "Trabalho extra"}')

EXCEPTION_ID=$(echo $EXCEPTION_RESPONSE | jq -r '.id')

# 3. Verificar que 15/02 está liberado
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-15&endDate=2026-02-15" \
  -H "X-Tenant-Id: kc"

# 4. Remover exceção
curl -X DELETE "http://localhost:8080/blocked-days/exceptions/$EXCEPTION_ID" \
  -H "X-Tenant-Id: kc"

# 5. Verificar que 15/02 voltou a ser bloqueado
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-15&endDate=2026-02-15" \
  -H "X-Tenant-Id: kc"
```

---

### ✅ Cenário 6: Agendamento em Data com Exceção

**Setup:**
1. Bloquear todos os domingos
2. Criar exceção para domingo 15/02/2026
3. Criar agendamento para domingo 15/02/2026

**Resultado Esperado:**
- ✅ Agendamento criado com sucesso

**Comandos:**
```bash
# 1. Bloquear domingos
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"dayOfWeek": "SUNDAY", "reason": "Folga"}'

# 2. Criar exceção
curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"exceptionDate": "2026-02-15", "reason": "Trabalho extra"}'

# 3. Criar agendamento (deve funcionar)
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "professionalId": "33333333-3333-3333-3333-333333333333",
    "serviceIds": ["<service-id>"],
    "date": "2026-02-15",
    "startTime": "10:00",
    "userName": "Cliente Teste",
    "userPhone": "11999999999"
  }'

# Deve retornar 201 Created
```

---

### ❌ Cenário 7: Agendamento em Domingo sem Exceção

**Setup:**
1. Bloquear todos os domingos
2. Tentar criar agendamento para domingo 22/02/2026 (sem exceção)

**Resultado Esperado:**
- ❌ Erro: "Não é possível agendar nesta data. O salão estará fechado."

**Comandos:**
```bash
# 1. Bloquear domingos
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"dayOfWeek": "SUNDAY", "reason": "Folga"}'

# 2. Tentar criar agendamento (deve falhar)
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "professionalId": "33333333-3333-3333-3333-333333333333",
    "serviceIds": ["<service-id>"],
    "date": "2026-02-22",
    "startTime": "10:00",
    "userName": "Cliente Teste",
    "userPhone": "11999999999"
  }'

# Deve retornar erro 400
```

---

## 📊 Matriz de Validação

| Condição | Bloqueio Específico | Exceção | Bloqueio Recorrente | Resultado |
|----------|---------------------|---------|---------------------|-----------|
| 1 | ✅ Sim | - | - | ❌ BLOQUEADO |
| 2 | ❌ Não | ✅ Sim | ✅ Sim | ✅ LIBERADO |
| 3 | ❌ Não | ❌ Não | ✅ Sim | ❌ BLOQUEADO |
| 4 | ❌ Não | ❌ Não | ❌ Não | ✅ LIBERADO |
| 5 | ✅ Sim | ✅ Sim | ✅ Sim | ❌ BLOQUEADO |

---

## 🚀 Comandos Úteis

### Limpar Dados de Teste
```bash
# Listar todas as exceções
curl -X GET http://localhost:8080/blocked-days/exceptions \
  -H "X-Tenant-Id: kc"

# Remover exceção específica
curl -X DELETE http://localhost:8080/blocked-days/exceptions/{exception-id} \
  -H "X-Tenant-Id: kc"

# Listar bloqueios recorrentes
curl -X GET http://localhost:8080/blocked-days/recurring \
  -H "X-Tenant-Id: kc"

# Remover bloqueio recorrente
curl -X DELETE http://localhost:8080/blocked-days/{blocked-day-id} \
  -H "X-Tenant-Id: kc"
```

### Verificar Estado Atual
```bash
# Verificar datas disponíveis em um mês
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-01&endDate=2026-02-28" \
  -H "X-Tenant-Id: kc"

# Listar todas as exceções futuras
curl -X GET http://localhost:8080/blocked-days/exceptions/future \
  -H "X-Tenant-Id: kc"

# Listar todos os bloqueios
curl -X GET http://localhost:8080/blocked-days \
  -H "X-Tenant-Id: kc"
```

---

## ✅ Checklist de Testes

- [ ] Cenário 1: Exceção libera domingo bloqueado
- [ ] Cenário 2: Bloqueio específico tem prioridade
- [ ] Cenário 3: Não permite exceção em bloqueio específico
- [ ] Cenário 4: Múltiplas exceções funcionam corretamente
- [ ] Cenário 5: Remover exceção volta a bloquear
- [ ] Cenário 6: Agendamento em data com exceção funciona
- [ ] Cenário 7: Agendamento em dia bloqueado falha
- [ ] Multi-tenancy: Exceções são isoladas por tenant
- [ ] Validações: Todos os campos obrigatórios são validados
- [ ] Performance: Consultas são rápidas com índices

---

## 🐛 Troubleshooting

### Problema: Exceção não está liberando a data

**Verificar:**
1. A data tem bloqueio específico? (Tem prioridade sobre exceção)
2. A exceção foi criada para o tenant correto?
3. A exceção está realmente no banco?

```bash
# Verificar exceção no banco
curl -X GET http://localhost:8080/blocked-days/exceptions \
  -H "X-Tenant-Id: kc"

# Verificar bloqueios da data
curl -X GET http://localhost:8080/blocked-days \
  -H "X-Tenant-Id: kc"
```

### Problema: Não consigo criar exceção

**Verificar:**
1. A data já tem bloqueio específico?
2. Já existe exceção para essa data?
3. O tenant está correto?

```bash
# Verificar bloqueios específicos
curl -X GET http://localhost:8080/blocked-days/specific \
  -H "X-Tenant-Id: kc"

# Verificar exceções existentes
curl -X GET http://localhost:8080/blocked-days/exceptions \
  -H "X-Tenant-Id: kc"
```

---

## 📝 Notas

1. **Header obrigatório**: Sempre enviar `X-Tenant-Id` em todas as requisições
2. **Formato de data**: Usar ISO-8601 (YYYY-MM-DD)
3. **IDs**: São UUIDs gerados automaticamente
4. **Isolamento**: Dados são isolados por tenant (multi-tenancy)

