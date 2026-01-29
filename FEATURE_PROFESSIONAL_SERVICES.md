# 🔗 Sistema de Vínculo Profissional ↔ Serviço

**Data de Implementação:** 2026-01-29  
**Versão:** 1.0  
**Status:** ✅ Implementado e Retrocompatível

---

## 📋 Visão Geral

Esta feature implementa a regra de negócio onde:
- **Serviços pertencem ao tenant (salão)**
- **Profissionais EXECUTAM serviços**
- Um serviço pode ser executado por vários profissionais
- Um profissional pode executar vários serviços

## 🎯 Objetivos

1. Permitir que o admin do salão configure quais serviços cada profissional pode executar
2. Validar agendamentos: apenas profissionais habilitados podem realizar os serviços
3. Filtrar profissionais disponíveis baseado nos serviços selecionados
4. Manter **100% de retrocompatibilidade** com agendamentos existentes

---

## 🏗️ Arquitetura

### Entidade: `ProfessionalServiceEntity`

Tabela: `tb_professional_services`

```java
@Entity
public class ProfessionalServiceEntity {
    UUID id;
    ProfessionalEntity professional;  // Profissional que executa
    ServicesEntity service;            // Serviço executado
    LocalDateTime createdAt;
}
```

### Repository: `ProfessionalServiceRepository`

- Queries otimizadas para buscar vínculos
- Validação se profissional executa todos os serviços
- Busca de profissionais qualificados para uma lista de serviços

### Service: `ProfessionalServiceService`

- `linkServicesToProfessional()`: Vincula serviços a um profissional
- `unlinkServiceFromProfessional()`: Remove vínculo específico
- `getServicesByProfessional()`: Lista serviços de um profissional
- `professionalExecutesAllServices()`: Valida se profissional executa todos os serviços
- `getProfessionalsByServices()`: Busca profissionais que executam serviços

---

## 🔌 Endpoints da API

### 1. Listar Serviços de um Profissional

**GET** `/professionals/{professionalId}/services`

**Headers:**
```
X-Tenant-Id: kc
```

**Resposta 200 OK:**
```json
{
  "professionalId": "uuid",
  "professionalName": "Maria Silva",
  "services": [
    {
      "id": "uuid",
      "name": "Design de Sobrancelhas",
      "duration": 30,
      "price": 50.00
    },
    {
      "id": "uuid",
      "name": "Aplicação de Cílios",
      "duration": 90,
      "price": 150.00
    }
  ]
}
```

---

### 2. Vincular Serviços a um Profissional

**PUT** `/professionals/{professionalId}/services`

**Headers:**
```
X-Tenant-Id: kc
Content-Type: application/json
```

**Body:**
```json
{
  "serviceIds": [
    "service-uuid-1",
    "service-uuid-2",
    "service-uuid-3"
  ]
}
```

**Resposta 200 OK:** (mesmo formato do GET)

**Observações:**
- Remove todos os vínculos antigos
- Cria novos vínculos
- Valida que todos os serviços pertencem ao tenant

---

### 3. Desvincular Serviço de um Profissional

**DELETE** `/professionals/{professionalId}/services/{serviceId}`

**Headers:**
```
X-Tenant-Id: kc
```

**Resposta:** `204 No Content`

---

### 4. Listar Profissionais Ativos (com filtro de serviços)

**GET** `/professionals/active?serviceIds=uuid1,uuid2`

**Headers:**
```
X-Tenant-Id: kc
```

**Query Parameters:**
- `serviceIds` (opcional): Lista de UUIDs separados por vírgula

**Comportamento:**
- **SEM serviceIds:** Retorna TODOS os profissionais ativos (comportamento original)
- **COM serviceIds:** Retorna APENAS profissionais que executam TODOS os serviços

**Resposta 200 OK:**
```json
[
  {
    "id": "uuid",
    "tenantId": "uuid",
    "professionalName": "Maria Silva",
    "professionalEmail": "maria@example.com",
    "professionalPhone": "11999999999",
    "active": true
  }
]
```

