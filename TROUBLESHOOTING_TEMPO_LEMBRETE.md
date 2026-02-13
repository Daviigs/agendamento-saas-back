# 🔍 Diagnóstico: Verificar Atualização do Tempo de Lembrete

## Problema Reportado
Após atualizar o tempo de lembrete de um tenant, o sistema ainda usa o valor antigo/padrão.

## ✅ Solução Implementada

### 1. Correção no Scheduler
O scheduler agora **recarrega o tenant do banco de dados** a cada execução para garantir que sempre use o valor mais recente.

**Mudança:**
```java
// ANTES: Usava o tenant passado por parâmetro (pode estar em cache)
int minutosAntecedencia = tenant.getTempoLembreteMinutos();

// DEPOIS: Força reload do banco
TenantEntity freshTenant = tenantService.getTenantByKey(tenant.getTenantKey());
int minutosAntecedencia = freshTenant.getTempoLembreteMinutos();
```

---

## 🔍 Scripts de Diagnóstico

Execute estes comandos para verificar o problema:

### 1. Verificar valor atual no banco de dados

```sql
-- Ver configuração atual de todos os tenants
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    updated_at,
    CASE 
        WHEN tempo_lembrete_minutos = 120 THEN '⚠️ PADRÃO (120 min)'
        ELSE '✅ CUSTOMIZADO (' || tempo_lembrete_minutos || ' min)'
    END AS status
FROM tb_tenants
ORDER BY tenant_key;
```

### 2. Ver histórico de atualizações

```sql
-- Ver quando foi a última atualização de cada tenant
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    created_at,
    updated_at,
    EXTRACT(EPOCH FROM (updated_at - created_at)) / 60 AS minutos_desde_criacao,
    CASE 
        WHEN created_at::date = updated_at::date 
             AND ABS(EXTRACT(EPOCH FROM (updated_at - created_at))) < 1
        THEN '⚠️ Nunca atualizado'
        ELSE '✅ Atualizado em ' || TO_CHAR(updated_at, 'DD/MM/YYYY HH24:MI')
    END AS status_atualizacao
FROM tb_tenants
ORDER BY updated_at DESC;
```

### 3. Verificar se a atualização foi salva corretamente

```sql
-- Substitua 'seu-tenant-key' pela chave do tenant que você atualizou
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos AS valor_atual,
    updated_at AS ultima_atualizacao,
    NOW() - updated_at AS tempo_desde_atualizacao
FROM tb_tenants
WHERE tenant_key = 'kc';  -- ← ALTERE AQUI
```

---

## 🛠️ Passos para Resolver

### Opção 1: Reiniciar a Aplicação (Recomendado)

```powershell
# Parar a aplicação (Ctrl+C no terminal onde está rodando)
# Depois iniciar novamente:
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw spring-boot:run
```

Ou se estiver usando JAR compilado:
```powershell
# Matar processo Java
taskkill /F /IM java.exe

# Iniciar novamente
java -jar target/agendamento-back-0.0.1-SNAPSHOT.jar
```

### Opção 2: Aguardar próxima execução do scheduler

O scheduler executa a cada minuto e agora força o reload do banco. Na próxima execução (em até 1 minuto) ele usará o valor correto.

**Monitore os logs:**
```
📋 Tenant 'kc': buscando agendamentos entre ... (120 minutos de antecedência)
```

O valor entre parênteses deve mostrar o novo tempo configurado.

---

## ✅ Como Verificar se Funcionou

### 1. Atualizar o tenant via API

```bash
# PowerShell
$tenantId = "seu-tenant-id-uuid-aqui"
$novoTempo = 45  # Novo tempo em minutos

$body = @{
    businessName = "Nome do Salão"
    contactEmail = "email@salao.com"
    contactPhone = "11999999999"
    tempoLembreteMinutos = $novoTempo
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/tenants/$tenantId" `
  -Method PUT `
  -Body $body `
  -ContentType "application/json"
```

### 2. Verificar no banco imediatamente após

```sql
SELECT 
    tenant_key,
    tempo_lembrete_minutos,
    updated_at
