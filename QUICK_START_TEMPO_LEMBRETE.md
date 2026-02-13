# 🚀 Guia Rápido: Testar Tempo de Lembrete Configurável

## ⚡ Início Rápido

### 1. Aplicar Migration

Execute no terminal:
```powershell
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
.\mvnw clean install -DskipTests
.\mvnw flyway:migrate
```

Ou diretamente no Maven:
```powershell
mvn clean install -DskipTests
mvn flyway:migrate
```

### 2. Verificar Migration

Conecte ao banco de dados e verifique:
```sql
-- Verificar que a coluna foi adicionada
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'tb_tenants' 
  AND column_name = 'tempo_lembrete_minutos';

-- Verificar valores dos tenants existentes
SELECT tenant_key, business_name, tempo_lembrete_minutos, active 
FROM tb_tenants;
```

Resultado esperado:
```
tenant_key    | business_name | tempo_lembrete_minutos | active
--------------+---------------+------------------------+--------
kc            | KC Salão      | 120                    | true
salao-teste   | Salão Teste   | 120                    | true
```

### 3. Iniciar Aplicação

```powershell
.\mvnw spring-boot:run
```

Ou:
```powershell
mvn spring-boot:run
```

### 4. Testar Endpoints

#### Teste 1: Criar Tenant com tempo personalizado (60 minutos)

```bash
curl -X POST http://localhost:8080/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantKey": "salao-rapido",
    "businessName": "Salão Rápido",
    "contactEmail": "contato@salaorapido.com",
    "contactPhone": "11999999999",
    "tempoLembreteMinutos": 60
  }'
```

**PowerShell:**
```powershell
$body = @{
    tenantKey = "salao-rapido"
    businessName = "Salão Rápido"
    contactEmail = "contato@salaorapido.com"
    contactPhone = "11999999999"
    tempoLembreteMinutos = 60
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/tenants" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

#### Teste 2: Criar Tenant sem especificar tempo (usa padrão 120)

```bash
curl -X POST http://localhost:8080/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantKey": "salao-padrao",
    "businessName": "Salão Padrão",
    "contactEmail": "contato@salaopadrao.com",
    "contactPhone": "11988888888"
  }'
```

**PowerShell:**
```powershell
$body = @{
    tenantKey = "salao-padrao"
    businessName = "Salão Padrão"
    contactEmail = "contato@salaopadrao.com"
    contactPhone = "11988888888"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/tenants" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

#### Teste 3: Consultar Tenant e verificar tempo de lembrete

```bash
curl http://localhost:8080/tenants/{tenant-id}
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/tenants/{tenant-id}" -Method GET
```

Resposta esperada:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "tenantKey": "salao-rapido",
  "businessName": "Salão Rápido",
  "contactEmail": "contato@salaorapido.com",
  "contactPhone": "11999999999",
  "active": true,
  "timezone": "America/Sao_Paulo",
  "tempoLembreteMinutos": 60,
  "createdAt": "2026-02-13T10:30:00",
  "updatedAt": "2026-02-13T10:30:00"
}
```

#### Teste 4: Atualizar tempo de lembrete de tenant existente

```bash
curl -X PUT http://localhost:8080/tenants/{tenant-id} \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Salão Rápido Atualizado",
    "contactEmail": "novo@salaorapido.com",
    "contactPhone": "11977777777",
    "tempoLembreteMinutos": 30
  }'
```

**PowerShell:**
```powershell
$body = @{
    businessName = "Salão Rápido Atualizado"
    contactEmail = "novo@salaorapido.com"
    contactPhone = "11977777777"
    tempoLembreteMinutos = 30
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/tenants/{tenant-id}" `
  -Method PUT `
  -Body $body `
  -ContentType "application/json"
```

#### Teste 5: Listar todos os tenants

```bash
curl http://localhost:8080/tenants
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/tenants" -Method GET
```

---

## 🧪 Testar o Scheduler

### Cenário de Teste Completo

#### 1. Criar agendamento para daqui a 1 hora e 30 minutos

Primeiro, obtenha o ID de um profissional e serviço:

```bash
# Listar profissionais
curl http://localhost:8080/professionals/active \
  -H "X-Tenant-Id: salao-rapido"

# Listar serviços
curl http://localhost:8080/services \
  -H "X-Tenant-Id: salao-rapido"
```

Depois, criar o agendamento:

```bash
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: salao-rapido" \
  -d '{
    "date": "2026-02-13",
    "startTime": "14:30",
    "userName": "Cliente Teste",
    "userPhone": "11999999999",
    "professionalId": "{professional-id}",
    "serviceIds": ["{service-id}"]
  }'
```

**PowerShell:**
```powershell
$dataHora = (Get-Date).AddMinutes(90)
$data = $dataHora.ToString("yyyy-MM-dd")
$hora = $dataHora.ToString("HH:mm")

