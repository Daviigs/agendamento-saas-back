# 🎯 FEATURE: Exceções de Bloqueios Recorrentes

## 📋 Resumo

Nova funcionalidade que permite **liberar datas específicas** que caem em **dias bloqueados recorrentes**.

### Exemplo de Uso
- ✅ Todos os domingos são bloqueados (recorrente)
- ❌ Mas o dono quer trabalhar no domingo 15/02/2026
- ✅ Cria-se uma **exceção** para essa data específica
- ✅ Agendamentos serão permitidos apenas nesse domingo específico

---

## 🧩 Regras de Negócio

### Ordem de Prioridade para Validação de Agendamento

O sistema agora valida datas na seguinte ordem:

1. **❌ Dia bloqueado pontual (específico)** - MAIOR PRIORIDADE
   - Se existe um bloqueio específico para a data, ela sempre fica bloqueada
   - Exemplo: 25/12/2026 bloqueado por "Natal"

2. **✅ Dia liberado por exceção**
   - Se existe uma exceção para a data, ela fica liberada mesmo com bloqueio recorrente
   - Exemplo: Domingo 15/02/2026 liberado por "Trabalho extra"

3. **❌ Dia bloqueado recorrente**
   - Se o dia da semana está bloqueado recorrentemente
   - Exemplo: Todos os domingos

4. **✅ Dia permitido**
   - Dia normal sem bloqueios

### Validações

- ✅ Não é possível criar exceção para uma data que já tem bloqueio específico
- ✅ Não é possível criar exceção duplicada para a mesma data
- ✅ Exceções só afetam bloqueios recorrentes, não bloqueios específicos

---

## 🗄️ Banco de Dados

### Nova Tabela: `tb_blocked_day_exceptions`

```sql
CREATE TABLE tb_blocked_day_exceptions (
    exception_id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    exception_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenant_exception_date UNIQUE (tenant_id, exception_date)
);
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `exception_id` | UUID | Identificador único da exceção |
| `tenant_id` | VARCHAR(50) | ID do tenant (isolamento multi-tenant) |
| `exception_date` | DATE | Data específica liberada |
| `reason` | VARCHAR(500) | Motivo da liberação |
| `created_at` | TIMESTAMP | Data de criação do registro |

### Índices

- `idx_blocked_day_exceptions_tenant` - Otimiza consultas por tenant
- `idx_blocked_day_exceptions_date` - Otimiza consultas por tenant + data

---

## 🚀 Arquivos Criados/Modificados

### ✨ Novos Arquivos

1. **Entity**: `BlockedDayExceptionEntity.java`
   - Representa exceções no modelo de dados

2. **Repository**: `BlockedDayExceptionRepository.java`
   - Acesso aos dados das exceções

3. **DTO**: `CreateBlockedDayExceptionRequest.java`
   - Request para criar exceções

4. **Controller**: `BlockedDayExceptionController.java`
   - Endpoints REST para gerenciar exceções

5. **Migration**: `V5__create_blocked_day_exceptions_table.sql`
   - Script Flyway para criar a tabela

### 🔧 Arquivos Modificados

1. **Service**: `BlockedDayService.java`
   - ✅ Atualizado método `isDateBlocked()` com nova lógica de prioridade
   - ✅ Adicionados métodos para gerenciar exceções:
     - `createException()`
     - `getAllExceptions()`
     - `getFutureExceptions()`
     - `deleteException()`

---

## 📡 API Endpoints

### Base URL: `/blocked-days/exceptions`

Todos os endpoints requerem o header `X-Tenant-Id`.

---

### 1️⃣ Criar Exceção

Libera uma data específica de um bloqueio recorrente.

**Endpoint:**
```http
POST /blocked-days/exceptions
```

**Request Body:**
```json
{
  "exceptionDate": "2026-02-15",
  "reason": "Trabalho extra"
}
```

**Response: 201 Created**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "tenantId": "kc",
  "exceptionDate": "2026-02-15",
  "reason": "Trabalho extra"
}
```

**Validações:**
- ❌ Data é obrigatória
- ❌ Motivo é obrigatório
- ❌ Não pode haver exceção duplicada
- ❌ Não pode criar exceção para data com bloqueio específico

---

### 2️⃣ Listar Todas as Exceções

Retorna todas as exceções do tenant.

**Endpoint:**
```http
GET /blocked-days/exceptions
```

