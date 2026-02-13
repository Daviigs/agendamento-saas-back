# 🔍 DIAGNÓSTICO: Valor Não Atualiza no Banco

## 🎯 Problema Identificado
A API retorna sucesso, mas o valor de `tempo_lembrete_minutos` não é salvo no banco de dados.

---

## 📝 SCRIPT DE DIAGNÓSTICO COMPLETO

Execute os scripts abaixo **EM ORDEM** para identificar o problema:

### 1️⃣ Verificar se a coluna existe

```sql
-- Verificar estrutura da tabela
SELECT 
    column_name,
    data_type,
    column_default,
    is_nullable,
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'tb_tenants'
ORDER BY ordinal_position;
```

**Resultado esperado:** Deve aparecer `tempo_lembrete_minutos` na lista.

---

### 2️⃣ Verificar se a migration foi aplicada

```sql
-- Ver histórico de migrations
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
```

**Buscar por:** `V8__add_tempo_lembrete_to_tenants.sql`

**Se NÃO aparecer:** A migration não foi executada!

---

### 3️⃣ Verificar valor atual

```sql
-- Ver valor atual de todos os tenants
SELECT 
    tenant_id,
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    active,
    updated_at
FROM tb_tenants
ORDER BY tenant_key;
```

---

### 4️⃣ Tentar atualizar manualmente no banco

```sql
-- Teste manual de UPDATE
UPDATE tb_tenants
SET tempo_lembrete_minutos = 45,
    updated_at = NOW()
WHERE tenant_key = 'kc';

-- Verificar se funcionou
SELECT 
    tenant_key,
    tempo_lembrete_minutos,
    updated_at
FROM tb_tenants
WHERE tenant_key = 'kc';
```

**Se FUNCIONAR:** O problema está na API/DTO, não no banco.  
**Se NÃO FUNCIONAR:** Há problema de constraint ou permissão.

---

## 🛠️ SOLUÇÕES POR CENÁRIO

### ❌ Cenário 1: Coluna não existe

**Causa:** Migration não foi executada.

**Solução:**
```powershell
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw flyway:migrate
```

**Verificar:**
```sql
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'tb_tenants' AND column_name = 'tempo_lembrete_minutos';
```

---

### ❌ Cenário 2: Constraint impedindo UPDATE

**Erro típico:**
```
ERROR: new row for relation "tb_tenants" violates check constraint "chk_tempo_lembrete_minutos"
```

**Causa:** Valor fora do intervalo 1-1440.

**Solução:** Verificar constraint:
```sql
-- Ver constraint
SELECT 
    conname,
    pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'tb_tenants'::regclass 
  AND conname = 'chk_tempo_lembrete_minutos';

-- Se estiver incorreta, dropar e recriar
ALTER TABLE tb_tenants DROP CONSTRAINT IF EXISTS chk_tempo_lembrete_minutos;

ALTER TABLE tb_tenants 
ADD CONSTRAINT chk_tempo_lembrete_minutos 
CHECK (tempo_lembrete_minutos > 0 AND tempo_lembrete_minutos <= 1440);
```

---

### ❌ Cenário 3: Campo não está sendo enviado na API

**Diagnóstico via logs:**

Adicione log no método `updateTenant`:

```java
log.info("🔍 DEBUG - Request recebido: businessName={}, tempoLembrete={}", 
    request.getBusinessName(), 
    request.getTempoLembreteMinutos());
```

**Se aparecer `tempoLembrete=null`:** O campo não está sendo enviado no body.

**Solução:** Verificar o body da requisição:

```json
{
  "businessName": "KC Salão",
  "contactEmail": "contato@kc.com",
  "contactPhone": "11999999999",
  "tempoLembreteMinutos": 45  ← ESTE CAMPO DEVE ESTAR PRESENTE
}
```

---

### ❌ Cenário 4: JPA não está salvando (problema de transação)

**Causa:** Transação não está sendo commitada.

**Solução temporária - Forçar flush:**

