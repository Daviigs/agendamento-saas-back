# 🧪 Testes - Fix de Exclusão de Serviço

## 📋 Preparação dos Testes

### Variáveis de Ambiente
```bash
BASE_URL=http://localhost:8080
TENANT_ID=lashsalao  # ou seu tenant ID
```

### Headers Necessários
```json
{
  "X-Tenant-Id": "lashsalao",
  "Content-Type": "application/json"
}
```

---

## 🧪 Cenário 1: Deletar Serviço SEM Agendamentos

### 1.1. Criar um Novo Serviço
```http
POST http://localhost:8080/services
X-Tenant-Id: lashsalao
Content-Type: application/json

{
  "name": "Teste Exclusão - Serviço Temporário",
  "duration": 30,
  "price": 50.00
}
```

**Resposta Esperada:** `201 Created`
```json
{
  "id": "uuid-gerado",
  "name": "Teste Exclusão - Serviço Temporário",
  "duration": 30,
  "price": 50.00,
  "tenantId": "lashsalao"
}
```

### 1.2. Deletar o Serviço (Deve Funcionar ✅)
```http
DELETE http://localhost:8080/services/{uuid-do-servico}
X-Tenant-Id: lashsalao
```

**Resposta Esperada:** `204 No Content`

---

## 🧪 Cenário 2: Deletar Serviço COM Agendamentos FUTUROS

### 2.1. Criar um Serviço
```http
POST http://localhost:8080/services
X-Tenant-Id: lashsalao
Content-Type: application/json

{
  "name": "Alongamento Premium",
  "duration": 60,
  "price": 150.00
}
```

**Resposta:** Guardar o `id` retornado (ex: `service-id-123`)

### 2.2. Criar um Agendamento FUTURO Usando o Serviço
```http
POST http://localhost:8080/appointments
X-Tenant-Id: lashsalao
Content-Type: application/json

{
  "professionalId": "uuid-do-profissional",
  "date": "2026-02-10",
  "startTime": "14:00",
  "serviceIds": ["service-id-123"],
  "userName": "Cliente Teste",
  "userPhone": "11999999999"
}
```

**Resposta Esperada:** `201 Created`

### 2.3. Tentar Deletar o Serviço (Deve Falhar ⚠️)
```http
DELETE http://localhost:8080/services/service-id-123
X-Tenant-Id: lashsalao
```

**Resposta Esperada:** `400 Bad Request`
```json
{
  "timestamp": "2026-02-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Não é possível excluir o serviço 'Alongamento Premium' pois ele está sendo usado em agendamentos futuros. Remova ou atualize os agendamentos futuros antes de excluir o serviço.",
  "path": "/services/service-id-123"
}
```

---

## 🧪 Cenário 2.5: Deletar Serviço COM Agendamentos PASSADOS (DEVE FUNCIONAR ✅)

### 2.5.1. Criar um Serviço
```http
POST http://localhost:8080/services
X-Tenant-Id: lashsalao
Content-Type: application/json

{
  "name": "Design de Sobrancelhas",
  "duration": 45,
  "price": 80.00
}
```

### 2.5.2. Criar Agendamento PASSADO (manualmente no banco ou esperar passar)
```sql
-- Inserir agendamento passado diretamente no banco para teste
INSERT INTO tb_appointments (appointment_id, tenant_id, professional_id, date, start_time, end_time, user_name, user_phone)
VALUES (gen_random_uuid(), 'lashsalao', 'uuid-do-profissional', '2026-01-15', '10:00', '10:45', 'Cliente Antigo', '11999999999');

INSERT INTO tb_appointment_services (appointment_id, service_id)
VALUES ((SELECT appointment_id FROM tb_appointments WHERE date = '2026-01-15' ORDER BY created_at DESC LIMIT 1), 'service-id');
```

### 2.5.3. Deletar o Serviço (DEVE FUNCIONAR ✅)
```http
DELETE http://localhost:8080/services/service-id
X-Tenant-Id: lashsalao
```

