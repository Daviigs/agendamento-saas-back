# 📊 Diagrama Visual - Exceções de Bloqueios Recorrentes

## 🎯 Visão Geral do Sistema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SISTEMA DE BLOQUEIO DE DATAS                         │
│                                                                         │
│  ┌───────────────┐  ┌─────────────────┐  ┌──────────────────────┐    │
│  │   Bloqueio    │  │   Bloqueio      │  │     Exceções         │    │
│  │   Específico  │  │   Recorrente    │  │   (Liberação)        │    │
│  │               │  │                 │  │                      │    │
│  │  Ex: 25/12    │  │  Ex: Domingos   │  │  Ex: 15/02 liberado  │    │
│  │  (Natal)      │  │  (Folga)        │  │  (Trabalho extra)    │    │
│  └───────────────┘  └─────────────────┘  └──────────────────────┘    │
│         │                    │                       │                 │
│         └────────────────────┴───────────────────────┘                 │
│                              │                                         │
│                    ┌─────────▼─────────┐                              │
│                    │   Validação de    │                              │
│                    │   Agendamento     │                              │
│                    └───────────────────┘                              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Fluxo de Decisão Detalhado

```
                      VALIDAÇÃO DE DATA
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │ Existe bloqueio específico para       │
        │ essa data?                            │
        │ (tb_blocked_days.specific_date)       │
        └───────────┬───────────────────┬───────┘
                    │                   │
                   SIM                 NÃO
                    │                   │
                    ▼                   ▼
         ┌──────────────────┐  ┌─────────────────────────────┐
         │                  │  │ Existe exceção liberando     │
         │  ❌ BLOQUEADO    │  │ essa data?                   │
         │                  │  │ (tb_blocked_day_exceptions)  │
         │  (Prioridade 1)  │  └────────┬──────────────┬─────┘
         │                  │           │              │
         └──────────────────┘          SIM            NÃO
                                        │              │
                                        ▼              ▼
                             ┌──────────────────┐  ┌───────────────────────┐
                             │                  │  │ Existe bloqueio       │
                             │  ✅ LIBERADO     │  │ recorrente para o dia │
                             │                  │  │ da semana?            │
                             │  (Prioridade 2)  │  │ (day_of_week)         │
                             │                  │  └────┬──────────┬───────┘
                             └──────────────────┘       │          │
                                                       SIM        NÃO
                                                        │          │
                                                        ▼          ▼
                                             ┌──────────────────┐  ┌──────────────────┐
                                             │                  │  │                  │
                                             │  ❌ BLOQUEADO    │  │  ✅ LIBERADO     │
                                             │                  │  │                  │
                                             │  (Prioridade 3)  │  │  (Prioridade 4)  │
                                             │                  │  │                  │
                                             └──────────────────┘  └──────────────────┘
```

---

## 📅 Exemplo Visual - Calendário de Fevereiro 2026

### Configuração:
- ❌ Todos os domingos bloqueados (recorrente)
- ✅ Domingo 15/02/2026 liberado (exceção)

```
        Fevereiro 2026
  ┌─────────────────────────────────┐
  │ DOM SEG TER QUA QUI SEX SAB     │
  ├─────────────────────────────────┤
  │  🔴   2   3   4   5   6   7     │  🔴 = Bloqueado (domingo recorrente)
  │  🔴   9  10  11  12  13  14     │  
  │  🟢  16  17  18  19  20  21     │  🟢 = Liberado (exceção)
  │  🔴  23  24  25  26  27  28     │  
  └─────────────────────────────────┘

  Legenda:
  🔴 = Bloqueado (domingo sem exceção)
  🟢 = Liberado (domingo com exceção)
  ⚪ = Normal (dias da semana)
```

---

## 🎭 Cenários de Prioridade

### Cenário A: Apenas Bloqueio Recorrente
```
┌────────────────────────────────────┐
│ Data: 08/02/2026 (Domingo)         │
├────────────────────────────────────┤
│ Bloqueio específico: ❌ Não        │
│ Exceção: ❌ Não                    │
│ Bloqueio recorrente: ✅ Sim        │
├────────────────────────────────────┤
│ RESULTADO: ❌ BLOQUEADO            │
└────────────────────────────────────┘
```

### Cenário B: Exceção Liberando
```
┌────────────────────────────────────┐
│ Data: 15/02/2026 (Domingo)         │
├────────────────────────────────────┤
│ Bloqueio específico: ❌ Não        │
│ Exceção: ✅ Sim                    │
│ Bloqueio recorrente: ✅ Sim        │
├────────────────────────────────────┤
│ RESULTADO: ✅ LIBERADO             │
└────────────────────────────────────┘
```