FROM tb_tenants
WHERE tenant_key = 'kc';  -- ← Seu tenant
```

**Resultado esperado:**
```
tenant_key | tempo_lembrete_minutos | updated_at
-----------+------------------------+---------------------
kc         | 45                     | 2026-02-13 14:35:27
```

### 3. Verificar nos logs do scheduler (próximo minuto)

Aguarde até 1 minuto e verifique os logs:

```
🔔 Iniciando verificação de lembretes...
👥 Tenants ativos: [kc, ...]
📋 Tenant 'kc': buscando agendamentos entre 13/02/2026 14:35 e 13/02/2026 15:20 (45 minutos de antecedência)
                                                                                    ^^^ DEVE MOSTRAR O NOVO VALOR
```

---

## 🐛 Troubleshooting

### Problema: Valor não muda no banco após PUT

**Diagnóstico:**
```sql
-- Ver logs de transação (se disponível)
SELECT * FROM pg_stat_activity WHERE query LIKE '%tb_tenants%';
```

**Solução:**
- Verificar se a requisição PUT retornou sucesso (200 OK)
- Verificar se enviou o campo `tempoLembreteMinutos` no body
- Verificar logs da aplicação para ver se salvou

### Problema: Valor muda no banco mas scheduler usa antigo

**Causa:** Cache de entidade JPA ou transação não commitada

**Solução aplicada:** 
✅ Adicionado reload forçado do tenant no scheduler:
```java
TenantEntity freshTenant = tenantService.getTenantByKey(tenant.getTenantKey());
```

**Verificar se funcionou:**
- Reiniciar aplicação
- Aguardar próxima execução do scheduler
- Verificar logs

### Problema: Aplicação não reinicia

```powershell
# Verificar se há processo travado
Get-Process java

# Matar todos os processos Java
Get-Process java | Stop-Process -Force

# Iniciar novamente
.\mvnw spring-boot:run
```

---

## 📊 Script de Teste Completo

Execute este script para fazer um teste completo:

```sql
-- 1. Ver configuração ANTES
SELECT 'ANTES DA MUDANÇA' AS momento, 
       tenant_key, tempo_lembrete_minutos, updated_at 
FROM tb_tenants WHERE tenant_key = 'kc';

-- 2. Atualizar (simular o que a API faz)
UPDATE tb_tenants 
SET tempo_lembrete_minutos = 45,
    updated_at = NOW()
WHERE tenant_key = 'kc';

-- 3. Ver configuração DEPOIS
SELECT 'DEPOIS DA MUDANÇA' AS momento, 
       tenant_key, tempo_lembrete_minutos, updated_at 
FROM tb_tenants WHERE tenant_key = 'kc';

-- 4. Verificar se o valor persistiu
SELECT 
    CASE 
        WHEN tempo_lembrete_minutos = 45 THEN '✅ SUCESSO: Valor atualizado'
        ELSE '❌ ERRO: Valor não mudou (ainda é ' || tempo_lembrete_minutos || ')'
    END AS resultado
FROM tb_tenants WHERE tenant_key = 'kc';

-- Se quiser voltar ao padrão:
-- UPDATE tb_tenants SET tempo_lembrete_minutos = 120 WHERE tenant_key = 'kc';
```

---

## 📝 Checklist de Validação

- [ ] Valor atualizado via API (PUT /tenants/{id})
- [ ] API retornou 200 OK
- [ ] Valor mudou no banco de dados (SELECT confirmado)
- [ ] Aplicação reiniciada (ou aguardou 1 minuto)
- [ ] Logs do scheduler mostram novo valor
- [ ] Lembretes sendo enviados no novo horário

---

## 🎯 Resultado Esperado

**ANTES:**
```
📋 Tenant 'kc': buscando agendamentos entre 13/02/2026 12:00 e 13/02/2026 14:00 (120 minutos de antecedência)
```

**DEPOIS (com novo valor de 45 minutos):**
```
📋 Tenant 'kc': buscando agendamentos entre 13/02/2026 12:00 e 13/02/2026 12:45 (45 minutos de antecedência)
```

---

## 💡 Dica Extra: Forçar Limpeza de Cache

Se mesmo após reiniciar não funcionar:

```powershell
# Limpar tudo e recompilar
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw clean
.\mvnw install -DskipTests
.\mvnw spring-boot:run
```

---

**Correção aplicada:** O scheduler agora sempre busca o tenant atualizado do banco a cada execução. ✅

**Próximo passo:** Reinicie a aplicação e monitore os logs na próxima execução do scheduler.