**Resposta Esperada:** `204 No Content` ✅
- Como o agendamento já passou, o serviço pode ser deletado!

---

## 🧪 Cenário 3: Deletar Agendamento e Depois o Serviço

### 3.1. Deletar o Agendamento
```http
DELETE http://localhost:8080/appointments/{appointment-id}
X-Tenant-Id: lashsalao
```

**Resposta Esperada:** `204 No Content`

### 3.2. Deletar o Serviço (Agora Deve Funcionar ✅)
```http
DELETE http://localhost:8080/services/service-id-123
X-Tenant-Id: lashsalao
```

**Resposta Esperada:** `204 No Content`

---

## 🧪 Cenário 4: Serviço Vinculado a Múltiplos Agendamentos

### 4.1. Criar Serviço
```http
POST http://localhost:8080/services
X-Tenant-Id: lashsalao
Content-Type: application/json

{
  "name": "Design de Sobrancelhas",
  "duration": 45,
  "price": 80.00
}
```

### 4.2. Criar 3 Agendamentos
```http
# Agendamento 1
POST http://localhost:8080/appointments
{
  "professionalId": "prof-id",
  "date": "2026-02-05",
  "startTime": "10:00",
  "serviceIds": ["service-id"],
  "userName": "Cliente 1",
  "userPhone": "11111111111"
}

# Agendamento 2
POST http://localhost:8080/appointments
{
  "professionalId": "prof-id",
  "date": "2026-02-06",
  "startTime": "11:00",
  "serviceIds": ["service-id"],
  "userName": "Cliente 2",
  "userPhone": "22222222222"
}

# Agendamento 3
POST http://localhost:8080/appointments
{
  "professionalId": "prof-id",
  "date": "2026-02-07",
  "startTime": "14:00",
  "serviceIds": ["service-id"],
  "userName": "Cliente 3",
  "userPhone": "33333333333"
}
```

### 4.3. Tentar Deletar o Serviço
```http
DELETE http://localhost:8080/services/service-id
X-Tenant-Id: lashsalao
```

**Resposta Esperada:** `400 Bad Request`
- Mesmo com múltiplos agendamentos, a validação funciona

### 4.4. Deletar Todos os Agendamentos
```http
DELETE http://localhost:8080/appointments/{id-1}
DELETE http://localhost:8080/appointments/{id-2}
DELETE http://localhost:8080/appointments/{id-3}
```

### 4.5. Deletar o Serviço (Agora Funciona)
```http
DELETE http://localhost:8080/services/service-id
X-Tenant-Id: lashsalao
```

**Resposta Esperada:** `204 No Content` ✅

---

## 🧪 Cenário 5: Validação de Segurança (Tenant Isolation)

### 5.1. Criar Serviço no Tenant A
```http
POST http://localhost:8080/services
X-Tenant-Id: tenant-a
Content-Type: application/json

{
  "name": "Serviço Tenant A",
  "duration": 30,
  "price": 50.00
}
```

### 5.2. Tentar Deletar com Tenant B (Deve Falhar)
```http
DELETE http://localhost:8080/services/{service-id}
X-Tenant-Id: tenant-b
```

**Resposta Esperada:** `404 Not Found`
- Garante que tenants não podem deletar serviços de outros

---

## 📊 Checklist de Validação

Use este checklist para garantir que tudo está funcionando:

- [ ] ✅ Consegue criar um serviço novo
- [ ] ✅ Consegue deletar serviço sem agendamentos
- [ ] ✅ Consegue deletar serviço com agendamentos PASSADOS
- [ ] ✅ Recebe erro 400 ao tentar deletar serviço com agendamentos FUTUROS
- [ ] ✅ Mensagem de erro é clara e descritiva (menciona "futuros")
- [ ] ✅ Após deletar agendamentos futuros, consegue deletar o serviço
- [ ] ✅ Múltiplos agendamentos futuros são detectados corretamente
- [ ] ✅ Isolamento de tenant funciona (não deleta de outro tenant)
- [ ] ✅ Vínculos com profissionais são removidos automaticamente
- [ ] ✅ Agendamento de hoje que já passou permite exclusão
- [ ] ✅ Agendamento de hoje que ainda não passou bloqueia exclusão

