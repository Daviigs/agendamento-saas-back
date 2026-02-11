# 🚨 SOLUÇÃO RÁPIDA - Erro de Migration

## ❌ Problema
```
ERRO: a coluna "horario_flexivel" da relação "tb_tenant_working_hours" contém valores nulos
```

## ✅ Solução Rápida (3 Passos)

### 1️⃣ Corrigir Banco de Dados

Conecte ao PostgreSQL e execute:

```sql
-- Remover coluna problemática
ALTER TABLE tb_tenant_working_hours DROP COLUMN IF EXISTS horario_flexivel;

-- Limpar histórico do Flyway
DELETE FROM flyway_schema_history WHERE version = '4';
```

**OU use o script automatizado:**
```powershell
.\fix-horario-flexivel.ps1
```

---

### 2️⃣ Reiniciar Aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

---

### 3️⃣ Verificar Sucesso

**Logs esperados:**
```
INFO: Successfully validated 4 migrations
INFO: Schema "public" is up to date
INFO: Started AgendamentoBackApplication
```

**Testar:**
```powershell
curl http://localhost:8080/actuator/health
```

---

## 📋 O Que Foi Corrigido

### Antes ❌
```properties
spring.jpa.hibernate.ddl-auto=update  # Hibernate gerenciava schema
```

### Depois ✅
```properties
spring.jpa.hibernate.ddl-auto=validate  # Flyway gerencia schema
spring.flyway.enabled=true
```

### Migration V4 Melhorada ✅
```sql
-- Agora em 4 passos seguros:
1. ADD COLUMN (nullable)
2. UPDATE valores NULL → false  
3. ALTER COLUMN SET NOT NULL
4. ALTER COLUMN SET DEFAULT false
```

---

## 🆘 Se Ainda Não Funcionar

### Opção 1: Aplicar Manualmente
```sql
-- Se a coluna já existe com NULL
UPDATE tb_tenant_working_hours SET horario_flexivel = false WHERE horario_flexivel IS NULL;
ALTER TABLE tb_tenant_working_hours ALTER COLUMN horario_flexivel SET NOT NULL;
ALTER TABLE tb_tenant_working_hours ALTER COLUMN horario_flexivel SET DEFAULT false;
```

### Opção 2: Consultar Documentação Completa
📖 Veja: `SOLUCAO_ERRO_HORARIO_FLEXIVEL.md`

---

## ✅ Validação Final

```sql
-- Verificar estrutura
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'tb_tenant_working_hours' AND column_name = 'horario_flexivel';

-- Esperado:
-- is_nullable: NO
-- column_default: false
```

---

## 🎯 Resultado

✅ Aplicação iniciando sem erros  
✅ Migration V4 aplicada  
✅ Coluna `horario_flexivel` criada corretamente  
✅ Endpoints funcionando  

---

**Tempo estimado:** 2-3 minutos  
**Complexidade:** Baixa  
**Impacto:** Zero (dados preservados)

