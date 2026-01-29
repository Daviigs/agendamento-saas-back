# Script de Testes - Sistema de Vínculos Profissional ↔ Serviço

## 🧪 Testes da API

### Configuração
```bash
BASE_URL="http://localhost:8080"
TENANT_ID="kc"
```

---

## 1️⃣ CRIAR SERVIÇOS

### Serviço 1: Design de Sobrancelhas
```bash
curl -X POST "${BASE_URL}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Design de Sobrancelhas",
    "duration": 30,
    "price": 50.00
  }'
```

### Serviço 2: Aplicação de Cílios
```bash
curl -X POST "${BASE_URL}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Aplicação de Cílios",
    "duration": 90,
    "price": 150.00
  }'
```

### Serviço 3: Henna de Sobrancelhas
```bash
curl -X POST "${BASE_URL}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Henna de Sobrancelhas",
    "duration": 45,
    "price": 60.00
  }'
```

### 📋 Listar Serviços Criados
```bash
curl -X GET "${BASE_URL}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**💡 ANOTE os IDs dos serviços retornados!**

---

## 2️⃣ CRIAR PROFISSIONAIS

### Profissional 1: Ana (Especialista em Sobrancelhas)
```bash
curl -X POST "${BASE_URL}/professionals" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "professionalName": "Ana Silva",
    "professionalEmail": "ana@salao.com",
    "professionalPhone": "11999991111"
  }'
```

### Profissional 2: Beatriz (Especialista em Cílios)
```bash
curl -X POST "${BASE_URL}/professionals" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "professionalName": "Beatriz Costa",
    "professionalEmail": "beatriz@salao.com",
    "professionalPhone": "11999992222"
  }'
```

### Profissional 3: Carla (Profissional Completa)
```bash
curl -X POST "${BASE_URL}/professionals" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "professionalName": "Carla Mendes",
    "professionalEmail": "carla@salao.com",
    "professionalPhone": "11999993333"
  }'
```

### 📋 Listar Profissionais Criados
```bash
curl -X GET "${BASE_URL}/professionals" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**💡 ANOTE os IDs dos profissionais retornados!**

---

## 3️⃣ VINCULAR SERVIÇOS AOS PROFISSIONAIS

**⚠️ SUBSTITUA os UUIDs pelos valores reais!**

### Ana → Design + Henna
```bash
curl -X PUT "${BASE_URL}/professionals/{ANA_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceIds": [
      "{DESIGN_SOBRANCELHAS_ID}",
      "{HENNA_SOBRANCELHAS_ID}"
    ]
  }'
```

### Beatriz → Aplicação de Cílios
```bash
curl -X PUT "${BASE_URL}/professionals/{BEATRIZ_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceIds": [
      "{APLICACAO_CILIOS_ID}"
    ]
  }'
```

### Carla → Todos os Serviços
```bash
curl -X PUT "${BASE_URL}/professionals/{CARLA_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceIds": [
      "{DESIGN_SOBRANCELHAS_ID}",
      "{APLICACAO_CILIOS_ID}",
      "{HENNA_SOBRANCELHAS_ID}"
    ]
  }'
```

---

## 4️⃣ VERIFICAR VÍNCULOS

### Listar Serviços de Ana
```bash
curl -X GET "${BASE_URL}/professionals/{ANA_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**Esperado:**
```json
{
  "professionalId": "...",
  "professionalName": "Ana Silva",
  "services": [
    { "name": "Design de Sobrancelhas", ... },
    { "name": "Henna de Sobrancelhas", ... }
  ]
}
```

### Listar Serviços de Beatriz
```bash
curl -X GET "${BASE_URL}/professionals/{BEATRIZ_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

### Listar Serviços de Carla
```bash
curl -X GET "${BASE_URL}/professionals/{CARLA_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

---

## 5️⃣ TESTAR FILTRO DE PROFISSIONAIS

### Teste 1: Buscar profissionais para "Design de Sobrancelhas"
```bash
curl -X GET "${BASE_URL}/professionals/active?serviceIds={DESIGN_SOBRANCELHAS_ID}" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**✅ Esperado:** Ana e Carla

---

### Teste 2: Buscar profissionais para "Aplicação de Cílios"
```bash
curl -X GET "${BASE_URL}/professionals/active?serviceIds={APLICACAO_CILIOS_ID}" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**✅ Esperado:** Beatriz e Carla

---

### Teste 3: Buscar profissionais para "Design + Aplicação"
```bash
curl -X GET "${BASE_URL}/professionals/active?serviceIds={DESIGN_SOBRANCELHAS_ID},{APLICACAO_CILIOS_ID}" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**✅ Esperado:** Apenas Carla (única que faz ambos)

---

### Teste 4: Buscar profissionais sem filtro
```bash
curl -X GET "${BASE_URL}/professionals/active" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**✅ Esperado:** Ana, Beatriz e Carla (todos)

---

## 6️⃣ TESTAR CRIAÇÃO DE AGENDAMENTO

### ✅ SUCESSO: Ana fazendo "Design de Sobrancelhas"
```bash
curl -X POST "${BASE_URL}/appointments" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "professionalId": "{ANA_ID}",
    "serviceIds": ["{DESIGN_SOBRANCELHAS_ID}"],
    "date": "2026-02-15",
    "startTime": "14:00",
    "userName": "Cliente Teste 1",
    "userPhone": "11888881111"
  }'
