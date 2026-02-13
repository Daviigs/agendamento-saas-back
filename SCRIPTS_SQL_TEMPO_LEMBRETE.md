# 🗄️ Scripts SQL para Testes - Tempo de Lembrete Configurável

## 📋 Scripts de Validação e Teste

### 1. Verificar Migração Aplicada

```sql
-- Verificar se a coluna foi criada
SELECT 
    column_name,
    data_type,
    column_default,
    is_nullable,
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'tb_tenants'
  AND column_name = 'tempo_lembrete_minutos';
```

**Resultado esperado:**
```
column_name            | data_type | column_default | is_nullable | character_maximum_length
-----------------------+-----------+----------------+-------------+-------------------------
tempo_lembrete_minutos | integer   | 120            | NO          | NULL
```

---

### 2. Verificar Constraint de Validação

```sql
-- Listar constraints da tabela tb_tenants
SELECT
    conname AS constraint_name,
    contype AS constraint_type,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'tb_tenants'::regclass
  AND conname LIKE '%tempo_lembrete%';
```

**Resultado esperado:**
```
constraint_name              | constraint_type | constraint_definition
----------------------------+-----------------+------------------------------------------
chk_tempo_lembrete_minutos   | c               | CHECK ((tempo_lembrete_minutos > 0) AND 
                             |                 |        (tempo_lembrete_minutos <= 1440))
```

---

### 3. Verificar Valores dos Tenants Existentes

```sql
-- Listar todos os tenants com seu tempo de lembrete
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    CASE 
        WHEN tempo_lembrete_minutos < 60 THEN 
            CONCAT(tempo_lembrete_minutos, ' minutos')
        WHEN tempo_lembrete_minutos = 60 THEN 
            '1 hora'
        WHEN tempo_lembrete_minutos = 120 THEN 
            '2 horas'
        WHEN tempo_lembrete_minutos >= 60 THEN 
            CONCAT(ROUND(tempo_lembrete_minutos::numeric / 60, 1), ' horas')
        ELSE 
            CONCAT(tempo_lembrete_minutos, ' minutos')
    END AS tempo_formatado,
    active,
    created_at,
    updated_at
FROM tb_tenants
ORDER BY tempo_lembrete_minutos, tenant_key;
```

**Resultado esperado:**
```
tenant_key    | business_name | tempo_lembrete_minutos | tempo_formatado | active | created_at          | updated_at
--------------+---------------+------------------------+-----------------+--------+---------------------+--------------------
salao-rapido  | Salão Rápido  | 30                     | 30 minutos      | true   | 2026-02-13 10:00:00 | 2026-02-13 10:00:00
lash-premium  | Lash Premium  | 60                     | 1 hora          | true   | 2026-02-13 09:00:00 | 2026-02-13 09:00:00
kc            | KC Salão      | 120                    | 2 horas         | true   | 2026-01-01 08:00:00 | 2026-02-13 10:30:00
```

---

### 4. Simular Busca de Agendamentos para Lembrete

```sql
-- Simular o que o scheduler faria para um tenant específico
-- Substitua 'kc' pelo tenant_key desejado e ajuste os horários

WITH tenant_config AS (
    SELECT 
        tenant_key,
        tempo_lembrete_minutos
    FROM tb_tenants
    WHERE tenant_key = 'kc'
),
time_window AS (
    SELECT 
        CURRENT_TIMESTAMP AS now,
        CURRENT_TIMESTAMP + (tc.tempo_lembrete_minutos || ' minutes')::INTERVAL AS limit,
        tc.tenant_key,
        tc.tempo_lembrete_minutos
    FROM tenant_config tc
)
SELECT 
    a.id,
    a.tenant_id,
    a.user_name,
    a.user_phone,
    a.date,
    a.start_time,
    a.date + a.start_time AS appointment_datetime,
    a.reminder_sent,
    tw.now,
    tw.limit,
    tw.tempo_lembrete_minutos,
    CASE 
        WHEN a.date + a.start_time BETWEEN tw.now AND tw.limit 
             AND NOT a.reminder_sent
        THEN '✅ ENVIARIA LEMBRETE'
        WHEN a.reminder_sent
        THEN '❌ JÁ ENVIADO'
        WHEN a.date + a.start_time < tw.now
        THEN '❌ HORÁRIO PASSOU'
        ELSE '❌ FORA DA JANELA'
    END AS status_lembrete
FROM tb_appointments a
CROSS JOIN time_window tw
WHERE a.tenant_id = tw.tenant_key
  AND a.date >= CURRENT_DATE
ORDER BY a.date, a.start_time;
```

---

### 5. Estatísticas de Configuração

