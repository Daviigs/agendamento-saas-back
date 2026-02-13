# 🚨 SOLUÇÃO URGENTE - Valor Não Salva no Banco

## ⚡ AÇÃO IMEDIATA

Execute estes passos **AGORA** para resolver:

---

## 1️⃣ VERIFICAR SE A COLUNA EXISTE

```sql
-- Execute no banco de dados:
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'tb_tenants' 
  AND column_name = 'tempo_lembrete_minutos';
```

### ✅ Se retornar DADOS (coluna existe):
Vá para o **Passo 2**.

### ❌ Se retornar VAZIO (coluna NÃO existe):

**SOLUÇÃO:**
```powershell
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw flyway:migrate
```

Depois verifique novamente:
```sql
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'tb_tenants' AND column_name = 'tempo_lembrete_minutos';
```

---

## 2️⃣ TESTE MANUAL NO BANCO

```sql
-- Atualizar manualmente
UPDATE tb_tenants
SET tempo_lembrete_minutos = 45,
    updated_at = NOW()
WHERE tenant_key = 'kc';  -- ← Substitua pelo seu tenant

-- Verificar se funcionou
SELECT tenant_key, tempo_lembrete_minutos, updated_at
FROM tb_tenants
WHERE tenant_key = 'kc';
```

### ✅ Se o UPDATE funcionou:
O problema está na **API/DTO**. Vá para o **Passo 3**.

### ❌ Se o UPDATE NÃO funcionou:
Execute:
```sql
-- Ver erros
SHOW ERRORS;

-- Ver constraints
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'tb_tenants'::regclass;
```

---

## 3️⃣ ADICIONAR LOGS E REINICIAR

**Código já foi atualizado** com logs de debug! ✅

**Reinicie a aplicação:**
```powershell
# Parar (Ctrl+C)
# Depois:
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw spring-boot:run
```

---

## 4️⃣ TESTAR VIA API COM LOGS

```powershell
# Buscar ID do tenant
$response = Invoke-RestMethod -Uri "http://localhost:8080/tenants" -Method GET
$tenant = $response | Where-Object { $_.tenantKey -eq "kc" }
$tenantId = $tenant.id

Write-Host "Tenant ID: $tenantId"
Write-Host "Tempo atual: $($tenant.tempoLembreteMinutos) minutos"

# Atualizar
$body = @{
    businessName = $tenant.businessName
    contactEmail = $tenant.contactEmail
    contactPhone = $tenant.contactPhone
    tempoLembreteMinutos = 45  # ← IMPORTANTE: Este campo deve existir
} | ConvertTo-Json

Write-Host "Body enviado:"
Write-Host $body

$result = Invoke-RestMethod -Uri "http://localhost:8080/tenants/$tenantId" `
  -Method PUT `
  -Body $body `
  -ContentType "application/json"

Write-Host "Resposta da API:"
Write-Host "Tempo retornado: $($result.tempoLembreteMinutos) minutos"
```

---

## 5️⃣ VERIFICAR LOGS DA APLICAÇÃO

Procure por estas linhas nos logs:

```log
🔍 DEBUG - Request recebido: businessName=..., tempoLembrete=45
🔍 DEBUG - Tenant atual: key=kc, tempoLembrete=120
🔍 DEBUG - Atualizando tempo de lembrete de 120 para 45
🔍 DEBUG - Tempo após setTempoLembreteMinutos: 45
🔍 DEBUG - Salvando tenant com tempoLembrete=45
🔍 DEBUG - Tenant salvo: tempoLembrete=45, updatedAt=...
```

### ⚠️ Se aparecer: `tempoLembreteMinutos está NULL no request!`

**PROBLEMA:** O campo não está sendo enviado no body da requisição!

**SOLUÇÃO:** Certifique-se que o JSON tem o campo:
```json
{
  "businessName": "Nome do Salão",
  "contactEmail": "email@exemplo.com",
  "contactPhone": "11999999999",
  "tempoLembreteMinutos": 45  ← ESTE CAMPO É OBRIGATÓRIO
}
```

---

## 6️⃣ CONFIRMAR NO BANCO

```sql
SELECT 
    tenant_key,
    tempo_lembrete_minutos,
    updated_at,
    NOW() - updated_at AS tempo_desde_update
FROM tb_tenants
WHERE tenant_key = 'kc';
```

**Resultado esperado:**
```
tenant_key | tempo_lembrete_minutos | updated_at          | tempo_desde_update
-----------+------------------------+---------------------+-------------------
kc         | 45                     | 2026-02-13 15:30:27 | 00:00:05
```

---

## 🔧 SOLUÇÕES POR ERRO

### Erro: "column tempo_lembrete_minutos does not exist"

```powershell
.\mvnw flyway:migrate
```

### Erro: "violates check constraint"

```sql
-- Dropar constraint antiga
ALTER TABLE tb_tenants DROP CONSTRAINT IF EXISTS chk_tempo_lembrete_minutos;