```

**✅ Esperado:** Agendamento criado (201 Created)

---

### ❌ ERRO: Ana tentando fazer "Aplicação de Cílios"
```bash
curl -X POST "${BASE_URL}/appointments" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "professionalId": "{ANA_ID}",
    "serviceIds": ["{APLICACAO_CILIOS_ID}"],
    "date": "2026-02-15",
    "startTime": "15:00",
    "userName": "Cliente Teste 2",
    "userPhone": "11888882222"
  }'
```

**❌ Esperado:** Erro 400 Bad Request
```json
{
  "message": "O profissional selecionado não está habilitado para executar todos os serviços deste agendamento..."
}
```

---

### ✅ SUCESSO: Carla fazendo múltiplos serviços
```bash
curl -X POST "${BASE_URL}/appointments" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "professionalId": "{CARLA_ID}",
    "serviceIds": [
      "{DESIGN_SOBRANCELHAS_ID}",
      "{APLICACAO_CILIOS_ID}"
    ],
    "date": "2026-02-15",
    "startTime": "16:00",
    "userName": "Cliente Teste 3",
    "userPhone": "11888883333"
  }'
```

**✅ Esperado:** Agendamento criado (201 Created)

---

## 7️⃣ TESTAR REMOÇÃO DE VÍNCULO

### Desvincular "Henna" de Ana
```bash
curl -X DELETE "${BASE_URL}/professionals/{ANA_ID}/services/{HENNA_SOBRANCELHAS_ID}" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**✅ Esperado:** 204 No Content

### Verificar serviços de Ana após remoção
```bash
curl -X GET "${BASE_URL}/professionals/{ANA_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**✅ Esperado:** Apenas "Design de Sobrancelhas"

---

## 8️⃣ VERIFICAR HORÁRIOS DISPONÍVEIS

### Horários de Ana para 15/02/2026
```bash
curl -X GET "${BASE_URL}/appointments/available-slots?professionalId={ANA_ID}&date=2026-02-15" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

**✅ Esperado:** Lista de horários disponíveis (exceto 14:00 já agendado)

---

## 📊 RESUMO DOS TESTES

| Teste | Endpoint | Resultado Esperado |
|-------|----------|-------------------|
| Vincular serviços | `PUT /professionals/{id}/services` | ✅ 200 OK |
| Listar vínculos | `GET /professionals/{id}/services` | ✅ 200 OK + JSON |
| Filtrar profissionais | `GET /professionals/active?serviceIds=...` | ✅ 200 OK + filtrados |
| Agendamento válido | `POST /appointments` | ✅ 201 Created |
| Agendamento inválido | `POST /appointments` | ❌ 400 Bad Request |
| Remover vínculo | `DELETE /professionals/{id}/services/{sid}` | ✅ 204 No Content |

---

## 🐛 Troubleshooting

### Erro: "Tenant não encontrado"
- Verifique o header `X-Tenant-Id`
- Confirme que o tenant existe e está ativo

### Erro: "Profissional não encontrado"
- Verifique se o UUID está correto
- Confirme que o profissional pertence ao tenant

### Erro: "Serviço não pertence ao tenant"
- Os serviços devem ter sido criados com o mesmo tenant

### Erro 500
- Verifique se a migration V3 foi executada
- Confirme que o banco de dados está acessível
- Veja os logs da aplicação

---

## 📝 Exemplo Completo (Copiar/Colar)

```bash
# Configuração
BASE_URL="http://localhost:8080"
TENANT_ID="kc"

# 1. Criar serviço
DESIGN_RESPONSE=$(curl -s -X POST "${BASE_URL}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Design de Sobrancelhas","duration":30,"price":50.00}')

DESIGN_ID=$(echo $DESIGN_RESPONSE | jq -r '.id')
echo "Serviço criado: $DESIGN_ID"

# 2. Criar profissional
ANA_RESPONSE=$(curl -s -X POST "${BASE_URL}/professionals" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{"professionalName":"Ana Silva","professionalEmail":"ana@salao.com","professionalPhone":"11999991111"}')

ANA_ID=$(echo $ANA_RESPONSE | jq -r '.id')
echo "Profissional criado: $ANA_ID"

# 3. Vincular serviço
curl -X PUT "${BASE_URL}/professionals/${ANA_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"serviceIds\":[\"${DESIGN_ID}\"]}"

echo "Vínculo criado!"

# 4. Verificar vínculo
curl -X GET "${BASE_URL}/professionals/${ANA_ID}/services" \
  -H "X-Tenant-Id: ${TENANT_ID}"
```

---

## ✅ Checklist de Validação

- [ ] Serviços criados com sucesso
- [ ] Profissionais criados com sucesso
- [ ] Vínculos criados e retornados corretamente
- [ ] Filtro de profissionais funciona
- [ ] Agendamento válido criado
- [ ] Agendamento inválido bloqueado com mensagem clara
- [ ] Remoção de vínculo funciona
- [ ] Sistema retrocompatível (agendamentos antigos funcionam)