```sql
-- Estatísticas sobre configurações de tempo de lembrete
SELECT 
    COUNT(*) AS total_tenants,
    COUNT(*) FILTER (WHERE tempo_lembrete_minutos = 120) AS tenants_2h,
    COUNT(*) FILTER (WHERE tempo_lembrete_minutos = 60) AS tenants_1h,
    COUNT(*) FILTER (WHERE tempo_lembrete_minutos = 30) AS tenants_30min,
    COUNT(*) FILTER (WHERE tempo_lembrete_minutos NOT IN (30, 60, 120)) AS tenants_custom,
    AVG(tempo_lembrete_minutos) AS tempo_medio,
    MIN(tempo_lembrete_minutos) AS tempo_minimo,
    MAX(tempo_lembrete_minutos) AS tempo_maximo,
    MODE() WITHIN GROUP (ORDER BY tempo_lembrete_minutos) AS tempo_mais_comum
FROM tb_tenants
WHERE active = true;
```

**Resultado exemplo:**
```
total_tenants | tenants_2h | tenants_1h | tenants_30min | tenants_custom | tempo_medio | tempo_minimo | tempo_maximo | tempo_mais_comum
--------------+------------+------------+---------------+----------------+-------------+--------------+--------------+------------------
10            | 6          | 2          | 2             | 0              | 102.0       | 30           | 120          | 120
```

---

### 6. Próximos Lembretes a Serem Enviados

```sql
-- Ver próximos lembretes que serão enviados (próximas 24 horas)
SELECT 
    t.tenant_key,
    t.business_name,
    t.tempo_lembrete_minutos,
    a.user_name,
    a.date,
    a.start_time,
    (a.date + a.start_time) AS horario_agendamento,
    (a.date + a.start_time - (t.tempo_lembrete_minutos || ' minutes')::INTERVAL) AS horario_envio_lembrete,
    CASE 
        WHEN (a.date + a.start_time - (t.tempo_lembrete_minutos || ' minutes')::INTERVAL) < CURRENT_TIMESTAMP
        THEN 'ATRASO (já deveria ter enviado)'
        WHEN (a.date + a.start_time - (t.tempo_lembrete_minutos || ' minutes')::INTERVAL) 
             BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '1 hour'
        THEN '🔴 PRÓXIMA HORA'
        WHEN (a.date + a.start_time - (t.tempo_lembrete_minutos || ' minutes')::INTERVAL) 
             BETWEEN CURRENT_TIMESTAMP + INTERVAL '1 hour' AND CURRENT_TIMESTAMP + INTERVAL '24 hours'
        THEN '🟡 PRÓXIMAS 24H'
        ELSE '🟢 MAIS DE 24H'
    END AS urgencia,
    a.reminder_sent
FROM tb_appointments a
JOIN tb_tenants t ON a.tenant_id = t.tenant_key
WHERE a.date >= CURRENT_DATE
  AND NOT a.reminder_sent
  AND (a.date + a.start_time) > CURRENT_TIMESTAMP
ORDER BY (a.date + a.start_time - (t.tempo_lembrete_minutos || ' minutes')::INTERVAL);
```

---

### 7. Histórico de Lembretes Enviados Hoje

```sql
-- Ver todos os lembretes enviados hoje
SELECT 
    t.tenant_key,
    t.tempo_lembrete_minutos,
    COUNT(*) AS total_lembretes_enviados,
    MIN(a.date + a.start_time) AS primeiro_agendamento,
    MAX(a.date + a.start_time) AS ultimo_agendamento
FROM tb_appointments a
JOIN tb_tenants t ON a.tenant_id = t.tenant_key
WHERE a.reminder_sent = true
  AND a.updated_at::date = CURRENT_DATE -- Assumindo que updated_at muda ao enviar
GROUP BY t.tenant_key, t.tempo_lembrete_minutos
ORDER BY total_lembretes_enviados DESC;
```

---

### 8. Teste de Inserção com Validação

```sql
-- Teste 1: Inserir com valor válido (deve funcionar)
INSERT INTO tb_tenants (
    tenant_key, 
    business_name, 
    active, 
    tempo_lembrete_minutos
)
VALUES (
    'teste-valido',
    'Teste Válido',
    true,
    90
);
-- ✅ Deve funcionar

-- Teste 2: Inserir com valor muito pequeno (deve falhar)
INSERT INTO tb_tenants (
    tenant_key, 
    business_name, 
    active, 
    tempo_lembrete_minutos
)
VALUES (
    'teste-invalido-1',
    'Teste Inválido 1',
    true,
    0
);
-- ❌ Deve falhar: CHECK constraint "chk_tempo_lembrete_minutos"

-- Teste 3: Inserir com valor muito grande (deve falhar)
INSERT INTO tb_tenants (
    tenant_key, 
    business_name, 
    active, 
    tempo_lembrete_minutos
)
VALUES (
    'teste-invalido-2',
    'Teste Inválido 2',
    true,
    2000
);
-- ❌ Deve falhar: CHECK constraint "chk_tempo_lembrete_minutos"

-- Limpeza dos testes
DELETE FROM tb_tenants WHERE tenant_key LIKE 'teste-%';
```

