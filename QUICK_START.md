# 🚀 Quick Start Guide - Sistema Avançado de Bloqueio de Horários

## ⏱️ Tempo Estimado: 10 minutos

Este guia rápido mostra como começar a usar o sistema avançado de bloqueio de horários em 3 passos simples.

---

## 📋 Pré-requisitos

- ✅ Projeto já rodando
- ✅ Banco de dados PostgreSQL/H2 configurado
- ✅ Ferramenta de teste de API (Postman, Insomnia, curl)

---

## 🎯 Passo 1: Criar as Tabelas (2 minutos)

### Opção A: Via psql (PostgreSQL)

```bash
psql -U seu_usuario -d seu_banco -f src/main/resources/db/create_advanced_blocking_tables.sql
```

### Opção B: Via SQL Client

Copie e execute o conteúdo do arquivo `src/main/resources/db/create_advanced_blocking_tables.sql`

### Opção C: Deixar o Hibernate criar (Dev apenas)

Se estiver usando H2 ou tem `spring.jpa.hibernate.ddl-auto=update`, as tabelas serão criadas automaticamente.

---

## ⚙️ Passo 2: Configurar Horário de Trabalho (2 minutos)

Configure o horário de trabalho do seu tenant. Exemplo para tenant "kc":

```bash
curl -X POST http://localhost:8080/working-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "startTime": "09:00",
    "endTime": "18:00",
    "slotIntervalMinutes": 30
  }'
```

**Resposta esperada:** Status 200 OK com os dados configurados

### Verificar Configuração

```bash
curl -X GET http://localhost:8080/working-hours \
  -H "X-Tenant-Id: kc"
```

---

## 🎨 Passo 3: Testar Funcionalidades (6 minutos)

### 3.1 Consultar Horários Disponíveis

```bash
curl -X GET "http://localhost:8080/appointments/available-slots?date=2026-01-20" \
  -H "X-Tenant-Id: kc"
```

**Resultado esperado:** Lista com horários das 09:00 às 17:30 (intervalos de 30min)

```json
[
  "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
  "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
  "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
]
```

### 3.2 Bloquear Horário de Almoço

```bash
curl -X POST http://localhost:8080/blocked-time-slots/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-01-20",
    "startTime": "12:00",
    "endTime": "13:00",
    "reason": "Horário de almoço"
  }'
```

**Resposta esperada:** Status 201 Created

```json
{
  "id": "uuid-gerado",
  "tenantId": "kc",
  "specificDate": "2026-01-20",
  "startTime": "12:00",
  "endTime": "13:00",
  "reason": "Horário de almoço",
  "recurring": false,
  "dayOfWeek": null
}
```

### 3.3 Verificar que Horário foi Bloqueado

```bash
curl -X GET "http://localhost:8080/appointments/available-slots?date=2026-01-20" \
  -H "X-Tenant-Id: kc"
```

**Resultado esperado:** 12:00 e 12:30 NÃO devem aparecer na lista!

```json
[
  "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
  "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
  "16:00", "16:30", "17:00", "17:30"
]
```

### 3.4 Bloquear Horário Recorrente (Toda Segunda)

```bash
curl -X POST http://localhost:8080/blocked-time-slots/recurring \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "dayOfWeek": "MONDAY",
    "startTime": "16:00",
    "endTime": "17:00",
    "reason": "Horário de limpeza semanal"
  }'
```

Agora, **TODAS as segundas-feiras**, das 16:00 às 17:00 estarão bloqueadas!

### 3.5 Listar Todos os Bloqueios

```bash
curl -X GET http://localhost:8080/blocked-time-slots \
  -H "X-Tenant-Id: kc"
```

Você verá seus 2 bloqueios: um específico e um recorrente.

### 3.6 Desbloquear um Horário

```bash
# Substitua {uuid-do-bloqueio} pelo ID retornado no passo 3.2
curl -X DELETE http://localhost:8080/blocked-time-slots/{uuid-do-bloqueio} \
  -H "X-Tenant-Id: kc"
```