---

## 🔒 Validações

### Na Criação de Agendamento

Quando um agendamento é criado, o sistema valida:

1. ✅ Tenant existe e está ativo
2. ✅ Profissional pertence ao tenant
3. ✅ Profissional está ativo
4. ✅ **NOVO:** Profissional executa TODOS os serviços do agendamento
5. ✅ Data não está bloqueada
6. ✅ Horário está dentro do expediente
7. ✅ Não há conflitos com outros agendamentos

**Se a validação 4 falhar:**
```json
{
  "message": "O profissional selecionado não está habilitado para executar todos os serviços deste agendamento. Por favor, selecione outro profissional ou ajuste os serviços."
}
```

---

## 🔄 Fluxo de Uso

### 1. Configuração Inicial (Admin)

```bash
# 1. Criar serviços do salão
POST /services
{
  "name": "Design de Sobrancelhas",
  "duration": 30,
  "price": 50.00
}

# 2. Criar profissionais
POST /professionals
{
  "professionalName": "Maria Silva",
  "professionalEmail": "maria@example.com",
  "professionalPhone": "11999999999"
}

# 3. Vincular serviços aos profissionais
PUT /professionals/{professionalId}/services
{
  "serviceIds": ["service-uuid-1", "service-uuid-2"]
}
```

### 2. Fluxo de Agendamento (Cliente)

```bash
# 1. Cliente seleciona serviços
serviceIds = ["service-uuid-1", "service-uuid-2"]

# 2. Frontend busca profissionais qualificados
GET /professionals/active?serviceIds=service-uuid-1,service-uuid-2

# 3. Cliente escolhe profissional e horário

# 4. Cria agendamento
POST /appointments
{
  "professionalId": "professional-uuid",
  "serviceIds": ["service-uuid-1", "service-uuid-2"],
  "date": "2026-02-15",
  "startTime": "14:00",
  "userName": "João Silva",
  "userPhone": "11888888888"
}
```

---

## ✅ Retrocompatibilidade

### Como funciona?

A implementação é **100% retrocompatível**:

1. **Agendamentos antigos:** Continuam funcionando normalmente
2. **Sem vínculos configurados:** Sistema permite agendamentos (como antes)
3. **Com vínculos configurados:** Sistema valida os vínculos

### Migração Gradual

```
┌─────────────────────────────────────────────┐
│  ANTES (Sistema Legado)                     │
├─────────────────────────────────────────────┤
│  ❌ Sem validação de serviços               │
│  ✅ Qualquer profissional → qualquer serviço│
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  TRANSIÇÃO (Coexistência)                   │
├─────────────────────────────────────────────┤
│  ⚠️  Vínculos sendo configurados            │
│  ✅ Agendamentos antigos funcionam          │
│  ✅ Novos agendamentos validados            │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  DEPOIS (Sistema Novo)                      │
├─────────────────────────────────────────────┤
│  ✅ Todos os profissionais têm vínculos     │
│  ✅ Validação completa ativa                │
│  ✅ Filtros de profissionais funcionando    │
└─────────────────────────────────────────────┘
```

---

## 🗄️ Banco de Dados

### Migration: `V3__create_professional_services_table.sql`

```sql
CREATE TABLE tb_professional_services (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL,
    service_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_professional_services_professional 
        FOREIGN KEY (professional_id) 
        REFERENCES tb_professionals(professional_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_professional_services_service 
        FOREIGN KEY (service_id) 
        REFERENCES tb_services(service_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT uk_professional_service 
        UNIQUE (professional_id, service_id)
);
```

### Índices

- `idx_professional_services_professional` (otimiza busca por profissional)
- `idx_professional_services_service` (otimiza busca por serviço)

---

## 🧪 Testes

### Cenário 1: Criar Vínculo

```bash
PUT /professionals/{professionalId}/services
{
  "serviceIds": ["service-1", "service-2"]
}

✅ Esperado: Vínculos criados, resposta 200 OK
```

### Cenário 2: Agendamento Válido