### Cenário C: Bloqueio Específico Vence
```
┌────────────────────────────────────┐
│ Data: 25/12/2026 (Sexta)           │
├────────────────────────────────────┤
│ Bloqueio específico: ✅ Sim (Natal)│
│ Exceção: ✅ Sim                    │
│ Bloqueio recorrente: ❌ Não        │
├────────────────────────────────────┤
│ RESULTADO: ❌ BLOQUEADO            │
│ (específico tem prioridade)        │
└────────────────────────────────────┘
```

### Cenário D: Dia Normal
```
┌────────────────────────────────────┐
│ Data: 10/02/2026 (Terça)           │
├────────────────────────────────────┤
│ Bloqueio específico: ❌ Não        │
│ Exceção: ❌ Não                    │
│ Bloqueio recorrente: ❌ Não        │
├────────────────────────────────────┤
│ RESULTADO: ✅ LIBERADO             │
└────────────────────────────────────┘
```

---

## 🗄️ Modelo de Dados

```
┌─────────────────────────────────────────────────────────────────┐
│                      tb_blocked_days                            │
├─────────────────────────────────────────────────────────────────┤
│ blocked_day_id (UUID) PK                                        │
│ tenant_id (VARCHAR)                                             │
│ specific_date (DATE) - Ex: 2026-12-25                           │
│ day_of_week (ENUM) - Ex: SUNDAY                                 │
│ reason (VARCHAR)                                                │
│ is_recurring (BOOLEAN)                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Validado junto com ▼
                              │
┌─────────────────────────────────────────────────────────────────┐
│                  tb_blocked_day_exceptions                      │
├─────────────────────────────────────────────────────────────────┤
│ exception_id (UUID) PK                                          │
│ tenant_id (VARCHAR)                                             │
│ exception_date (DATE) - Ex: 2026-02-15                          │
│ reason (VARCHAR) - Ex: "Trabalho extra"                         │
│ created_at (TIMESTAMP)                                          │
│                                                                 │
│ UNIQUE(tenant_id, exception_date)                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔀 Fluxo de Criação de Exceção

```
    USUÁRIO
       │
       │ POST /blocked-days/exceptions
       │ { "exceptionDate": "2026-02-15", "reason": "Trabalho extra" }
       ▼
┌──────────────────────┐
│ Controller           │
│ Valida Request       │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│ Service              │
│ 1. Valida duplicata  │──── Já existe? ──> ❌ Erro 409
│ 2. Valida bloqueio   │
│    específico        │──── Tem bloqueio específico? ──> ❌ Erro 409
│ 3. Salva exceção     │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│ Repository           │
│ INSERT INTO          │
│ tb_blocked_day_      │
│ exceptions           │
└──────┬───────────────┘
       │
       ▼
    ✅ 201 Created
```

---

## 🎬 Caso de Uso Real

### Situação: Salão normalmente fechado aos domingos

```
┌─────────────────────────────────────────────────────────────────┐
│                         CALENDÁRIO ANUAL                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Janeiro   │ 🔴 🔴 🔴 🔴  (Todos domingos bloqueados)           │
│  Fevereiro │ 🔴 🟢 🔴 🔴  (15/02 liberado para evento)          │
│  Março     │ 🔴 🔴 🔴 🔴  (Todos domingos bloqueados)           │
│  Abril     │ 🔴 🔴 🟢 🔴  (12/04 liberado para reposição)       │
│  Maio      │ 🔴 🔴 🔴 🔴  (Todos domingos bloqueados)           │
│  Junho     │ 🔴 🔴 🔴 🔴  (Todos domingos bloqueados)           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

Configuração:
1. Bloqueio recorrente: Todos os DOMINGOS
2. Exceções:
   - 15/02/2026: "Evento especial de inauguração"
   - 12/04/2026: "Reposição por feriado"
```

---

## 📊 Matriz de Decisão

```
┌─────────────┬──────────────┬──────────┬──────────────┬────────────┐
│   Data      │  Específico  │ Exceção  │  Recorrente  │  Resultado │
├─────────────┼──────────────┼──────────┼──────────────┼────────────┤
│ 08/02 (Dom) │      ❌      │    ❌    │      ✅      │     ❌     │
│ 10/02 (Ter) │      ❌      │    ❌    │      ❌      │     ✅     │
│ 15/02 (Dom) │      ❌      │    ✅    │      ✅      │     ✅     │
│ 22/02 (Dom) │      ❌      │    ❌    │      ✅      │     ❌     │
│ 25/12 (Sex) │ ✅ (Natal)   │    ❌    │      ❌      │     ❌     │
│ 25/12 (Sex) │ ✅ (Natal)   │    ✅    │      ❌      │     ❌     │
└─────────────┴──────────────┴──────────┴──────────────┴────────────┘