```java
@Transactional
public TenantEntity updateTenant(UUID tenantId, CreateTenantRequest request) {
    TenantEntity tenant = getTenantById(tenantId);

    tenant.setBusinessName(request.getBusinessName());
    tenant.setContactEmail(request.getContactEmail());
    tenant.setContactPhone(request.getContactPhone());

    if (request.getTempoLembreteMinutos() != null) {
        tenant.setTempoLembreteMinutos(request.getTempoLembreteMinutos());
        log.info("Tenant {}: tempo de lembrete atualizado para {} minutos",
                tenant.getTenantKey(), request.getTempoLembreteMinutos());
    }

    log.info("Tenant atualizado: {}", tenant.getTenantKey());
    
    TenantEntity saved = tenantRepository.save(tenant);
    tenantRepository.flush(); // ← FORÇAR COMMIT
    
    return saved;
}
```

---

## 🧪 TESTE COMPLETO VIA SQL

Execute este script para fazer um teste completo:

```sql
-- ========================================
-- TESTE COMPLETO DE UPDATE
-- ========================================

-- 1. Ver estado ANTES
SELECT 'ANTES' AS momento, tenant_key, tempo_lembrete_minutos, updated_at 
FROM tb_tenants WHERE tenant_key = 'kc';

-- 2. Fazer UPDATE
UPDATE tb_tenants 
SET tempo_lembrete_minutos = 75,
    updated_at = NOW()
WHERE tenant_key = 'kc';

-- 3. Ver estado DEPOIS
SELECT 'DEPOIS' AS momento, tenant_key, tempo_lembrete_minutos, updated_at 
FROM tb_tenants WHERE tenant_key = 'kc';

-- 4. Verificar se mudou
SELECT 
    CASE 
        WHEN tempo_lembrete_minutos = 75 
        THEN '✅ UPDATE FUNCIONOU!'
        ELSE '❌ UPDATE NÃO FUNCIONOU - Valor: ' || tempo_lembrete_minutos
    END AS resultado
FROM tb_tenants WHERE tenant_key = 'kc';

-- 5. Rollback se quiser voltar
-- ROLLBACK;
-- Ou commit para manter:
-- COMMIT;
```

---

## 🔍 TESTE VIA API COM DEBUG

Use este script PowerShell para testar a API com debug detalhado:

```powershell
# Configuração
$baseUrl = "http://localhost:8080"
$tenantKey = "kc"  # Substitua pelo seu tenant

# 1. Buscar ID do tenant
Write-Host "📊 PASSO 1: Buscando tenant..." -ForegroundColor Yellow
$tenants = Invoke-RestMethod -Uri "$baseUrl/tenants" -Method GET
$tenant = $tenants | Where-Object { $_.tenantKey -eq $tenantKey }

if (-not $tenant) {
    Write-Host "❌ Tenant '$tenantKey' não encontrado!" -ForegroundColor Red
    exit
}

$tenantId = $tenant.id
Write-Host "✅ Tenant encontrado: $($tenant.businessName)" -ForegroundColor Green
Write-Host "   ID: $tenantId" -ForegroundColor Cyan
Write-Host "   Tempo atual: $($tenant.tempoLembreteMinutos) minutos" -ForegroundColor Cyan

# 2. Atualizar com novo valor
Write-Host "`n🔄 PASSO 2: Atualizando para 45 minutos..." -ForegroundColor Yellow

$body = @{
    businessName = $tenant.businessName
    contactEmail = $tenant.contactEmail
    contactPhone = $tenant.contactPhone
    tempoLembreteMinutos = 45
} | ConvertTo-Json

