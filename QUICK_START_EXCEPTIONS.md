# 🚀 Guia Rápido - Exceções de Bloqueios Recorrentes

## ⚡ Início Rápido em 5 Minutos

### 1️⃣ Iniciar o Sistema

```bash
cd "C:\Users\daviigs\Documents\site mainha\lash-salao-kc-back"
mvn spring-boot:run
```

A migration `V5__create_blocked_day_exceptions_table.sql` será executada automaticamente.

### 2️⃣ Configurar Bloqueio Recorrente

Bloquear todos os domingos:

```bash
curl -X POST http://localhost:8080/blocked-days/recurring ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: kc" ^
  -d "{\"dayOfWeek\": \"SUNDAY\", \"reason\": \"Folga semanal\"}"
```

### 3️⃣ Criar Exceção

Liberar domingo 15/02/2026:

```bash
curl -X POST http://localhost:8080/blocked-days/exceptions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: kc" ^
  -d "{\"exceptionDate\": \"2026-02-15\", \"reason\": \"Trabalho extra\"}"
```

### 4️⃣ Verificar Resultado

```bash
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-01&endDate=2026-02-28" ^
  -H "X-Tenant-Id: kc"
```

✅ Deve incluir 15/02 mas não outros domingos!

---

## 📋 Comandos Essenciais

### Gerenciar Exceções

```bash
# Criar exceção
POST /blocked-days/exceptions
{
  "exceptionDate": "2026-02-15",
  "reason": "Trabalho extra"
}

# Listar todas
GET /blocked-days/exceptions

# Listar apenas futuras
GET /blocked-days/exceptions/future

# Remover
DELETE /blocked-days/exceptions/{id}
```

### Gerenciar Bloqueios

```bash
# Bloquear dia recorrente
POST /blocked-days/recurring
{"dayOfWeek": "SUNDAY", "reason": "Folga"}

# Bloquear data específica
POST /blocked-days/specific
{"date": "2026-12-25", "reason": "Natal"}

# Listar todos
GET /blocked-days
```

---

## 🎯 Regras de Ouro

### ✅ O que PODE fazer:

1. ✅ Criar exceção para liberar domingo bloqueado
2. ✅ Criar múltiplas exceções
3. ✅ Remover exceção (volta a bloquear)

### ❌ O que NÃO PODE fazer:

1. ❌ Criar exceção para data com bloqueio específico
2. ❌ Criar exceção duplicada
3. ❌ Exceção NÃO sobrepõe bloqueio específico

---

## 🔍 Ordem de Prioridade

```
1. ❌ Bloqueio Específico (SEMPRE bloqueia)
2. ✅ Exceção (Libera bloqueio recorrente)
3. ❌ Bloqueio Recorrente
4. ✅ Dia normal (Sem bloqueio)
```

---

## 💡 Exemplos Práticos

### Exemplo 1: Trabalhar em Domingo

**Problema:** Todos domingos bloqueados, mas preciso trabalhar dia 15/02

**Solução:**
```bash
curl -X POST http://localhost:8080/blocked-days/exceptions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: kc" ^
  -d "{\"exceptionDate\": \"2026-02-15\", \"reason\": \"Evento especial\"}"
```

### Exemplo 2: Cancelar Exceção

**Problema:** Criei exceção mas mudei de ideia

**Solução:**
```bash
# 1. Listar exceções e pegar o ID
curl -X GET http://localhost:8080/blocked-days/exceptions ^
  -H "X-Tenant-Id: kc"

# 2. Remover
curl -X DELETE http://localhost:8080/blocked-days/exceptions/{id} ^
  -H "X-Tenant-Id: kc"
```

### Exemplo 3: Feriado em Dia Normal

**Problema:** Segunda-feira é dia de trabalho, mas dia 23/02 é feriado

**Solução:**
Use bloqueio específico (não exceção):
```bash
curl -X POST http://localhost:8080/blocked-days/specific ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: kc" ^
  -d "{\"date\": \"2026-02-23\", \"reason\": \"Carnaval\"}"
```

---

## 🧪 Teste Rápido