-- Recriar corretamente
ALTER TABLE tb_tenants 
ADD CONSTRAINT chk_tempo_lembrete_minutos 
CHECK (tempo_lembrete_minutos > 0 AND tempo_lembrete_minutos <= 1440);
```

### Erro: Campo NULL no request

**Verifique o DTO:**
```java
// CreateTenantRequest.java deve ter:
private Integer tempoLembreteMinutos;
```

**E o JSON deve ter:**
```json
{
  "tempoLembreteMinutos": 45
}
```

---

## ✅ TESTE RÁPIDO - COPIE E COLE

```powershell
# ==================================================
# TESTE COMPLETO - COPIE E EXECUTE
# ==================================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTE DE ATUALIZAÇÃO DE TEMPO DE LEMBRETE" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Buscar tenant
Write-Host "1️⃣ Buscando tenant..." -ForegroundColor Yellow
$tenants = Invoke-RestMethod -Uri "http://localhost:8080/tenants" -Method GET
$tenant = $tenants | Where-Object { $_.tenantKey -eq "kc" }

if (-not $tenant) {
    Write-Host "❌ Tenant 'kc' não encontrado!" -ForegroundColor Red
    exit
}

Write-Host "   Tenant: $($tenant.businessName)" -ForegroundColor Green
Write-Host "   ID: $($tenant.id)" -ForegroundColor Gray
Write-Host "   Tempo ATUAL: $($tenant.tempoLembreteMinutos) minutos" -ForegroundColor Gray

# 2. Atualizar para 50 minutos
Write-Host "`n2️⃣ Atualizando para 50 minutos..." -ForegroundColor Yellow

$body = @{
    businessName = $tenant.businessName
    contactEmail = $tenant.contactEmail
    contactPhone = $tenant.contactPhone
    tempoLembreteMinutos = 50
}

Write-Host "   Body JSON:" -ForegroundColor Gray
Write-Host "   $($body | ConvertTo-Json)" -ForegroundColor Gray

$bodyJson = $body | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "http://localhost:8080/tenants/$($tenant.id)" `
        -Method PUT `
        -Body $bodyJson `
        -ContentType "application/json"
    
    Write-Host "   ✅ API retornou sucesso!" -ForegroundColor Green
    Write-Host "   Tempo na RESPOSTA: $($result.tempoLembreteMinutos) minutos" -ForegroundColor Cyan
    
    # 3. Buscar novamente
    Write-Host "`n3️⃣ Verificando no servidor..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    
    $check = Invoke-RestMethod -Uri "http://localhost:8080/tenants/$($tenant.id)" -Method GET
    Write-Host "   Tempo no GET: $($check.tempoLembreteMinutos) minutos" -ForegroundColor Cyan
    Write-Host "   Updated At: $($check.updatedAt)" -ForegroundColor Gray
    
    # 4. Resultado
    Write-Host "`n========================================" -ForegroundColor Cyan
    if ($check.tempoLembreteMinutos -eq 50) {
        Write-Host "🎉 SUCESSO TOTAL!" -ForegroundColor Green
        Write-Host "O valor foi atualizado corretamente!" -ForegroundColor Green
    } else {
        Write-Host "❌ FALHOU!" -ForegroundColor Red
        Write-Host "Valor esperado: 50" -ForegroundColor Yellow
        Write-Host "Valor recebido: $($check.tempoLembreteMinutos)" -ForegroundColor Yellow
        Write-Host "`nVERIFIQUE OS LOGS DA APLICAÇÃO!" -ForegroundColor Red
    }
    Write-Host "========================================`n" -ForegroundColor Cyan
    
} catch {
    Write-Host "   ❌ ERRO!" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
}
```

---

## 📋 CHECKLIST FINAL

Execute em ordem e marque o que funciona:

- [ ] Coluna `tempo_lembrete_minutos` existe no banco
- [ ] Migration V8 foi aplicada (ver `flyway_schema_history`)
- [ ] UPDATE manual no SQL funciona
- [ ] Aplicação reiniciada com novos logs
- [ ] API retorna 200 OK
- [ ] Resposta da API mostra novo valor
- [ ] GET após PUT mostra novo valor
- [ ] Logs mostram: "DEBUG - tempoLembrete=45" (não NULL)
- [ ] SELECT no banco mostra novo valor

---

## 🆘 SE NADA FUNCIONAR

Execute este SQL para adicionar a coluna manualmente:

```sql
-- FORÇAR criação da coluna
ALTER TABLE tb_tenants 
ADD COLUMN IF NOT EXISTS tempo_lembrete_minutos INTEGER DEFAULT 120 NOT NULL;

-- Atualizar todos os registros
UPDATE tb_tenants 
SET tempo_lembrete_minutos = 120 
WHERE tempo_lembrete_minutos IS NULL;

-- Adicionar constraint
ALTER TABLE tb_tenants 
DROP CONSTRAINT IF EXISTS chk_tempo_lembrete_minutos;

ALTER TABLE tb_tenants 
ADD CONSTRAINT chk_tempo_lembrete_minutos 
CHECK (tempo_lembrete_minutos > 0 AND tempo_lembrete_minutos <= 1440);

-- Verificar
SELECT tenant_key, tempo_lembrete_minutos FROM tb_tenants;
```

Depois:
1. Reinicie a aplicação
2. Execute o teste PowerShell acima
3. Verifique os logs

---

**EXECUTE AGORA e me envie:**
1. Resultado do SQL: `SELECT column_name FROM information_schema.columns WHERE table_name = 'tb_tenants' AND column_name = 'tempo_lembrete_minutos';`
2. Logs da aplicação após tentar atualizar
3. Resultado do teste PowerShell