Write-Host "Body da requisição:" -ForegroundColor Gray
Write-Host $body -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/tenants/$tenantId" `
        -Method PUT `
        -Body $body `
        -ContentType "application/json"
    
    Write-Host "✅ API retornou sucesso!" -ForegroundColor Green
    Write-Host "   Novo valor na resposta: $($response.tempoLembreteMinutos) minutos" -ForegroundColor Cyan
    
    # 3. Buscar novamente para confirmar
    Write-Host "`n🔍 PASSO 3: Verificando no banco..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    
    $verificacao = Invoke-RestMethod -Uri "$baseUrl/tenants/$tenantId" -Method GET
    
    Write-Host "   Valor atual: $($verificacao.tempoLembreteMinutos) minutos" -ForegroundColor Cyan
    Write-Host "   Updated at: $($verificacao.updatedAt)" -ForegroundColor Cyan
    
    if ($verificacao.tempoLembreteMinutos -eq 45) {
        Write-Host "`n🎉 SUCESSO! Valor foi atualizado corretamente!" -ForegroundColor Green
    } else {
        Write-Host "`n❌ ERRO! Valor não mudou no banco!" -ForegroundColor Red
        Write-Host "   Esperado: 45" -ForegroundColor Yellow
        Write-Host "   Recebido: $($verificacao.tempoLembreteMinutos)" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "❌ ERRO na API!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
```

---

## 🎯 SOLUÇÃO DEFINITIVA

Se nada funcionar, aplique esta correção no código:

### Arquivo: `TenantService.java`

```java
@Transactional
public TenantEntity updateTenant(UUID tenantId, CreateTenantRequest request) {
    log.info("🔍 Atualizando tenant: {}", tenantId);
    log.info("🔍 Request: businessName={}, email={}, phone={}, tempoLembrete={}", 
        request.getBusinessName(), 
        request.getContactEmail(), 
        request.getContactPhone(),
        request.getTempoLembreteMinutos());
    
    TenantEntity tenant = getTenantById(tenantId);
    
    log.info("🔍 Tenant atual: key={}, tempoLembrete={}", 
        tenant.getTenantKey(), 
        tenant.getTempoLembreteMinutos());

    tenant.setBusinessName(request.getBusinessName());
    tenant.setContactEmail(request.getContactEmail());
    tenant.setContactPhone(request.getContactPhone());

    // SEMPRE atualizar, mesmo que seja null (mantém o valor atual)
    Integer novoTempo = request.getTempoLembreteMinutos();
    if (novoTempo != null && novoTempo >= 1 && novoTempo <= 1440) {
        log.info("🔍 Atualizando tempo de {} para {}", 
            tenant.getTempoLembreteMinutos(), novoTempo);
        tenant.setTempoLembreteMinutos(novoTempo);
    } else if (novoTempo != null) {
        log.warn("⚠️ Valor inválido: {}. Mantendo valor atual: {}", 
            novoTempo, tenant.getTempoLembreteMinutos());
    }

    log.info("🔍 Antes do save: tempoLembrete={}", tenant.getTempoLembreteMinutos());
    TenantEntity saved = tenantRepository.save(tenant);
    log.info("🔍 Depois do save: tempoLembrete={}", saved.getTempoLembreteMinutos());

    return saved;
}
```

---

## ✅ CHECKLIST DE DIAGNÓSTICO

Execute em ordem:

1. [ ] Migration foi aplicada? (Ver `flyway_schema_history`)
2. [ ] Coluna existe? (Ver `information_schema.columns`)
3. [ ] UPDATE manual funciona? (Executar UPDATE direto no banco)
4. [ ] Body da API contém o campo? (Ver logs da aplicação)
5. [ ] API retorna o valor correto na resposta?
6. [ ] GET após UPDATE mostra o novo valor?
7. [ ] Logs mostram o `tempoLembreteMinutos` sendo setado?

---

## 📞 PRÓXIMO PASSO

**Execute agora:**

1. Este SQL:
```sql
SELECT 
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name = 'tb_tenants'
  AND column_name = 'tempo_lembrete_minutos';
```

2. Se retornar **vazio** → Execute: `.\mvnw flyway:migrate`

3. Se retornar **dados** → Execute o teste PowerShell acima e envie o resultado.

---

**Aguardando resultado dos testes para identificar a causa exata!** 🔍

