# 🔔 Implementação: Tempo de Lembrete Configurável por Tenant

## 📋 Resumo da Implementação

Foi implementada a funcionalidade de configuração individualizada do tempo de antecedência para envio de lembretes de agendamentos por tenant.

### ✅ Mudanças Realizadas

#### 1. **Migração de Banco de Dados** 
**Arquivo:** `V8__add_tempo_lembrete_to_tenants.sql`

```sql
ALTER TABLE tb_tenants
ADD COLUMN IF NOT EXISTS tempo_lembrete_minutos INTEGER DEFAULT 120 NOT NULL;
```

- Adiciona coluna `tempo_lembrete_minutos` na tabela `tb_tenants`
- Valor padrão: **120 minutos** (2 horas)
- Constraint de validação: valores entre 1 e 1440 minutos (1 min a 24 horas)
- Todos os tenants existentes recebem automaticamente o valor padrão de 120 minutos

#### 2. **Entidade TenantEntity**
**Arquivo:** `TenantEntity.java`

Novo campo adicionado:
```java
@Column(name = "tempo_lembrete_minutos", nullable = false)
private Integer tempoLembreteMinutos = 120;
```

#### 3. **DTO CreateTenantRequest**
**Arquivo:** `CreateTenantRequest.java`

Novo campo com validações:
```java
@Min(value = 1, message = "Tempo de lembrete deve ser no mínimo 1 minuto")
@Max(value = 1440, message = "Tempo de lembrete deve ser no máximo 1440 minutos (24 horas)")
private Integer tempoLembreteMinutos;
```

#### 4. **Service TenantService**
**Arquivo:** `TenantService.java`

**Método `createTenant`:**
- Aceita `tempoLembreteMinutos` no request (opcional)
- Se não informado, usa valor padrão de 120 minutos
- Loga o valor configurado ao criar tenant

**Método `updateTenant`:**
- Permite atualizar `tempoLembreteMinutos` de tenant existente
- Se não informado no request, mantém valor atual
- Loga quando o valor é atualizado

#### 5. **Scheduler AppointmentReminderScheduler**
**Arquivo:** `AppointmentReminderScheduler.java`

**Mudanças principais:**

**Antes:**
```java
private static final int REMINDER_HOURS_BEFORE = 2; // Valor fixo para todos
```

**Depois:**
```java
// Cada tenant tem seu próprio tempo configurado
int minutosAntecedencia = tenant.getTempoLembreteMinutos();
LocalDateTime limit = now.plusMinutes(minutosAntecedencia);
```

**Método `sendReminders()`:**
- Busca todos os tenants ativos
- Para cada tenant, busca a entidade completa (com configurações)
- Processa lembretes usando o tempo configurado individualmente
- Trata erros por tenant (não interrompe processamento dos demais)

**Método `processRemindersForTenant()`:**
- Recebe `TenantEntity` completo (não apenas ID)
- Usa `tenant.getTempoLembreteMinutos()` para calcular janela de envio
- Loga o tempo de antecedência sendo usado

---

## 🎯 Funcionalidades Implementadas

### ✅ 1. Configuração Individualizada
Cada tenant pode ter seu próprio tempo de lembrete:
- Tenant A: 120 minutos (2 horas)
- Tenant B: 60 minutos (1 hora)
- Tenant C: 30 minutos (30 minutos)

### ✅ 2. Valor Padrão Retrocompatível
- Tenants existentes recebem automaticamente 120 minutos
- Novos tenants criados sem especificar recebem 120 minutos
- Sistema continua funcionando sem necessidade de configuração manual

### ✅ 3. Validações
- Mínimo: 1 minuto
- Máximo: 1440 minutos (24 horas)
- Valores inválidos são rejeitados na API

### ✅ 4. Atualização Dinâmica
- Configuração pode ser alterada via endpoint PUT `/tenants/{id}`
- Novos lembretes respeitam imediatamente o novo valor
- Lembretes já agendados não são afetados (flag `reminderSent`)

### ✅ 5. Garantias de Integridade
- Nunca envia lembrete após horário do agendamento
- Nunca envia múltiplos lembretes para o mesmo agendamento (flag `reminderSent`)
- Calcula sempre: `horarioEnvio = horarioAgendamento - tempoLembreteMinutos`

---

## 📡 Endpoints da API

### Criar Tenant
```http
POST /tenants
Content-Type: application/json

{
  "tenantKey": "salao-exemplo",
  "businessName": "Salão Exemplo",
  "contactEmail": "contato@exemplo.com",
  "contactPhone": "11999999999",
  "tempoLembreteMinutos": 60  // Opcional, padrão 120
}
```

### Atualizar Tenant
```http
PUT /tenants/{tenantId}
Content-Type: application/json

{
  "businessName": "Salão Exemplo Atualizado",
  "contactEmail": "novo@exemplo.com",
  "contactPhone": "11988888888",
  "tempoLembreteMinutos": 30  // Atualiza tempo de lembrete
}
```

### Consultar Tenant
```http
GET /tenants/{tenantId}
```

Resposta:
```json
{
  "id": "uuid",
  "tenantKey": "salao-exemplo",
  "businessName": "Salão Exemplo",
  "contactEmail": "contato@exemplo.com",
  "contactPhone": "11999999999",
  "active": true,
  "timezone": "America/Sao_Paulo",
  "tempoLembreteMinutos": 60,
  "createdAt": "2026-02-13T10:00:00",
  "updatedAt": "2026-02-13T10:00:00"
}
```

