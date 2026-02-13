# 🚀 GUIA RÁPIDO - Testar Correção do Tempo de Lembrete

## ✅ CORREÇÃO APLICADA

O código foi atualizado para **sempre buscar o valor mais recente** do banco de dados. 

---

## 📝 PASSOS PARA TESTAR

### 1️⃣ Verificar Valor Atual no Banco

```sql
-- Execute no seu banco de dados PostgreSQL
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    updated_at
FROM tb_tenants
WHERE tenant_key = 'kc';  -- ← Substitua pelo seu tenant
```

**Resultado esperado:**
```
tenant_key | business_name | tempo_lembrete_minutos | updated_at
-----------+---------------+------------------------+--------------------
kc         | KC Salão      | 120                    | 2026-02-13 10:00:00
```

---

### 2️⃣ Reiniciar a Aplicação

```powershell
# No terminal onde a aplicação está rodando:
# 1. Parar: Pressione Ctrl+C

# 2. Iniciar novamente:
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw spring-boot:run
```

**OU** se estiver usando JAR compilado:

```powershell
# Parar todos os processos Java
Get-Process java | Stop-Process -Force

# Iniciar novamente
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
java -jar target/agendamento-back-0.0.1-SNAPSHOT.jar
```

---

### 3️⃣ Atualizar o Tempo de Lembrete via API

**Opção A: PowerShell**

```powershell
# Substitua os valores:
$tenantId = "COLE-UUID-DO-TENANT-AQUI"  # Ex: "123e4567-e89b-12d3-a456-426614174000"
$novoTempo = 45  # Novo tempo em minutos (45 = 45 minutos antes)

$body = @{
    businessName = "KC Salão"
    contactEmail = "contato@kc.com"
    contactPhone = "11999999999"
    tempoLembreteMinutos = $novoTempo
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/tenants/$tenantId" `
  -Method PUT `
  -Body $body `
  -ContentType "application/json"

Write-Host "✅ Atualização realizada!" -ForegroundColor Green
Write-Host "Novo tempo: $($response.tempoLembreteMinutos) minutos" -ForegroundColor Cyan
```

**Opção B: Postman/Insomnia**

```http
PUT http://localhost:8080/tenants/{tenant-id}
Content-Type: application/json

{
  "businessName": "KC Salão",
  "contactEmail": "contato@kc.com",
  "contactPhone": "11999999999",
  "tempoLembreteMinutos": 45
}
```

**Resposta esperada (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "tenantKey": "kc",
  "businessName": "KC Salão",
  "contactEmail": "contato@kc.com",
  "contactPhone": "11999999999",
  "active": true,
  "timezone": "America/Sao_Paulo",
  "tempoLembreteMinutos": 45,  ← DEVE MOSTRAR O NOVO VALOR
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-02-13T14:35:27"  ← DATA/HORA DA ATUALIZAÇÃO
}
```

---

### 4️⃣ Confirmar Mudança no Banco

```sql
-- Execute novamente a query
SELECT 
    tenant_key,
    tempo_lembrete_minutos,
    updated_at,
    NOW() - updated_at AS segundos_atras
FROM tb_tenants
WHERE tenant_key = 'kc';
```

**Resultado esperado:**
```
tenant_key | tempo_lembrete_minutos | updated_at          | segundos_atras
-----------+------------------------+---------------------+----------------
kc         | 45                     | 2026-02-13 14:35:27 | 00:00:03
           ^^^                        ^^^^^^^^^^^^^^^^^^^
           NOVO VALOR                 ACABOU DE ATUALIZAR
```

---

### 5️⃣ Monitorar Logs do Scheduler (1 minuto)

Aguarde até **1 minuto** após atualizar e verifique os logs da aplicação:

**ANTES (com 120 minutos):**
```log
🔔 Iniciando verificação de lembretes...
👥 Tenants ativos: [kc, ...]
📋 Tenant 'kc': buscando agendamentos entre 13/02/2026 12:00 e 13/02/2026 14:00 (120 minutos de antecedência)
                                                                                  ^^^^^^^^^^^^^^^^^^^^^^^^
```

**DEPOIS (com 45 minutos):**
```log
🔔 Iniciando verificação de lembretes...
👥 Tenants ativos: [kc, ...]
📋 Tenant 'kc': buscando agendamentos entre 13/02/2026 12:00 e 13/02/2026 12:45 (45 minutos de antecedência)
                                                                                  ^^^^^^^^^^^^^^^^^^^^^^^
                                                                                  ✅ VALOR ATUALIZADO!
```

---

## ✅ VALIDAÇÃO COMPLETA

### Checklist de Sucesso

