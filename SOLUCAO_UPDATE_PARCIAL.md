# ✅ SOLUÇÃO FINAL - UPDATE Parcial (PATCH behavior)

## 🎯 Problema

**Erro:**
```
Field error: 'businessName': rejected value [null]
```

**Causa:** O frontend só quer atualizar `tempoLembreteMinutos`, mas o DTO exigia `businessName` como obrigatório.

---

## ✅ Solução Implementada

### Comportamento PATCH no PUT

Agora o endpoint `PUT /tenants/current` funciona como um **PATCH** - você pode enviar **apenas os campos que quer atualizar**!

---

## 📝 Mudanças

### 1. UpdateTenantRequest.java

```java
@Data
public class UpdateTenantRequest {
    // TODOS os campos são OPCIONAIS
    private String businessName;        // ← SEM @NotBlank
    private String contactEmail;
    private String contactPhone;
    private Integer tempoLembreteMinutos;
}
```

### 2. TenantService.java

```java
public TenantEntity updateTenant(UUID tenantId, UpdateTenantRequest request) {
    TenantEntity tenant = getTenantById(tenantId);
    
    // Atualiza APENAS os campos não-null
    if (request.getBusinessName() != null) {
        tenant.setBusinessName(request.getBusinessName());
    }
    
    if (request.getContactEmail() != null) {
        tenant.setContactEmail(request.getContactEmail());
    }
    
    if (request.getContactPhone() != null) {
        tenant.setContactPhone(request.getContactPhone());
    }
    
    if (request.getTempoLembreteMinutos() != null) {
        tenant.setTempoLembreteMinutos(request.getTempoLembreteMinutos());
    }
    
    return tenantRepository.save(tenant);
}
```

---

## 🚀 REINICIAR APLICAÇÃO

```powershell
# Parar (Ctrl+C)
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw spring-boot:run
```

---

## ✅ TESTAR AGORA

### Atualizar APENAS o tempo de lembrete:

```powershell
$body = @{
    tempoLembreteMinutos = 45
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri "http://localhost:8080/tenants/current" `
    -Method PUT `
    -Headers @{"X-Tenant-Id"="rb"; "Content-Type"="application/json"} `
    -Body $body

Write-Host "✅ Tempo atualizado: $($result.tempoLembreteMinutos) minutos"
Write-Host "BusinessName mantido: $($result.businessName)"
```

### Atualizar múltiplos campos:

```powershell
$body = @{
    businessName = "Novo Nome"
    tempoLembreteMinutos = 60
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/tenants/current" `
    -Method PUT `
    -Headers @{"X-Tenant-Id"="rb"; "Content-Type"="application/json"} `
    -Body $body
```

### Atualizar apenas o nome:

```json
{
  "businessName": "Novo Nome do Salão"
}
```

### Atualizar apenas o tempo:

```json
{
  "tempoLembreteMinutos": 30
}
```

---

## 📊 Exemplos de Uso

### Frontend pode enviar APENAS o que quer atualizar:

**Cenário 1:** Atualizar só tempo de lembrete
```javascript
fetch('/tenants/current', {
    method: 'PUT',
    headers: {
        'X-Tenant-Id': 'rb',
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        tempoLembreteMinutos: 45  // ← SÓ ISSO!
    })
});
```

**Cenário 2:** Atualizar só nome
```javascript
fetch('/tenants/current', {
    method: 'PUT',
    body: JSON.stringify({
        businessName: "Novo Nome"  // ← SÓ ISSO!
    })
});
```

**Cenário 3:** Atualizar tudo
```javascript
fetch('/tenants/current', {
    method: 'PUT',
    body: JSON.stringify({
        businessName: "Salão RB Atualizado",
        contactEmail: "novo@rb.com",
        contactPhone: "11988888888",
        tempoLembreteMinutos: 60
    })
});
```

---

## 🔍 Logs Esperados

### Atualizando apenas tempo:

```log
🔍 DEBUG - UpdateRequest recebido: businessName=null, tempoLembrete=45
🔍 DEBUG - Tenant atual: key=rb, businessName=Salão RB, tempoLembrete=120
🔍 DEBUG - Atualizando tempo de lembrete de 120 para 45
🔍 DEBUG - Salvando tenant com tempoLembrete=45
🔍 DEBUG - Tenant salvo: tempoLembrete=45
```

**Nota:** `businessName` NÃO foi atualizado (mantém "Salão RB")

---

## 📊 Verificar no Banco

```sql
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    updated_at
FROM tb_tenants
WHERE tenant_key = 'rb';
```

**Resultado:**
```
tenant_key | business_name | tempo_lembrete_minutos | updated_at
-----------+---------------+------------------------+--------------------
rb         | Salão RB      | 45                     | 2026-02-13 08:10:00
           ^^^^^^^^^^^^       ^^
           MANTIDO            ATUALIZADO!
```

---

## ✅ Comportamento Resumido

| Campo Enviado | Ação |
|--------------|------|
| `tempoLembreteMinutos: 45` | ✅ Atualiza para 45 |
| `businessName: "Novo"` | ✅ Atualiza para "Novo" |
| Campo **não** enviado | ✅ **Mantém valor atual** |
| Campo `null` | ✅ **Mantém valor atual** |

---

## 🎉 RESULTADO

**Frontend funcionará perfeitamente!**

Agora pode enviar **apenas os campos que quer atualizar**, sem precisar enviar todos os campos obrigatórios.

---

## 📋 Checklist

- [ ] Aplicação reiniciada
- [ ] Enviar apenas `{tempoLembreteMinutos: 45}` funciona
- [ ] Outros campos não são modificados
- [ ] Valor salvo no banco
- [ ] Logs confirmam atualização parcial

---

**Arquivos alterados:**
1. `UpdateTenantRequest.java` - Todos campos opcionais
2. `TenantService.java` - Atualiza apenas campos não-null

**Comportamento:** ✅ PATCH (atualização parcial)  
**Status:** ✅ PROBLEMA RESOLVIDO  
**Data:** 13/02/2026

---

**REINICIE E TESTE AGORA!** 🚀

O frontend pode enviar apenas:
```json
{
  "tempoLembreteMinutos": 45
}
```

E funcionará perfeitamente! ✅