---

## 🐛 Troubleshooting

### Problema: Recebo 500 em vez de 400
**Solução:** 
- Verifique se a aplicação foi recompilada
- Reinicie o servidor Spring Boot
- Verifique os logs para ver se há erros de compilação

### Problema: Consigo deletar serviço mesmo com agendamentos
**Solução:**
- Verifique se as mudanças foram aplicadas corretamente
- Confirme que `appointmentsRepository.existsByServiceId()` existe
- Verifique se a validação está no método `deleteService()`

### Problema: Erro de conexão com banco
**Solução:**
- Verifique se o PostgreSQL está rodando
- Confirme as credenciais em `application-dev.properties`
- Teste a query SQL manualmente no banco

---

## 🔍 Validação Manual no Banco de Dados

Se quiser verificar diretamente no banco:

```sql
-- Ver serviços e seus agendamentos (TODOS)
SELECT 
    s.service_id,
    s.name,
    COUNT(aps.appointment_id) as qtd_agendamentos_total,
    COUNT(CASE WHEN a.date > CURRENT_DATE 
               OR (a.date = CURRENT_DATE AND a.start_time >= CURRENT_TIME) 
          THEN 1 END) as qtd_agendamentos_futuros
FROM tb_services s
LEFT JOIN tb_appointment_services aps ON s.service_id = aps.service_id
LEFT JOIN tb_appointments a ON aps.appointment_id = a.appointment_id
GROUP BY s.service_id, s.name
ORDER BY qtd_agendamentos_futuros DESC, qtd_agendamentos_total DESC;

-- Verificar se um serviço específico tem agendamentos FUTUROS
SELECT EXISTS(
    SELECT 1 
    FROM tb_appointment_services aps
    JOIN tb_appointments a ON aps.appointment_id = a.appointment_id
    WHERE aps.service_id = 'uuid-do-servico'
    AND (a.date > CURRENT_DATE 
         OR (a.date = CURRENT_DATE AND a.start_time >= CURRENT_TIME))
) as tem_agendamentos_futuros;

-- Listar agendamentos de um serviço específico (passados vs futuros)
SELECT 
    a.date,
    a.start_time,
    a.user_name,
    CASE 
        WHEN a.date > CURRENT_DATE THEN 'FUTURO'
        WHEN a.date = CURRENT_DATE AND a.start_time >= CURRENT_TIME THEN 'FUTURO (hoje)'
        ELSE 'PASSADO'
    END as status
FROM tb_appointments a
JOIN tb_appointment_services aps ON a.appointment_id = aps.appointment_id
WHERE aps.service_id = 'uuid-do-servico'
ORDER BY a.date DESC, a.start_time DESC;
```

---

## ✅ Resultados Esperados

| Ação | Status | Mensagem |
|------|--------|----------|
| Deletar serviço sem uso | 204 | (vazio) |
| Deletar serviço com agendamentos passados | 204 | (vazio) ✅ NOVA FUNCIONALIDADE |
| Deletar serviço com agendamentos futuros | 400 | Mensagem clara sobre agendamentos futuros |
| Deletar após remover agendamentos futuros | 204 | (vazio) |
| Tenant isolation | 404 | Serviço não encontrado |
| Agendamento de hoje às 10:00 (agora 10:01) | 204 | Pode deletar (já passou) |
| Agendamento de hoje às 14:00 (agora 13:00) | 400 | Não pode deletar (ainda não passou) |

---

**Última Atualização:** 2026-02-01  
**Status dos Testes:** ✅ Prontos para Execução