- [ ] Aplicação reiniciada após mudança no código
- [ ] Atualização via API retornou 200 OK
- [ ] Valor mudou no banco de dados (SELECT confirmado)
- [ ] `updated_at` mostra data/hora recente
- [ ] Aguardou pelo menos 1 minuto
- [ ] Logs mostram novo valor entre parênteses
- [ ] Sistema está enviando lembretes no novo horário

---

## 🎯 TESTE PRÁTICO COMPLETO

Vamos fazer um teste do início ao fim:

### Cenário: Mudar de 2 horas para 30 minutos

```powershell
# 1. Ver valor atual
Write-Host "📊 PASSO 1: Verificando valor atual..." -ForegroundColor Yellow

# 2. Atualizar para 30 minutos
$tenantId = "SEU-TENANT-UUID-AQUI"
$body = @{
    businessName = "KC Salão"
    tempoLembreteMinutos = 30
} | ConvertTo-Json

Write-Host "🔄 PASSO 2: Atualizando para 30 minutos..." -ForegroundColor Yellow
$response = Invoke-RestMethod -Uri "http://localhost:8080/tenants/$tenantId" `
  -Method PUT -Body $body -ContentType "application/json"

Write-Host "✅ ATUALIZADO! Novo valor: $($response.tempoLembreteMinutos) minutos" -ForegroundColor Green

# 3. Aguardar 65 segundos (margem de segurança)
Write-Host "⏳ PASSO 3: Aguardando 65 segundos para próxima execução do scheduler..." -ForegroundColor Yellow
Start-Sleep -Seconds 65

Write-Host "🔍 PASSO 4: Verifique os logs agora!" -ForegroundColor Cyan
Write-Host "Procure por: '30 minutos de antecedência'" -ForegroundColor Cyan
```

---

## 🐛 PROBLEMAS COMUNS

### ❌ Problema: Logs ainda mostram 120 minutos

**Causa:** Aplicação não foi reiniciada após mudança no código

**Solução:**
```powershell
Get-Process java | Stop-Process -Force
.\mvnw spring-boot:run
```

---

### ❌ Problema: Erro ao atualizar via API

**Causa:** ID do tenant incorreto ou formato inválido

**Solução:** Buscar ID correto:
```sql
SELECT id, tenant_key FROM tb_tenants WHERE tenant_key = 'kc';
```

Use o **UUID completo** retornado.

---

### ❌ Problema: Valor não muda no banco

**Causa:** Transação não foi commitada ou constraint violada

**Solução:** Verificar constraints:
```sql
-- Ver se valor está dentro do permitido (1-1440)
SELECT 
    CASE 
        WHEN 30 BETWEEN 1 AND 1440 THEN '✅ Valor válido'
        ELSE '❌ Valor inválido'
    END AS validacao;
```

---

## 📊 COMPARAÇÃO VISUAL

### Timeline com 120 minutos (ANTES):

```
Agendamento: 14:00
Lembrete:    12:00 (2 horas antes)

11:00   11:30   12:00   12:30   13:00   13:30   14:00
  │       │       ▼       │       │       │       │
  │       │    ENVIA      │       │       │       │
  │       │   LEMBRETE    │       │       │       │
```

### Timeline com 45 minutos (DEPOIS):

```
Agendamento: 14:00
Lembrete:    13:15 (45 minutos antes)

11:00   11:30   12:00   12:30   13:00   13:15   14:00
  │       │       │       │       │       ▼       │
  │       │       │       │       │    ENVIA      │
  │       │       │       │       │   LEMBRETE    │
```

---

## 🎉 RESULTADO ESPERADO

Após seguir todos os passos, você verá:

```log
🔔 Iniciando verificação de lembretes...
👥 Tenants ativos: [kc]
📋 Tenant 'kc': buscando agendamentos entre 13/02/2026 14:00 e 13/02/2026 14:45 (45 minutos de antecedência)
📋 Tenant 'kc': 0 agendamento(s) para lembrar
🎯 Total de lembretes enviados: 0
```

**Isso confirma que:**
- ✅ Sistema está usando o **novo valor** (45 minutos)
- ✅ Correção funcionou perfeitamente
- ✅ Próximos lembretes serão enviados com o tempo correto

---

## 📞 AINDA NÃO FUNCIONOU?

Se após seguir TODOS os passos ainda não funcionar:

1. **Envie a saída do banco:**
   ```sql
   SELECT * FROM tb_tenants WHERE tenant_key = 'kc';
   ```

2. **Envie os logs do scheduler:**
   ```
   [últimas 20 linhas que começam com 🔔 ou 📋]
   ```

3. **Confirme que fez:**
   - [ ] Reiniciou a aplicação
   - [ ] Aguardou 1 minuto
   - [ ] Verificou que o código tem `freshTenant = tenantService.getTenantByKey(...)`

---

**Data da correção:** 13/02/2026  
**Status:** ✅ CORREÇÃO APLICADA - PRONTA PARA TESTE