Legenda:
✅ = Sim / Liberado
❌ = Não / Bloqueado
```

---

## 🔍 Ordem de Verificação no Código

```java
public boolean isDateBlocked(LocalDate date) {
    String tenantId = TenantContext.getTenantId();

    // ┌─────────────────────────────────────┐
    // │ PRIORIDADE 1: Bloqueio Específico   │
    // │ Se existe, sempre bloqueia          │
    // └─────────────────────────────────────┘
    if (blockedDayRepository
        .findByTenantIdAndSpecificDate(tenantId, date)
        .isPresent()) {
        return true; // ❌ BLOQUEADO
    }

    // ┌─────────────────────────────────────┐
    // │ PRIORIDADE 2: Exceção               │
    // │ Se existe, libera mesmo com         │
    // │ bloqueio recorrente                 │
    // └─────────────────────────────────────┘
    if (blockedDayExceptionRepository
        .findByTenantIdAndExceptionDate(tenantId, date)
        .isPresent()) {
        return false; // ✅ LIBERADO
    }

    // ┌─────────────────────────────────────┐
    // │ PRIORIDADE 3: Bloqueio Recorrente   │
    // │ Verifica dia da semana              │
    // └─────────────────────────────────────┘
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    return blockedDayRepository
        .findByTenantIdAndDayOfWeekAndRecurring(
            tenantId, dayOfWeek, true)
        .isPresent(); // ❌ ou ✅
}
```

---

## 🎨 Interface de Usuário (Sugestão)

```
┌─────────────────────────────────────────────────────────────┐
│  Gerenciamento de Dias Bloqueados                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Bloqueios Recorrentes:                                     │
│  ┌───────────────────────────────────────────────────┐     │
│  │ 🔴 SUNDAY    - Folga semanal          [Remover]   │     │
│  │ 🔴 SATURDAY  - Folga semanal          [Remover]   │     │
│  └───────────────────────────────────────────────────┘     │
│  [+ Adicionar Bloqueio Recorrente]                          │
│                                                             │
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  Exceções (Dias Liberados):                                 │
│  ┌───────────────────────────────────────────────────┐     │
│  │ 🟢 15/02/2026 - Trabalho extra        [Remover]   │     │
│  │ 🟢 12/04/2026 - Reposição             [Remover]   │     │
│  └───────────────────────────────────────────────────┘     │
│  [+ Adicionar Exceção]                                      │
│                                                             │
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  Bloqueios Específicos:                                     │
│  ┌───────────────────────────────────────────────────┐     │
│  │ 🔴 25/12/2026 - Natal                 [Remover]   │     │
│  │ 🔴 01/01/2027 - Ano Novo              [Remover]   │     │
│  └───────────────────────────────────────────────────┘     │
│  [+ Adicionar Bloqueio Específico]                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📈 Performance e Índices

```
Query: Verificar se data 15/02/2026 está bloqueada

Step 1: Bloqueio Específico
┌─────────────────────────────────────────┐
│ SELECT * FROM tb_blocked_days           │
│ WHERE tenant_id = 'kc'                  │
│   AND specific_date = '2026-02-15'      │
│                                         │
│ Usa índice: idx_blocked_specific_date   │
│ Performance: O(1)                       │
└─────────────────────────────────────────┘
              │
              ▼ Não encontrado
              │
Step 2: Exceção
┌─────────────────────────────────────────┐
│ SELECT * FROM                           │
│ tb_blocked_day_exceptions               │
│ WHERE tenant_id = 'kc'                  │
│   AND exception_date = '2026-02-15'     │
│                                         │
│ Usa índice:                             │
│ idx_blocked_day_exceptions_date         │
│ Performance: O(1)                       │
└─────────────────────────────────────────┘
              │
              ▼ Encontrado! ✅ LIBERADO
```

---

## 🎯 Resumo Visual

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  ORDEM DE PRIORIDADE - Validação de Data            ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃                                                       ┃
┃  1️⃣  Bloqueio Específico (tb_blocked_days)          ┃
┃      └─> ✅ Existe? → ❌ SEMPRE BLOQUEADO            ┃
┃                                                       ┃
┃  2️⃣  Exceção (tb_blocked_day_exceptions)            ┃
┃      └─> ✅ Existe? → ✅ SEMPRE LIBERADO             ┃
┃                                                       ┃
┃  3️⃣  Bloqueio Recorrente (tb_blocked_days)          ┃
┃      └─> ✅ Existe? → ❌ BLOQUEADO                   ┃
┃                                                       ┃
┃  4️⃣  Padrão                                          ┃
┃      └─> ✅ LIBERADO                                 ┃
┃                                                       ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