```bash
# 1. Bloquear domingos
curl -X POST http://localhost:8080/blocked-days/recurring -H "Content-Type: application/json" -H "X-Tenant-Id: kc" -d "{\"dayOfWeek\": \"SUNDAY\", \"reason\": \"Folga\"}"

# 2. Criar exceção
curl -X POST http://localhost:8080/blocked-days/exceptions -H "Content-Type: application/json" -H "X-Tenant-Id: kc" -d "{\"exceptionDate\": \"2026-02-15\", \"reason\": \"Trabalho\"}"

# 3. Verificar
curl -X GET "http://localhost:8080/blocked-days/available?startDate=2026-02-01&endDate=2026-02-28" -H "X-Tenant-Id: kc"
```

---

## 📁 Arquivos da Feature

```
src/main/java/lash_salao_kc/agendamento_back/
├── domain/
│   ├── entity/
│   │   └── BlockedDayExceptionEntity.java      ✨ Nova entidade
│   └── dto/
│       └── CreateBlockedDayExceptionRequest.java ✨ Novo DTO
├── repository/
│   └── BlockedDayExceptionRepository.java       ✨ Novo repository
├── service/
│   └── BlockedDayService.java                   🔧 Modificado
└── controller/
    └── BlockedDayExceptionController.java       ✨ Novo controller

src/main/resources/db/migration/
└── V5__create_blocked_day_exceptions_table.sql  ✨ Nova migration
```

---

## 🐛 Troubleshooting Rápido

### Problema: "Data possui bloqueio específico"

**Causa:** Tentou criar exceção para data com bloqueio específico

**Solução:** Remova o bloqueio específico primeiro
```bash
GET /blocked-days/specific  # Pegar ID
DELETE /blocked-days/{id}   # Remover
```

### Problema: "Já existe exceção"

**Causa:** Exceção duplicada

**Solução:** Remova a existente ou use outra data
```bash
GET /blocked-days/exceptions  # Ver existentes
DELETE /blocked-days/exceptions/{id}  # Remover se necessário
```

### Problema: Exceção não está funcionando

**Verificar:**
1. Header `X-Tenant-Id` está correto?
2. Data tem bloqueio específico? (Tem prioridade)
3. Exceção foi salva?
```bash
GET /blocked-days/exceptions -H "X-Tenant-Id: kc"
```

---

## 📚 Documentação Completa

- 📖 `FEATURE_BLOCKED_DAY_EXCEPTIONS.md` - Documentação completa
- 🧪 `TESTES_BLOCKED_DAY_EXCEPTIONS.md` - Guia de testes
- 📊 `DIAGRAMA_BLOCKED_DAY_EXCEPTIONS.md` - Diagramas visuais

---

## ✅ Checklist de Validação

Após implementar, validar:

- [ ] Migration executada sem erros
- [ ] Criar exceção funciona
- [ ] Exceção libera dia bloqueado recorrente
- [ ] Bloqueio específico tem prioridade
- [ ] Não permite exceção em bloqueio específico
- [ ] Multi-tenancy funciona (dados isolados)
- [ ] API retorna dados corretos

---

## 🎓 Conceitos Importantes

### Bloqueio Específico vs Exceção

**Bloqueio Específico:**
- Para bloquear um dia que normalmente é permitido
- Exemplo: Feriado, fechamento pontual
- Tem PRIORIDADE MÁXIMA

**Exceção:**
- Para liberar um dia que é bloqueado recorrentemente
- Exemplo: Trabalhar em um domingo específico
- Não sobrepõe bloqueio específico

### Multi-Tenancy

Cada tenant (salão) tem seus próprios:
- Bloqueios
- Exceções
- Agendamentos

Use sempre o header `X-Tenant-Id` correto!

---

## 🔗 Endpoints Completos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/blocked-days/exceptions` | Criar exceção |
| GET | `/blocked-days/exceptions` | Listar todas |
| GET | `/blocked-days/exceptions/future` | Listar futuras |
| DELETE | `/blocked-days/exceptions/{id}` | Remover |
| GET | `/blocked-days/available` | Verificar disponibilidade |

---

## 💻 Exemplo de Resposta

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "tenantId": "kc",
  "exceptionDate": "2026-02-15",
  "reason": "Trabalho extra"
}
```

---

## 📞 Suporte

Em caso de dúvidas:
1. Consulte `FEATURE_BLOCKED_DAY_EXCEPTIONS.md`
2. Veja os testes em `TESTES_BLOCKED_DAY_EXCEPTIONS.md`
3. Analise os diagramas em `DIAGRAMA_BLOCKED_DAY_EXCEPTIONS.md`