$body = @{
    date = $data
    startTime = $hora
    userName = "Cliente Teste"
    userPhone = "11999999999"
    professionalId = "{professional-id}"
    serviceIds = @("{service-id}")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/appointments" `
  -Method POST `
  -Headers @{"X-Tenant-Id"="salao-rapido"} `
  -Body $body `
  -ContentType "application/json"
```

#### 2. Verificar logs do scheduler

Aguarde alguns minutos e verifique os logs da aplicação:

```log
🔔 Iniciando verificação de lembretes...
👥 Tenants ativos: [salao-rapido, salao-padrao, kc]

📋 Tenant 'salao-rapido': buscando agendamentos entre 13/02/2026 13:00 e 13/02/2026 14:00 (60 minutos de antecedência)
📋 Tenant 'salao-rapido': 1 agendamento(s) para lembrar
  ➡️  Enviando lembrete para: Cliente Teste | Data: 13/02/2026 às 14:30
  ✅ Lembrete enviado com sucesso!

📋 Tenant 'salao-padrao': buscando agendamentos entre 13/02/2026 13:00 e 13/02/2026 15:00 (120 minutos de antecedência)
📋 Tenant 'salao-padrao': 0 agendamento(s) para lembrar

🎯 Total de lembretes enviados: 1
```

#### 3. Verificar que o lembrete não é enviado novamente

Aguarde mais um minuto. O log deve mostrar:

```log
📋 Tenant 'salao-rapido': buscando agendamentos entre 13/02/2026 13:01 e 13/02/2026 14:01 (60 minutos de antecedência)
📋 Tenant 'salao-rapido': 0 agendamento(s) para lembrar
```

Porque o agendamento já tem `reminderSent = true`.

---

## ✅ Validações de Erro

### Teste 1: Tempo muito pequeno (< 1 minuto)

```bash
curl -X POST http://localhost:8080/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantKey": "invalido",
    "businessName": "Inválido",
    "tempoLembreteMinutos": 0
  }'
```

**Resposta esperada:** Status 400
```json
{
  "message": "Tempo de lembrete deve ser no mínimo 1 minuto"
}
```

### Teste 2: Tempo muito grande (> 1440 minutos)

```bash
curl -X POST http://localhost:8080/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantKey": "invalido2",
    "businessName": "Inválido 2",
    "tempoLembreteMinutos": 2000
  }'
```

**Resposta esperada:** Status 400
```json
{
  "message": "Tempo de lembrete deve ser no máximo 1440 minutos (24 horas)"
}
```

---

## 📊 Verificar no Banco de Dados

```sql
-- Ver configuração de todos os tenants
SELECT 
    tenant_key,
    business_name,
    tempo_lembrete_minutos,
    CASE 
        WHEN tempo_lembrete_minutos >= 60 THEN 
            CONCAT(tempo_lembrete_minutos / 60, ' hora(s)')
        ELSE 
            CONCAT(tempo_lembrete_minutos, ' minutos')
    END as tempo_formatado,
    active
FROM tb_tenants
ORDER BY tempo_lembrete_minutos;

-- Ver agendamentos que receberão lembrete em breve
SELECT 
    a.id,
    a.tenant_id,
    a.user_name,
    a.date,
    a.start_time,
    a.reminder_sent,
    t.tempo_lembrete_minutos,
    (a.date + a.start_time - (t.tempo_lembrete_minutos || ' minutes')::INTERVAL) as horario_envio_lembrete
FROM tb_appointments a
JOIN tb_tenants t ON a.tenant_id = t.tenant_key
WHERE a.reminder_sent = false
  AND a.date >= CURRENT_DATE
ORDER BY a.date, a.start_time;
```

---

## 🎯 Checklist de Testes

- [ ] Migration aplicada com sucesso
- [ ] Coluna `tempo_lembrete_minutos` criada
- [ ] Tenants existentes têm valor 120
- [ ] Criar tenant com tempo personalizado
- [ ] Criar tenant sem especificar tempo (usa 120)
- [ ] Atualizar tempo de lembrete
- [ ] Validação de valor mínimo (1)
- [ ] Validação de valor máximo (1440)
- [ ] Scheduler usa tempo configurado
- [ ] Logs mostram tempo correto
- [ ] Lembrete não é duplicado
- [ ] Tenants diferentes usam tempos diferentes

---

## 🐛 Troubleshooting

### Problema: Migration não executa

**Solução:**
```powershell
mvn flyway:clean
mvn flyway:migrate
```

### Problema: Coluna já existe

**Solução:** A migration usa `IF NOT EXISTS`, deve funcionar. Verifique a versão no `flyway_schema_history`:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
```

### Problema: Lombok não gera getters/setters

**Solução:**
```powershell
mvn clean install -DskipTests
# Reinicie a IDE
```

### Problema: Scheduler não executa

**Solução:**
Verifique se a anotação `@EnableScheduling` está presente na classe principal:

```java
@SpringBootApplication
@EnableScheduling
public class AgendamentoBackApplication {
    // ...
}
```

---

## 📞 Suporte

Em caso de dúvidas, consulte:
- `FEATURE_TEMPO_LEMBRETE_CONFIGURAVEL.md` - Documentação completa
- Logs da aplicação em `logs/application.log`
- Tabela `flyway_schema_history` no banco de dados