**Response: 200 OK**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "tenantId": "kc",
    "exceptionDate": "2026-02-15",
    "reason": "Trabalho extra"
  },
  {
    "id": "123e4567-e89b-12d3-a456-426614174001",
    "tenantId": "kc",
    "exceptionDate": "2026-03-22",
    "reason": "Reposição"
  }
]
```

---

### 3️⃣ Listar Exceções Futuras

Retorna apenas exceções a partir da data atual.

**Endpoint:**
```http
GET /blocked-days/exceptions/future
```

**Response: 200 OK**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174001",
    "tenantId": "kc",
    "exceptionDate": "2026-03-22",
    "reason": "Reposição"
  }
]
```

---

### 4️⃣ Remover Exceção

Remove uma exceção existente.

**Endpoint:**
```http
DELETE /blocked-days/exceptions/{exceptionId}
```

**Response: 204 No Content**

---

## 🧪 Exemplos de Uso

### Cenário 1: Trabalhar em um Domingo Específico

**Situação:**
- Todos os domingos são bloqueados (recorrente)
- Preciso trabalhar no domingo 15/02/2026

**Solução:**

1. **Criar exceção:**
```bash
POST /blocked-days/exceptions
{
  "exceptionDate": "2026-02-15",
  "reason": "Trabalho extra para evento especial"
}
```

2. **Resultado:**
   - ✅ 15/02/2026 (domingo) - **LIBERADO**
   - ❌ 22/02/2026 (domingo) - Bloqueado
   - ❌ 01/03/2026 (domingo) - Bloqueado

---

### Cenário 2: Feriado em Dia Normalmente Permitido

**Situação:**
- Segunda-feira normalmente é dia de trabalho
- Mas segunda-feira 23/02/2026 é feriado (Carnaval)

**Solução:**

1. **Criar bloqueio específico (não usar exceção):**
```bash
POST /blocked-days/specific
{
  "date": "2026-02-23",
  "reason": "Carnaval - Feriado"
}
```

2. **Resultado:**
   - ❌ 23/02/2026 - BLOQUEADO (bloqueio específico tem prioridade máxima)

---

### Cenário 3: Tentativa Incorreta

**Situação:**
- 25/12/2026 (Natal) já está bloqueado como data específica
- Tentativa de criar exceção para essa data

**Solução:**
```bash
POST /blocked-days/exceptions
{
  "exceptionDate": "2026-12-25",
  "reason": "Quero trabalhar no Natal"
}
```

**Resultado:**
```json
{
  "error": "DuplicateResourceException",
  "message": "Esta data possui um bloqueio específico. Remova o bloqueio específico ao invés de criar uma exceção."
}
```

**Ação Correta:**
- Remover o bloqueio específico do Natal: `DELETE /blocked-days/{blockedDayId}`

---

## 🔄 Fluxo de Validação

### Método `isDateBlocked(LocalDate date)`

```java
public boolean isDateBlocked(LocalDate date) {
    String tenantId = TenantContext.getTenantId();

    // PRIORIDADE 1: Bloqueio específico sempre tem prioridade
    if (blockedDayRepository.findByTenantIdAndSpecificDate(tenantId, date).isPresent()) {
        return true; // ❌ BLOQUEADO
    }

    // PRIORIDADE 2: Exceção libera a data
    if (blockedDayExceptionRepository.findByTenantIdAndExceptionDate(tenantId, date).isPresent()) {
        return false; // ✅ LIBERADO
    }

    // PRIORIDADE 3: Bloqueio recorrente
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    return blockedDayRepository.findByTenantIdAndDayOfWeekAndRecurring(tenantId, dayOfWeek, true).isPresent();
}
```

---

## 🧪 Casos de Teste

### Teste 1: Exceção Libera Dia Bloqueado Recorrente
```java
@Test
void testExceptionOverridesRecurringBlock() {
    // Setup: Bloquear todos os domingos
    blockedDayService.blockRecurringDayOfWeek(DayOfWeek.SUNDAY, "Folga");
    
    // Criar exceção para domingo 15/02/2026
    LocalDate sunday = LocalDate.of(2026, 2, 15);
    blockedDayService.createException(sunday, "Trabalho extra");
    
    // Assert: Domingo específico está liberado
    assertFalse(blockedDayService.isDateBlocked(sunday));
    
    // Assert: Outros domingos continuam bloqueados
    assertTrue(blockedDayService.isDateBlocked(LocalDate.of(2026, 2, 22)));
}
```

