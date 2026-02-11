# 🔧 Guia de Correção - Erro de Migration Horário Flexível

## 🚨 Erro Encontrado

```
ERRO: a coluna "horario_flexivel" da relação "tb_tenant_working_hours" contém valores nulos
```

## 🔍 Causa do Problema

O Hibernate estava configurado com `ddl-auto=update` e tentou criar a coluna antes do Flyway, resultando em:
1. Coluna criada com valores NULL
2. Tentativa de adicionar constraint NOT NULL falhou

## ✅ Solução Implementada

### 1. Configurações Corrigidas

**Antes:**
```properties
spring.jpa.hibernate.ddl-auto=update  ❌
```

**Depois:**
```properties
spring.jpa.hibernate.ddl-auto=validate  ✅
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

### 2. Migration V4 Melhorada

A migration agora:
1. ✅ Adiciona coluna como NULLABLE primeiro
2. ✅ Atualiza valores NULL existentes para FALSE
3. ✅ Adiciona constraint NOT NULL depois
4. ✅ Define DEFAULT false para novos registros

## 🚀 Passos para Corrigir

### Opção 1: Limpar e Reaplicar (Recomendado)

**1. Conectar ao banco de dados:**
```bash
psql -U postgres -d agendamentodb
```

**2. Executar script de correção:**
```sql
-- Verificar estado atual
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'tb_tenant_working_hours'
  AND column_name = 'horario_flexivel';

-- Remover coluna problemática
ALTER TABLE tb_tenant_working_hours 
DROP COLUMN IF EXISTS horario_flexivel;

-- Remover migration do histórico do Flyway (se necessário)
DELETE FROM flyway_schema_history WHERE version = '4';
```

**3. Reiniciar a aplicação:**
```powershell
.\mvnw.cmd spring-boot:run
```

O Flyway aplicará corretamente a migration V4.

---

### Opção 2: Aplicar Manualmente

Se a coluna já existe com valores NULL:

```sql
-- Atualizar valores NULL para FALSE
UPDATE tb_tenant_working_hours 
SET horario_flexivel = false 
WHERE horario_flexivel IS NULL;

-- Adicionar constraint NOT NULL
ALTER TABLE tb_tenant_working_hours 
ALTER COLUMN horario_flexivel SET NOT NULL;

-- Adicionar valor padrão
ALTER TABLE tb_tenant_working_hours 
ALTER COLUMN horario_flexivel SET DEFAULT false;

-- Registrar migration como aplicada
INSERT INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES 
(
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
    '4',
    'add horario flexivel column',
    'SQL',
    'V4__add_horario_flexivel_column.sql',
    NULL,
    'postgres',
    NOW(),
    0,
    true
);
```

---

## 🧪 Verificar se Está Funcionando

### 1. Verificar a coluna no banco:
```sql
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns
WHERE table_name = 'tb_tenant_working_hours'
  AND column_name = 'horario_flexivel';
```

**Resultado esperado:**
```
column_name      | horario_flexivel
data_type        | boolean
is_nullable      | NO
column_default   | false
```

### 2. Verificar dados existentes:
```sql
SELECT 
    tenant_id, 
    start_time, 
    end_time, 
    horario_flexivel, 
    active
FROM tb_tenant_working_hours;
```

**Resultado esperado:** Todos os registros com `horario_flexivel = false`

### 3. Verificar histórico do Flyway:
```sql
SELECT 
    installed_rank, 
    version, 
    description, 
    success, 
    installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

**Resultado esperado:** V4 aparece com `success = true`

### 4. Testar a aplicação:
```powershell
# Verificar saúde
curl http://localhost:8080/actuator/health

# Consultar working hours
curl -X GET "http://localhost:8080/working-hours" -H "X-Tenant-Id: kc"
```

---

## 📋 Checklist de Correção

- [ ] Banco de dados acessível
- [ ] Coluna `horario_flexivel` removida ou corrigida
- [ ] Migration V4 removida do histórico (se necessário)
- [ ] Configuração `hibernate.ddl-auto` alterada para `validate`
- [ ] Flyway habilitado nas configurações
- [ ] Aplicação reiniciada
- [ ] Migration V4 aplicada com sucesso
- [ ] Coluna criada corretamente (NOT NULL, DEFAULT false)
- [ ] Dados existentes com `horario_flexivel = false`
- [ ] Endpoints funcionando

---

## ⚠️ Prevenindo Problemas Futuros

### 1. Sempre use Flyway para migrations
```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate  ✅ Use validate
spring.flyway.enabled=true              ✅ Ative o Flyway
```

### 2. Teste migrations em ordem
```sql
-- Sempre teste suas migrations antes de aplicar em produção
-- 1. Criar coluna NULLABLE
-- 2. Popular com valores padrão
-- 3. Adicionar constraint NOT NULL
```

### 3. Verifique o histórico do Flyway
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
```

---

## 🆘 Erros Comuns e Soluções

### Erro: "Flyway failed to initialize"
**Solução:** Verifique se a tabela `flyway_schema_history` existe
```sql
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);
```

### Erro: "Column already exists"
**Solução:** Use `ADD COLUMN IF NOT EXISTS` nas migrations

### Erro: "Validation failed"
**Solução:** Verifique se o schema do banco corresponde às entidades JPA

---

## 📞 Referências

- **Migration Corrigida:** `V4__add_horario_flexivel_column.sql`
- **Script de Correção:** `FIX_HORARIO_FLEXIVEL.sql`
- **Configurações:** `application.properties` e `application-dev.properties`
- **Documentação:** `FEATURE_HORARIO_FLEXIVEL.md`

---

## ✅ Confirmação de Sucesso

Após seguir os passos, você deve ver nos logs:

```
INFO: Flyway Community Edition 9.x.x
INFO: Database: jdbc:postgresql://localhost:5432/agendamentodb
INFO: Successfully validated 4 migrations
INFO: Current version of schema "public": 4
INFO: Schema "public" is up to date. No migration necessary.
INFO: Started AgendamentoBackApplication in X.XXX seconds
```

🎉 **Problema resolvido!**

---

**Data de Criação:** 2026-02-10  
**Versão:** 1.0  
**Tipo:** Guia de Correção

