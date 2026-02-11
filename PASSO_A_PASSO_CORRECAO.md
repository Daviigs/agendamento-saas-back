# 🎯 PASSO A PASSO - Corrigir e Executar

## 📌 Situação Atual

Você está com este erro:
```
ERRO: a coluna "horario_flexivel" da relação "tb_tenant_working_hours" contém valores nulos
```

---

## ✅ SOLUÇÃO EM 3 PASSOS

### 🔵 PASSO 1: Preparar o Banco de Dados

**Abra o terminal PowerShell:**
```powershell
# Conectar ao PostgreSQL
psql -U postgres -d agendamentodb
```

**Execute os comandos SQL:**
```sql
-- Remover a coluna problemática
ALTER TABLE tb_tenant_working_hours DROP COLUMN IF EXISTS horario_flexivel;

-- Limpar histórico do Flyway
DELETE FROM flyway_schema_history WHERE version = '4';

-- Verificar que foi removida
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'tb_tenant_working_hours' AND column_name = 'horario_flexivel';

-- Sair do psql
\q
```

**✅ Resultado Esperado:**
- A coluna `horario_flexivel` foi removida
- O histórico do Flyway para V4 foi limpo
- Pronto para reaplicar a migration

---

### 🟢 PASSO 2: Reiniciar a Aplicação

**No terminal PowerShell (na pasta do projeto):**
```powershell
# Navegar para a pasta do projeto
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"

# Reiniciar a aplicação
.\mvnw.cmd spring-boot:run
```

**✅ Aguarde ver nos logs:**
```
INFO: Flyway Community Edition 9.x.x
INFO: Successfully validated 4 migrations
INFO: Migrating schema "public" to version "4 - add horario flexivel column"
INFO: Successfully applied 1 migration
INFO: Started AgendamentoBackApplication in X.XXX seconds
```

**🎉 SE VIR ISSO, DEU CERTO!**

---

### 🟣 PASSO 3: Verificar que Funcionou

**Abra OUTRO terminal e teste:**

```powershell
# Teste 1: Verificar saúde da aplicação
curl http://localhost:8080/actuator/health

# Esperado: {"status":"UP"}
```

```powershell
# Teste 2: Consultar working hours
curl -X GET "http://localhost:8080/working-hours" -H "X-Tenant-Id: kc"

# Esperado: JSON com campo "horarioFlexivel": false ou true
```

**✅ Se ambos funcionaram, SUCESSO TOTAL!**

---

## 🎁 BÔNUS: Testar a Nova Funcionalidade

### Ativar Modo Flexível
```powershell
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" -H "X-Tenant-Id: kc"
```

### Consultar Configuração
```powershell
curl -X GET "http://localhost:8080/working-hours" -H "X-Tenant-Id: kc"
```

### Verificar Logs
Volte no terminal da aplicação e procure por:
```
INFO: Modo de horário: FLEXÍVEL (horarioFlexivel=true)
```

---

## 🔄 ALTERNATIVA: Script Automatizado

Se preferir fazer tudo automaticamente:

```powershell
# Executar script de correção
.\fix-horario-flexivel.ps1

# Seguir as instruções na tela
```

---

## 📊 Checklist Visual

### Antes de Começar
- [ ] PostgreSQL está rodando
- [ ] Você tem acesso ao banco `agendamentodb`
- [ ] Usuário `postgres` com senha `postgress`

### Durante o Processo
- [ ] Conectei ao banco via psql
- [ ] Executei os comandos SQL
- [ ] Vi confirmação da remoção
- [ ] Reiniciei a aplicação
- [ ] Vi logs de sucesso do Flyway

### Validação Final
- [ ] Aplicação iniciou sem erros
- [ ] Endpoint `/actuator/health` retorna UP
- [ ] Endpoint `/working-hours` funciona
- [ ] Campo `horarioFlexivel` aparece no JSON
- [ ] Consigo alterar com PATCH

---

## 🆘 Problemas?

### ❌ "psql: command not found"

**Solução:**
1. Encontre o PostgreSQL bin: `C:\Program Files\PostgreSQL\[versão]\bin`
2. Adicione ao PATH ou use caminho completo:
```powershell
& "C:\Program Files\PostgreSQL\15\bin\psql.exe" -U postgres -d agendamentodb
```

### ❌ "connection refused"

**Solução:**
1. Verifique se PostgreSQL está rodando:
```powershell
Get-Service -Name postgresql*
```

2. Inicie se necessário:
```powershell
Start-Service postgresql-x64-15  # ajuste o nome
```

### ❌ "authentication failed"

**Solução:**
Verifique a senha em `application-dev.properties`:
```properties
spring.datasource.password=postgress
```

### ❌ "Flyway failed to initialize"

**Solução:**
Verifique se a tabela existe:
```sql
SELECT * FROM information_schema.tables WHERE table_name = 'flyway_schema_history';
```

---

## 📚 Documentação Adicional

- **Solução Rápida:** `SOLUCAO_RAPIDA.md`
- **Guia Completo:** `SOLUCAO_ERRO_HORARIO_FLEXIVEL.md`
- **SQL Manual:** `FIX_HORARIO_FLEXIVEL.sql`
- **Script PowerShell:** `fix-horario-flexivel.ps1`
- **Feature Completa:** `FEATURE_HORARIO_FLEXIVEL.md`

---

## 🎯 Resumo Final

| Passo | Ação | Status |
|-------|------|--------|
| 1️⃣ | Limpar banco de dados | ⏳ Pendente |
| 2️⃣ | Reiniciar aplicação | ⏳ Pendente |
| 3️⃣ | Validar funcionamento | ⏳ Pendente |

**Tempo Total:** 5 minutos  
**Dificuldade:** Fácil  
**Impacto:** Zero (dados preservados)

---

## ✅ Sucesso!

Quando tudo estiver funcionando, você verá:

```
✅ Aplicação rodando sem erros
✅ Migration V4 aplicada
✅ Coluna horario_flexivel criada
✅ Endpoints respondendo
✅ Feature pronta para uso
```

**🎉 PARABÉNS! A funcionalidade Horário Flexível está funcionando!**

---

**Criado em:** 2026-02-10  
**Versão:** 1.0  
**Autor:** GitHub Copilot