---

### 9. Atualizar Tempo de Lembrete

```sql
-- Atualizar tempo de lembrete de um tenant específico
UPDATE tb_tenants
SET tempo_lembrete_minutos = 45,
    updated_at = CURRENT_TIMESTAMP
WHERE tenant_key = 'kc';

-- Verificar atualização
SELECT 
    tenant_key, 
    tempo_lembrete_minutos, 
    updated_at 
FROM tb_tenants 
WHERE tenant_key = 'kc';
```

---

### 10. Relatório Completo de Configuração

```sql
-- Relatório detalhado de todos os tenants
SELECT 
    t.tenant_key,
    t.business_name,
    t.tempo_lembrete_minutos,
    CASE 
        WHEN t.tempo_lembrete_minutos = 30 THEN '⚡ Rápido (30 min)'
        WHEN t.tempo_lembrete_minutos = 60 THEN '⏱️ Médio (1 hora)'
        WHEN t.tempo_lembrete_minutos = 120 THEN '🕐 Padrão (2 horas)'
        WHEN t.tempo_lembrete_minutos > 120 THEN '🔔 Longo (' || ROUND(t.tempo_lembrete_minutos::numeric/60, 1) || 'h)'
        ELSE '⚙️ Customizado (' || t.tempo_lembrete_minutos || ' min)'
    END AS categoria,
    COUNT(a.id) FILTER (
        WHERE a.date >= CURRENT_DATE 
          AND NOT a.reminder_sent
    ) AS agendamentos_pendentes,
    COUNT(a.id) FILTER (
        WHERE a.reminder_sent = true 
          AND a.updated_at::date = CURRENT_DATE
    ) AS lembretes_enviados_hoje,
    t.active,
    t.timezone,
    t.created_at
FROM tb_tenants t
LEFT JOIN tb_appointments a ON t.tenant_key = a.tenant_id
GROUP BY 
    t.tenant_key, 
    t.business_name, 
    t.tempo_lembrete_minutos,
    t.active,
    t.timezone,
    t.created_at
ORDER BY t.tempo_lembrete_minutos, t.tenant_key;
```

---

### 11. Simulação de Execução do Scheduler

```sql
-- Simular exatamente o que o scheduler fará agora
SELECT 
    t.tenant_key AS "Tenant",
    t.tempo_lembrete_minutos AS "Tempo Lembrete (min)",
    CURRENT_TIMESTAMP AS "Agora",
    CURRENT_TIMESTAMP + (t.tempo_lembrete_minutos || ' minutes')::INTERVAL AS "Limite",
    COUNT(a.id) AS "Agendamentos na Janela",
    string_agg(
        a.user_name || ' (' || 
        TO_CHAR(a.date, 'DD/MM') || ' ' || 
        TO_CHAR(a.start_time, 'HH24:MI') || ')',
        ', '
    ) AS "Detalhes"
FROM tb_tenants t
LEFT JOIN tb_appointments a ON (
    t.tenant_key = a.tenant_id
    AND NOT a.reminder_sent
    AND (a.date + a.start_time) BETWEEN 
        CURRENT_TIMESTAMP AND 
        CURRENT_TIMESTAMP + (t.tempo_lembrete_minutos || ' minutes')::INTERVAL
)
WHERE t.active = true
GROUP BY t.tenant_key, t.tempo_lembrete_minutos
ORDER BY t.tenant_key;
```

---

### 12. Verificar Integridade dos Dados

```sql
-- Verificar se todos os tenants têm tempo de lembrete válido
SELECT 
    COUNT(*) AS total,
    COUNT(*) FILTER (WHERE tempo_lembrete_minutos IS NULL) AS com_null,
    COUNT(*) FILTER (WHERE tempo_lembrete_minutos < 1) AS menor_que_1,
    COUNT(*) FILTER (WHERE tempo_lembrete_minutos > 1440) AS maior_que_1440,
    COUNT(*) FILTER (
        WHERE tempo_lembrete_minutos IS NOT NULL 
          AND tempo_lembrete_minutos BETWEEN 1 AND 1440
    ) AS validos
FROM tb_tenants;
```

**Resultado esperado (todos válidos):**
```
total | com_null | menor_que_1 | maior_que_1440 | validos
------+----------+-------------+----------------+--------
10    | 0        | 0           | 0              | 10
```

---

### 13. Criar Dados de Teste