### Teste 2: Bloqueio Específico Tem Prioridade
```java
@Test
void testSpecificBlockHasPriority() {
    // Setup: Criar exceção
    LocalDate date = LocalDate.of(2026, 2, 15);
    blockedDayService.createException(date, "Trabalho extra");
    
    // Criar bloqueio específico para mesma data
    blockedDayService.blockSpecificDate(date, "Emergência");
    
    // Assert: Data está bloqueada (bloqueio específico vence)
    assertTrue(blockedDayService.isDateBlocked(date));
}
```

### Teste 3: Não Permite Exceção em Bloqueio Específico
```java
@Test
void testCannotCreateExceptionForSpecificBlock() {
    // Setup: Bloquear data específica
    LocalDate date = LocalDate.of(2026, 12, 25);
    blockedDayService.blockSpecificDate(date, "Natal");
    
    // Assert: Exceção lança erro
    assertThrows(DuplicateResourceException.class, () -> {
        blockedDayService.createException(date, "Trabalho extra");
    });
}
```

---

## 📊 Diagrama de Decisão

```
┌─────────────────────────────────────┐
│   Validar se data está bloqueada    │
└──────────────┬──────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│ Existe bloqueio específico?          │
│ (tb_blocked_days.specific_date)      │
└──────┬───────────────────────┬───────┘
       │ SIM                   │ NÃO
       ▼                       ▼
  ❌ BLOQUEADO    ┌──────────────────────────────┐
                  │ Existe exceção?              │
                  │ (tb_blocked_day_exceptions)  │
                  └──────┬────────────────┬──────┘
                         │ SIM            │ NÃO
                         ▼                ▼
                    ✅ LIBERADO  ┌────────────────────────┐
                                 │ Existe bloqueio        │
                                 │ recorrente?            │
                                 │ (day_of_week)          │
                                 └────┬────────────┬──────┘
                                      │ SIM        │ NÃO
                                      ▼            ▼
                                 ❌ BLOQUEADO  ✅ LIBERADO
```

---

## ✅ Checklist de Implementação

- [x] Criar entidade `BlockedDayExceptionEntity`
- [x] Criar repository `BlockedDayExceptionRepository`
- [x] Criar DTO `CreateBlockedDayExceptionRequest`
- [x] Atualizar `BlockedDayService.isDateBlocked()` com nova lógica
- [x] Adicionar métodos de gerenciamento de exceções no `BlockedDayService`
- [x] Criar controller `BlockedDayExceptionController`
- [x] Criar migration SQL `V5__create_blocked_day_exceptions_table.sql`
- [x] Documentar API e regras de negócio
- [ ] Executar testes unitários
- [ ] Executar testes de integração
- [ ] Testar cenários reais

---

## 🚀 Como Usar

### 1. Executar Migration

A migration será executada automaticamente pelo Flyway na próxima inicialização:

```bash
mvn spring-boot:run
```

### 2. Testar API

```bash
# 1. Bloquear todos os domingos
curl -X POST http://localhost:8080/blocked-days/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "dayOfWeek": "SUNDAY",
    "reason": "Folga semanal"
  }'

# 2. Criar exceção para domingo 15/02/2026
curl -X POST http://localhost:8080/blocked-days/exceptions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "exceptionDate": "2026-02-15",
    "reason": "Trabalho extra"
  }'

# 3. Verificar datas disponíveis
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-01&endDate=2026-02-28" \
  -H "X-Tenant-Id: kc"
```

---

## 📝 Notas Importantes

1. **Multi-Tenancy**: Todas as operações são isoladas por tenant via header `X-Tenant-Id`

2. **Prioridade**: Bloqueios específicos sempre têm prioridade sobre exceções

3. **Escopo**: Exceções só afetam bloqueios recorrentes, não específicos

4. **Validação**: Sistema impede conflitos e duplicações

5. **Retroatividade**: Exceções podem ser criadas para qualquer data (passada ou futura)

---

## 🔗 Referências

- `BlockedDayService.java` - Lógica de negócio principal
- `BlockedDayExceptionController.java` - Endpoints REST
- `V5__create_blocked_day_exceptions_table.sql` - Schema do banco

