# 🚀 QUICK START: Prevenção de Agendamentos no Passado

## ⚡ TL;DR

**O que mudou:**
- ✅ Horários que já passaram NÃO aparecem mais na lista de disponíveis
- ✅ Não é mais possível criar agendamentos retroativos
- ✅ Sistema considera o timezone de cada tenant

## 📦 O que foi adicionado?

### 1 Novo Campo no Banco
```sql
tb_tenants.timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo'
```

### 1 Novo Serviço
```java
TenantDateTimeService - Gerencia data/hora por timezone
```

### 2 Validações Novas
1. **Filtro de horários:** Remove slots passados da listagem
2. **Bloqueio de criação:** Rejeita agendamentos no passado

---

## 🔧 Instalação

### Passo 1: Atualizar o Código
```bash
git pull origin main
```

### Passo 2: Rodar Migration
```bash
./mvnw flyway:migrate
```
ou simplesmente inicie a aplicação (Flyway roda automaticamente).

### Passo 3: Verificar
```sql
SELECT tenant_key, timezone FROM tb_tenants;
```

Deve retornar algo como:
```
tenant_key | timezone
-----------+-----------------
kc         | America/Sao_Paulo
mjs        | America/Sao_Paulo
```

---

## 🧪 Teste Rápido

### 1. Consultar Horários Disponíveis

**Hoje às 14:00, consultar horários para hoje:**

```bash
curl -X GET "http://localhost:8080/appointments/available-slots?professionalId=<UUID>&date=2026-02-11&serviceIds=<UUID>" \
  -H "X-Tenant-Id: kc"
```

**Esperado:**
- ❌ NÃO deve retornar horários antes das 14:00
- ✅ Deve retornar apenas horários >= 14:00

### 2. Tentar Criar Agendamento no Passado

```bash
curl -X POST "http://localhost:8080/appointments" \
  -H "X-Tenant-Id: kc" \
  -H "Content-Type: application/json" \
  -d '{
    "professionalId": "<UUID>",
    "serviceIds": ["<UUID>"],
    "date": "2026-02-10",
    "startTime": "10:00",
    "userName": "Test User",
    "userPhone": "+5511999999999"
  }'
```

**Esperado:**
```json
{
  "status": 400,
  "message": "Não é possível agendar para um horário que já passou..."
}
```

---

## 🎯 Casos de Uso

### Caso 1: Frontend Consultando Horários

**Antes:**
```
Hoje: 11/02 às 14:00
GET /available-slots?date=2026-02-11

Retornava:
[09:00, 09:30, 10:00, ..., 14:00, 14:30, 15:00] ❌
```

**Agora:**
```
Hoje: 11/02 às 14:00
GET /available-slots?date=2026-02-11

Retorna:
[14:30, 15:00, 15:30, ...] ✅
```

### Caso 2: Usuário Tentando Agendar no Passado

**Antes:**
```
POST /appointments
{ date: "2026-02-10", ... }

→ Agendamento criado ❌ (BUG!)
```

**Agora:**
```
POST /appointments
{ date: "2026-02-10", ... }

→ 400 Bad Request ✅
   "Não é possível agendar para um horário que já passou"
```

---

## 🌍 Configurar Timezone (Opcional)

Se um tenant estiver em outro timezone:

```sql
UPDATE tb_tenants 
SET timezone = 'America/New_York' 
WHERE tenant_key = 'ny-salon';
```

Timezones válidos: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

Exemplos:
- `America/Sao_Paulo` (UTC-3)
- `America/New_York` (UTC-5)
- `Europe/London` (UTC+0)
- `Asia/Tokyo` (UTC+9)

---

## 🐛 Troubleshooting

### Problema: Horários ainda aparecem no passado

**Causa:** Migration não rodou

**Solução:**
```bash
./mvnw flyway:migrate
```

### Problema: Erro "Cannot resolve method"

**Causa:** IDE não atualizou índices

**Solução:**
- IntelliJ: File → Invalidate Caches → Restart
- Eclipse: Project → Clean

### Problema: Timezone não funciona

**Solução:**
```sql
-- Verificar valor
SELECT timezone FROM tb_tenants WHERE tenant_key = 'kc';

-- Se NULL ou inválido, corrigir
UPDATE tb_tenants SET timezone = 'America/Sao_Paulo' WHERE tenant_key = 'kc';
```

---

## 📚 Documentação Completa

- **Especificação:** `FEATURE_PREVENT_PAST_APPOINTMENTS.md`
- **Testes:** `TESTES_PREVENT_PAST_APPOINTMENTS.md`
- **Resumo:** `RESUMO_IMPLEMENTACAO_PREVENT_PAST.md`

---

## ✅ Checklist Pós-Deploy

- [ ] Migration executada com sucesso
- [ ] Coluna `timezone` existe em `tb_tenants`
- [ ] Teste: Consultar horários para hoje (filtra passados)
- [ ] Teste: Tentar criar agendamento no passado (rejeitado)
- [ ] Teste: Criar agendamento no futuro (funciona)
- [ ] Logs mostram filtragem de slots passados
- [ ] Frontend exibe apenas horários válidos

---

## 🎉 Pronto!

A feature está funcionando. O sistema agora é **impossível** de criar agendamentos retroativos.

**Dúvidas?** Consulte a documentação completa ou os testes unitários.

