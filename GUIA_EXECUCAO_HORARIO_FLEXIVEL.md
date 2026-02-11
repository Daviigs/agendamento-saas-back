# 🚀 Guia Rápido - Executar Aplicação com Horário Flexível

## 📋 Pré-requisitos

- Java 17 ou superior
- Maven 3.6+
- PostgreSQL (ou banco de dados configurado)
- IDE (IntelliJ IDEA, Eclipse, VS Code, etc.)

## 🔧 Passos para Executar

### 1. Aplicar Migrations

As migrations do Flyway serão aplicadas automaticamente na inicialização da aplicação.

A nova migration `V4__add_horario_flexivel_column.sql` será executada e adicionará a coluna `horario_flexivel` na tabela `tb_tenant_working_hours`.

### 2. Compilar o Projeto

**Windows (PowerShell):**
```powershell
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw.cmd clean install -DskipTests
```

**Windows (CMD):**
```cmd
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
mvnw.cmd clean install -DskipTests
```

**Linux/Mac:**
```bash
cd "/path/to/lash-salao-kc-back"
./mvnw clean install -DskipTests
```

### 3. Executar a Aplicação

**Via Maven:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Via JAR:**
```powershell
java -jar target/agendamento-back-0.0.1-SNAPSHOT.jar
```

**Via IDE:**
- Abra a classe `AgendamentoBackApplication.java`
- Execute como Java Application (Run)

### 4. Verificar se Está Funcionando

```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP"
}
```

## 🧪 Testar a Nova Funcionalidade

### Teste Rápido 1: Criar Horário com Modo Flexível

```powershell
curl -X POST "http://localhost:8080/working-hours" `
  -H "Content-Type: application/json" `
  -H "X-Tenant-Id: kc" `
  -d '{
    \"startTime\": \"09:00:00\",
    \"endTime\": \"18:00:00\",
    \"slotIntervalMinutes\": 30,
    \"horarioFlexivel\": true
  }'
```

### Teste Rápido 2: Alternar Modo

```powershell
# Ativar modo flexível
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=true" `
  -H "X-Tenant-Id: kc"

# Desativar modo flexível (modo rígido)
curl -X PATCH "http://localhost:8080/working-hours/horario-flexivel?flexivel=false" `
  -H "X-Tenant-Id: kc"
```

### Teste Rápido 3: Consultar Configuração Atual

```powershell
curl -X GET "http://localhost:8080/working-hours" `
  -H "X-Tenant-Id: kc"
```

## 📁 Arquivos Modificados/Criados

### Novos Arquivos (Migrations):
- `src/main/resources/db/migration/V4__add_horario_flexivel_column.sql`

### Arquivos Modificados (Backend):
- `src/main/java/.../domain/entity/TenantWorkingHoursEntity.java`
- `src/main/java/.../domain/dto/TenantWorkingHoursRequest.java`
- `src/main/java/.../service/TenantWorkingHoursService.java`
- `src/main/java/.../service/AvailableTimeSlotsService.java`
- `src/main/java/.../controller/TenantWorkingHoursController.java`

### Documentação:
- `FEATURE_HORARIO_FLEXIVEL.md` - Documentação completa da feature
- `EXEMPLOS_HORARIO_FLEXIVEL.md` - Exemplos de uso da API
- `TESTES_HORARIO_FLEXIVEL.md` - Guia de testes
- `GUIA_EXECUCAO_HORARIO_FLEXIVEL.md` - Este guia

## 🔍 Verificar Migration no Banco

Após iniciar a aplicação, conecte-se ao banco de dados e execute:

```sql
-- Verificar se a coluna foi criada
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'tb_tenant_working_hours'
  AND column_name = 'horario_flexivel';

-- Verificar histórico de migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- Verificar dados atuais
SELECT tenant_id, start_time, end_time, horario_flexivel, active
FROM tb_tenant_working_hours;
```

## ⚠️ Troubleshooting

### Erro: "Column horario_flexivel does not exist"

**Causa:** Migration não foi executada.

**Solução:**
1. Verifique os logs da aplicação para erros do Flyway
2. Confirme que o arquivo `V4__add_horario_flexivel_column.sql` está na pasta `src/main/resources/db/migration/`
3. Verifique a tabela `flyway_schema_history` no banco
4. Se necessário, execute a migration manualmente:
```sql
ALTER TABLE tb_tenant_working_hours 
ADD COLUMN horario_flexivel BOOLEAN NOT NULL DEFAULT false;
```

### Erro: "Cannot resolve method getHorarioFlexivel"

**Causa:** IDE não recompilou as classes Lombok.

**Solução:**
1. Limpe e recompile o projeto: `mvnw clean compile`
2. No IntelliJ IDEA: File > Invalidate Caches > Invalidate and Restart
3. Verifique se o plugin Lombok está instalado na IDE

### Erro de Compilação

**Solução:**
```powershell
# Limpar completamente e recompilar
.\mvnw.cmd clean install -U -DskipTests

# Se persistir, deletar pasta target
Remove-Item -Recurse -Force target
.\mvnw.cmd clean install -DskipTests
```

## 📊 Logs Importantes

Ao iniciar a aplicação, procure por estas mensagens nos logs:

```
✅ Sucesso na Migration:
INFO: Successfully applied 1 migration to schema "public" (execution time 00:00.234s)
INFO: Flyway migration V4__add_horario_flexivel_column executed successfully

✅ Aplicação Iniciada:
INFO: Started AgendamentoBackApplication in 8.456 seconds

✅ Feature Funcionando:
INFO: Modo de horário do profissional XXX: FLEXÍVEL (horarioFlexivel=true)
```

## 🎓 Próximos Passos

1. Execute os testes do guia `TESTES_HORARIO_FLEXIVEL.md`
2. Configure seus tenants com o modo adequado (flexível ou rígido)
3. Teste com dados reais em ambiente de desenvolvimento
4. Consulte `EXEMPLOS_HORARIO_FLEXIVEL.md` para casos de uso

## 📞 Referências

- **Documentação Completa**: `FEATURE_HORARIO_FLEXIVEL.md`
- **Exemplos de API**: `EXEMPLOS_HORARIO_FLEXIVEL.md`
- **Guia de Testes**: `TESTES_HORARIO_FLEXIVEL.md`

## ✅ Checklist de Validação Rápida

Após iniciar a aplicação, verifique:

- [ ] Aplicação iniciou sem erros
- [ ] Migration V4 foi aplicada (verificar logs ou banco)
- [ ] Endpoint GET /working-hours retorna o campo `horarioFlexivel`
- [ ] Endpoint PATCH /working-hours/horario-flexivel funciona
- [ ] Logs mostram "Modo de horário: FLEXÍVEL" ou "RÍGIDO"

---

**Data de Criação**: 2026-02-10  
**Versão**: 1.0  
**Feature**: Horário Flexível (V4)