```bash
# Profissional executa: [service-1, service-2]
POST /appointments
{
  "professionalId": "prof-1",
  "serviceIds": ["service-1"],  # ✅ Subset válido
  ...
}

✅ Esperado: Agendamento criado
```

### Cenário 3: Agendamento Inválido

```bash
# Profissional executa: [service-1]
POST /appointments
{
  "professionalId": "prof-1",
  "serviceIds": ["service-1", "service-2"],  # ❌ service-2 não vinculado
  ...
}

❌ Esperado: Erro 400 Bad Request
```

### Cenário 4: Filtro de Profissionais

```bash
# Serviços selecionados: [service-1, service-2]
GET /professionals/active?serviceIds=service-1,service-2

✅ Esperado: Apenas profissionais que executam AMBOS os serviços
```

---

## 📊 Casos de Uso

### 1. Salão com Especialistas

**Contexto:** Salão tem 3 profissionais, cada um especializado em serviços diferentes

- **Ana:** Design de Sobrancelhas, Henna
- **Beatriz:** Aplicação de Cílios, Lifting de Cílios
- **Carla:** Todos os serviços (profissional completa)

**Configuração:**
```bash
# Ana
PUT /professionals/{ana-id}/services
{ "serviceIds": ["design-sobrancelhas", "henna"] }

# Beatriz
PUT /professionals/{beatriz-id}/services
{ "serviceIds": ["aplicacao-cilios", "lifting-cilios"] }

# Carla
PUT /professionals/{carla-id}/services
{ "serviceIds": ["design-sobrancelhas", "henna", "aplicacao-cilios", "lifting-cilios"] }
```

**Resultado:**
- Cliente escolhe "Design de Sobrancelhas" → Frontend mostra: Ana, Carla
- Cliente escolhe "Aplicação de Cílios" → Frontend mostra: Beatriz, Carla
- Cliente escolhe "Design + Aplicação" → Frontend mostra: Carla (única qualificada)

### 2. Migração de Sistema Antigo

**Situação:** Salão já tem 500 agendamentos históricos sem vínculos configurados

**Solução:**
1. Deploy da nova versão (sem vínculos)
2. Agendamentos antigos continuam funcionando
3. Admin configura vínculos gradualmente
4. Novos agendamentos respeitam vínculos
5. Sistema 100% operacional durante toda transição

---

## 🚨 Troubleshooting

### Problema: "Profissional não está habilitado"

**Causa:** Profissional não tem vínculo com o serviço

**Solução:**
```bash
PUT /professionals/{professionalId}/services
{
  "serviceIds": ["service-uuid"]
}
```

### Problema: "Nenhum profissional disponível"

**Causa:** Nenhum profissional executa todos os serviços selecionados

**Soluções:**
1. Reduzir número de serviços no agendamento
2. Vincular mais serviços aos profissionais existentes
3. Contratar profissional mais completo

---

## 📝 Checklist de Implementação

- [x] Entidade `ProfessionalServiceEntity`
- [x] Repository `ProfessionalServiceRepository`
- [x] Service `ProfessionalServiceService`
- [x] Controller `ProfessionalServiceController`
- [x] DTOs: `LinkServicesRequest`, `ServiceSummary`, `ProfessionalServicesResponse`
- [x] Validação em `AppointmentsService.createAppointment()`
- [x] Filtro em `ProfessionalController.getActiveProfessionals()`
- [x] Migration SQL `V3__create_professional_services_table.sql`
- [x] Documentação completa
- [x] Retrocompatibilidade garantida

---

## 🎉 Conclusão

A feature foi implementada com sucesso, mantendo 100% de compatibilidade com o sistema existente. O admin pode configurar gradualmente os vínculos, e o sistema se adapta automaticamente.

**Próximos passos sugeridos:**
1. Testar endpoints em ambiente de desenvolvimento
2. Configurar vínculos para profissionais existentes
3. Atualizar frontend para usar o filtro de profissionais
4. Monitorar logs durante a transição