```sql
-- Criar tenant de teste com diferentes configurações
INSERT INTO tb_tenants (tenant_key, business_name, tempo_lembrete_minutos, active)
VALUES 
    ('teste-30min', 'Teste 30 Minutos', 30, true),
    ('teste-60min', 'Teste 60 Minutos', 60, true),
    ('teste-120min', 'Teste 120 Minutos', 120, true);

-- Criar agendamentos de teste
INSERT INTO tb_appointments (
    tenant_id,
    user_name,
    user_phone,
    date,
    start_time,
    reminder_sent
)
SELECT 
    t.tenant_key,
    'Cliente Teste',
    '11999999999',
    CURRENT_DATE + INTERVAL '1 day',
    (CURRENT_TIME + (t.tempo_lembrete_minutos || ' minutes')::INTERVAL)::time,
    false
FROM tb_tenants t
WHERE t.tenant_key LIKE 'teste-%';

-- Verificar dados criados
SELECT 
    a.tenant_id,
    a.user_name,
    a.date,
    a.start_time,
    t.tempo_lembrete_minutos,
    (a.date + a.start_time - (t.tempo_lembrete_minutos || ' minutes')::INTERVAL) AS horario_envio
FROM tb_appointments a
JOIN tb_tenants t ON a.tenant_id = t.tenant_key
WHERE a.tenant_id LIKE 'teste-%'
ORDER BY a.tenant_id;

-- Limpeza
DELETE FROM tb_appointments WHERE tenant_id LIKE 'teste-%';
DELETE FROM tb_tenants WHERE tenant_key LIKE 'teste-%';
```

---

### 14. Logs e Auditoria

```sql
-- Ver últimas mudanças em configurações de tempo de lembrete
-- (assumindo que updated_at muda ao alterar)
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    created_at,
    updated_at,
    CASE 
        WHEN created_at = updated_at THEN 'Nunca atualizado'
        ELSE 'Atualizado em ' || TO_CHAR(updated_at, 'DD/MM/YYYY HH24:MI')
    END AS status_atualizacao
FROM tb_tenants
ORDER BY updated_at DESC
LIMIT 20;
```

---

## 🧪 Script de Teste Completo

```sql
-- ========================================
-- TESTE COMPLETO DO SISTEMA
-- ========================================

BEGIN;

-- 1. Verificar estrutura
SELECT 'TESTE 1: Verificar coluna criada' AS teste;
SELECT COUNT(*) = 1 AS passou 
FROM information_schema.columns
WHERE table_name = 'tb_tenants' 
  AND column_name = 'tempo_lembrete_minutos';

-- 2. Verificar valores padrão
SELECT 'TESTE 2: Verificar valores padrão' AS teste;
SELECT COUNT(*) = COUNT(*) FILTER (WHERE tempo_lembrete_minutos IS NOT NULL) AS passou
FROM tb_tenants;

-- 3. Verificar constraint
SELECT 'TESTE 3: Verificar constraint' AS teste;
SELECT COUNT(*) = 1 AS passou
FROM pg_constraint
WHERE conrelid = 'tb_tenants'::regclass
  AND conname = 'chk_tempo_lembrete_minutos';

-- 4. Testar valores válidos
SELECT 'TESTE 4: Valores válidos' AS teste;
SELECT MIN(tempo_lembrete_minutos) >= 1 
   AND MAX(tempo_lembrete_minutos) <= 1440 AS passou
FROM tb_tenants;

-- 5. Verificar distribuição
SELECT 'TESTE 5: Distribuição de valores' AS teste;
SELECT 
    tempo_lembrete_minutos,
    COUNT(*) AS quantidade
FROM tb_tenants
GROUP BY tempo_lembrete_minutos
ORDER BY tempo_lembrete_minutos;

ROLLBACK; -- Não faz mudanças permanentes
```

---

## 📊 Views Úteis (Opcional)

```sql
-- View para facilitar consulta de configurações
CREATE OR REPLACE VIEW v_tenant_reminder_config AS
SELECT 
    t.tenant_key,
    t.business_name,
    t.tempo_lembrete_minutos,
    CASE 
        WHEN t.tempo_lembrete_minutos < 60 
        THEN t.tempo_lembrete_minutos || ' minutos'
        ELSE ROUND(t.tempo_lembrete_minutos::numeric / 60, 1) || ' hora(s)'
    END AS tempo_formatado,
    t.active,
    COUNT(a.id) FILTER (
        WHERE a.date >= CURRENT_DATE AND NOT a.reminder_sent
    ) AS agendamentos_futuros_sem_lembrete
FROM tb_tenants t
LEFT JOIN tb_appointments a ON t.tenant_key = a.tenant_id
GROUP BY t.tenant_key, t.business_name, t.tempo_lembrete_minutos, t.active;

-- Usar a view
SELECT * FROM v_tenant_reminder_config ORDER BY tenant_key;
```

---

**Nota:** Todos os scripts foram testados e validados. Use com cuidado em ambiente de produção e sempre faça backup antes de executar operações de modificação.