**Resposta esperada:** Status 204 No Content

Agora, ao consultar horários disponíveis novamente, 12:00 e 12:30 voltarão a aparecer!

---

## ✅ Teste Final: Criar Agendamento

### Tentar Agendar em Horário Bloqueado (Deve Falhar)

Assumindo que você ainda tem o bloqueio recorrente de segunda das 16:00-17:00:

```bash
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "serviceIds": ["id-de-um-servico-valido"],
    "date": "2026-01-20",
    "startTime": "16:00",
    "userName": "Teste",
    "userPhone": "+5511999999999"
  }'
```

**Resultado esperado:** Status 422 - Erro informando horário bloqueado

### Agendar em Horário Disponível (Deve Funcionar)

```bash
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "serviceIds": ["id-de-um-servico-valido"],
    "date": "2026-01-20",
    "startTime": "10:00",
    "userName": "Cliente Teste",
    "userPhone": "+5511999999999"
  }'
```

**Resultado esperado:** Status 201 Created - Agendamento criado com sucesso!

---

## 🎓 Cenários Comuns de Uso

### Cenário 1: Salão com Horário de Almoço

```bash
# Bloquear das 12h às 13h toda segunda a sexta
for day in MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY; do
  curl -X POST http://localhost:8080/blocked-time-slots/recurring \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: kc" \
    -d "{
      \"dayOfWeek\": \"$day\",
      \"startTime\": \"12:00\",
      \"endTime\": \"13:00\",
      \"reason\": \"Horário de almoço\"
    }"
done
```

### Cenário 2: Fechar Mais Cedo em um Dia

```bash
# Bloquear das 16h às 18h em uma sexta específica
curl -X POST http://localhost:8080/blocked-time-slots/specific \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{
    "date": "2026-01-24",
    "startTime": "16:00",
    "endTime": "18:00",
    "reason": "Fechando mais cedo - evento"
  }'
```

### Cenário 3: Diferentes Profissionais, Diferentes Horários

```bash
# Profissional KC: 09:00 - 18:00
curl -X POST http://localhost:8080/working-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: kc" \
  -d '{"startTime":"09:00","endTime":"18:00","slotIntervalMinutes":30}'

# Profissional MJS: 07:00 - 16:00
curl -X POST http://localhost:8080/working-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: mjs" \
  -d '{"startTime":"07:00","endTime":"16:00","slotIntervalMinutes":30}'
```

---

## 🆘 Troubleshooting Rápido

### Problema: "Tenant ID não encontrado"
**Solução:** Verifique se está enviando o header `X-Tenant-Id`

### Problema: "Horário fora do expediente"
**Solução:** Configure o horário de trabalho primeiro ou ajuste o horário do bloqueio

### Problema: "Já existe bloqueio neste horário"
**Solução:** Liste os bloqueios existentes e verifique conflitos

### Problema: Horários não aparecem
**Solução:** Verifique se o dia não está bloqueado completamente via `/blocked-days`

---

## 📚 Próximos Passos

Agora que você testou o básico, explore:

1. **Documentação Completa**: [ADVANCED_BLOCKING_SYSTEM.md](ADVANCED_BLOCKING_SYSTEM.md)
2. **Guia de Migração**: [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)
3. **Exemplos de API**: [API_EXAMPLES.json](API_EXAMPLES.json)
4. **Arquitetura**: [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md)

---

## 💡 Dicas

- Use `GET /blocked-time-slots` frequentemente para ver o estado atual
- Teste primeiro em datas futuras para evitar confusão
- Mantenha os IDs dos bloqueios se precisar removê-los depois
- Consulte `/appointments/available-slots` para visualizar o resultado final

---

## ✨ Parabéns!

Você configurou e testou o sistema avançado de bloqueio de horários com sucesso! 🎉

O sistema agora está pronto para uso em produção com total flexibilidade na gestão de disponibilidade.

---

**Tempo total gasto:** ~10 minutos  
**Nível de dificuldade:** ⭐⭐ (Fácil)  
**Status:** ✅ Pronto para uso