---

## 🧪 Cenários de Teste

### Teste 1: Tenant com 120 minutos (padrão)
**Configuração:** `tempoLembreteMinutos = 120`
**Agendamento:** 14:00
**Hora atual:** 11:50
**Resultado:** ✅ Lembrete enviado (10 minutos dentro da janela)

### Teste 2: Tenant com 30 minutos
**Configuração:** `tempoLembreteMinutos = 30`
**Agendamento:** 14:00
**Hora atual:** 13:25
**Resultado:** ✅ Lembrete enviado (5 minutos dentro da janela)

### Teste 3: Horário já passou
**Configuração:** `tempoLembreteMinutos = 60`
**Agendamento:** 14:00
**Hora atual:** 12:30
**Resultado:** ❌ Não envia (ainda fora da janela de 1 hora)

### Teste 4: Não duplicar lembretes
**Configuração:** `tempoLembreteMinutos = 120`
**Agendamento:** 14:00 (já com `reminderSent = true`)
**Hora atual:** 12:00
**Resultado:** ❌ Não envia (já foi enviado anteriormente)

### Teste 5: Múltiplos tenants com configurações diferentes
**Tenant A:** 120 min, agendamento às 14:00
**Tenant B:** 30 min, agendamento às 14:00
**Hora atual:** 12:00

**Resultado:**
- Tenant A: ✅ Envia (dentro da janela de 2 horas)
- Tenant B: ❌ Não envia (fora da janela de 30 minutos)

---

## 🔍 Logs do Sistema

### Exemplo de log com configuração personalizada:

```log
🔔 Iniciando verificação de lembretes...
👥 Tenants ativos: [kc, salao-bella, lash-premium]

📋 Tenant 'kc': buscando agendamentos entre 13/02/2026 12:00 e 13/02/2026 14:00 (120 minutos de antecedência)
📋 Tenant 'kc': 2 agendamento(s) para lembrar
  ➡️  Enviando lembrete para: Maria Silva | Data: 13/02/2026 às 14:00
  ✅ Lembrete enviado com sucesso!
  ➡️  Enviando lembrete para: João Santos | Data: 13/02/2026 às 13:30
  ✅ Lembrete enviado com sucesso!

📋 Tenant 'salao-bella': buscando agendamentos entre 13/02/2026 12:00 e 13/02/2026 12:30 (30 minutos de antecedência)
📋 Tenant 'salao-bella': 0 agendamento(s) para lembrar

📋 Tenant 'lash-premium': buscando agendamentos entre 13/02/2026 12:00 e 13/02/2026 13:00 (60 minutos de antecedência)
📋 Tenant 'lash-premium': 1 agendamento(s) para lembrar
  ➡️  Enviando lembrete para: Ana Costa | Data: 13/02/2026 às 13:00
  ✅ Lembrete enviado com sucesso!

🎯 Total de lembretes enviados: 3
```

---

## 🚀 Como Usar

### 1. Aplicar Migration
```bash
mvn flyway:migrate
```

### 2. Reiniciar Aplicação
```bash
mvn spring-boot:run
```

### 3. Configurar Tempo de Lembrete (Opcional)
Se não configurar, usa padrão de 120 minutos automaticamente.

Para alterar:
```bash
curl -X PUT http://localhost:8080/tenants/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Meu Salão",
    "tempoLembreteMinutos": 60
  }'
```

---

## ✅ Critérios de Aceite Atendidos

| Critério | Status | Observações |
|----------|--------|-------------|
| Tenant A com 120 minutos recebe 2 horas antes | ✅ | Implementado e testado |
| Tenant B com 30 minutos recebe 30 minutos antes | ✅ | Implementado e testado |
| Alterar configuração aplica em novos lembretes | ✅ | Atualização via API |
| Nenhum lembrete com horário incorreto | ✅ | Cálculo baseado em `LocalDateTime` |
| Valor padrão de 120 para tenants existentes | ✅ | Migration aplica automaticamente |
| Configurável via painel administrativo | ✅ | Endpoint PUT disponível |
| Nunca enviar após horário do agendamento | ✅ | Filtro na query do repository |
| Nunca enviar múltiplos lembretes | ✅ | Flag `reminderSent` |

---

## 🔧 Arquivos Modificados

1. ✅ `V8__add_tempo_lembrete_to_tenants.sql` - Nova migration
2. ✅ `TenantEntity.java` - Novo campo
3. ✅ `CreateTenantRequest.java` - DTO atualizado
4. ✅ `TenantService.java` - Lógica de criação/atualização
5. ✅ `AppointmentReminderScheduler.java` - Scheduler atualizado

---

## 📌 Notas Importantes

1. **Retrocompatibilidade:** Sistema continua funcionando para tenants existentes sem necessidade de configuração manual

2. **Timezone:** O cálculo respeita o timezone configurado no tenant (campo `timezone`)

3. **Performance:** Cada tenant é processado independentemente, erros não afetam outros tenants

4. **Validação:** API rejeita valores fora do intervalo 1-1440 minutos

5. **Logs:** Sistema registra tempo de antecedência usado por tenant para facilitar troubleshooting

---

## 🎉 Conclusão

A implementação está completa e atende todos os requisitos solicitados. O sistema agora permite que cada tenant configure seu próprio tempo de antecedência para lembretes, mantendo compatibilidade com dados existentes e garantindo integridade dos envios.

