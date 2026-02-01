# 🐛 BUGFIX: Violação de Constraint ao Deletar Serviço

## 📋 Descrição do Problema

Ao tentar deletar um serviço que estava sendo usado em agendamentos existentes, o sistema gerava um erro de violação de constraint de chave estrangeira:

```
org.springframework.dao.DataIntegrityViolationException: could not execute statement 
[ERRO: atualização ou exclusão em tabela "tb_services" viola restrição de chave 
estrangeira "fkqtpymvf5rl3yl4y9r2cvqhu0j" em "tb_appointment_services"]
```

## 🎯 Regra de Negócio Implementada

**IMPORTANTE:** O sistema agora permite deletar serviços que só possuem agendamentos passados, bloqueando apenas quando há **agendamentos futuros**.

### Lógica de Validação:
- ✅ **PODE DELETAR**: Serviço usado apenas em agendamentos passados
- ❌ **NÃO PODE DELETAR**: Serviço usado em agendamentos futuros ou agendamentos de hoje que ainda não aconteceram

### Definição de "Agendamento Futuro":
- Data do agendamento > Data atual, OU
- Data do agendamento = Data atual E Horário >= Horário atual

### Causa Raiz

O método `deleteService()` do `ServicesService` não validava se o serviço estava sendo referenciado na tabela de junção `tb_appointment_services` antes de tentar deletá-lo. Como existe uma constraint de chave estrangeira, o PostgreSQL bloqueava a operação para manter a integridade referencial.

**Adicionalmente**, a primeira versão do fix bloqueava a exclusão mesmo para agendamentos passados, o que não fazia sentido do ponto de vista de negócio.

## ✅ Solução Implementada

### 1. Criação de Método de Validação no Repository

**Arquivo:** `AppointmentsRepository.java`

Adicionado método para verificar se um serviço está sendo usado em **agendamentos FUTUROS**:

```java
/**
 * Verifica se existe algum agendamento FUTURO que utiliza o serviço especificado.
 * Considera futuro: data maior que hoje OU data igual a hoje com horário maior ou igual ao atual.
 */
@Query("""
    SELECT COUNT(a) > 0 FROM AppointmentsEntity a 
    JOIN a.services s 
    WHERE s.id = :serviceId
    AND (a.date > :currentDate OR (a.date = :currentDate AND a.startTime >= :currentTime))
""")
boolean existsFutureAppointmentsByServiceId(
        @Param("serviceId") UUID serviceId,
        @Param("currentDate") LocalDate currentDate,
        @Param("currentTime") java.time.LocalTime currentTime
);
```

### 2. Validação Antes da Exclusão

**Arquivo:** `ServicesService.java`

Modificado o método `deleteService()` para validar apenas agendamentos futuros:

```java
@Transactional
public void deleteService(UUID id) {
    ServicesEntity service = findById(id);
    
    // Valida se o serviço está sendo usado em algum agendamento FUTURO
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    
    if (appointmentsRepository.existsFutureAppointmentsByServiceId(id, today, now)) {
        throw new BusinessException(
            String.format("Não é possível excluir o serviço '%s' pois ele está sendo usado em agendamentos futuros. " +
                    "Remova ou atualize os agendamentos futuros antes de excluir o serviço.", service.getName())
        );
    }
    
    // Remove vínculos com profissionais
    professionalServicesRepository.deleteByServiceId(id);
    
    // Deleta o serviço
    servicesRepository.delete(service);
}
```

## 🔄 Fluxo de Validação

```
DELETE /services/{id}
         │
         ▼
   ServicesService.deleteService()
         │
         ├─► findById() - Verifica se o serviço existe
         │
         ├─► appointmentsRepository.existsFutureAppointmentsByServiceId()
         │   Verifica se há agendamentos FUTUROS
         │   │
         │   ├─► SIM → throw BusinessException (HTTP 400)
         │   │         "Não é possível excluir o serviço...
         │   │          pois ele está sendo usado em agendamentos futuros"
         │   │
         │   └─► NÃO (só tem agendamentos passados ou nenhum) → Continua...
         │
         ├─► professionalServicesRepository.deleteByServiceId()
         │   Remove vínculos com profissionais
         │
         └─► servicesRepository.delete()
             Deleta o serviço com sucesso
```

## 📅 Exemplos de Cenários

### ✅ PODE DELETAR:
1. Serviço sem nenhum agendamento
2. Serviço usado apenas em agendamentos de ontem
3. Serviço usado apenas em agendamentos da semana passada
4. Serviço usado em agendamento de hoje às 10:00, sendo agora 10:01

### ❌ NÃO PODE DELETAR:
1. Serviço com agendamento para amanhã
2. Serviço com agendamento para hoje às 15:00, sendo agora 14:00
3. Serviço com agendamento para hoje às 14:00, sendo agora 14:00 (horário exato)
4. Serviço com múltiplos agendamentos, sendo pelo menos um futuro

## 📝 Comportamento Esperado

### Antes do Fix
- ❌ Erro 500 (Internal Server Error)
- ❌ Stack trace exposto
- ❌ Mensagem técnica confusa para o usuário

### Depois do Fix (v1)
- ✅ Erro 400 (Bad Request) com mensagem clara
- ✅ Validação antes da tentativa de exclusão
- ⚠️ Mas bloqueava TODOS os agendamentos (incluindo passados)

### Depois do Fix (v2 - ATUAL)
- ✅ Erro 400 (Bad Request) apenas para agendamentos futuros
- ✅ Permite exclusão de serviços com agendamentos passados
- ✅ Mensagem amigável explicando o problema:
  ```json
  {
    "error": "Não é possível excluir o serviço 'Alongamento de Cílios' pois ele está 
             sendo usado em agendamentos futuros. Remova ou atualize os agendamentos 
             futuros antes de excluir o serviço."
  }
  ```

## 🧪 Testes Sugeridos

### Cenário 1: Deletar serviço SEM agendamentos
```bash
# 1. Criar um serviço
POST /services
{
  "name": "Teste Exclusão",
  "duration": 30,
  "price": 50.00
}

# 2. Deletar o serviço (deve funcionar)
DELETE /services/{service_id}
# Resposta: 204 No Content ✅
```

### Cenário 2: Deletar serviço COM agendamentos
```bash
# 1. Criar um serviço
POST /services
{
  "name": "Alongamento",
  "duration": 60,
  "price": 120.00
}

# 2. Criar um agendamento usando o serviço
POST /appointments
{
  "professionalId": "...",
  "date": "2026-02-05",
  "startTime": "10:00",
  "serviceIds": ["{service_id}"],
  "userName": "Cliente Teste",
  "userPhone": "11999999999"
}

# 3. Tentar deletar o serviço (deve falhar com mensagem clara)
DELETE /services/{service_id}
# Resposta: 400 Bad Request ✅
# {
#   "error": "Não é possível excluir o serviço 'Alongamento' pois ele está 
#            sendo usado em agendamentos existentes..."
# }
```

### Cenário 3: Deletar agendamento e depois o serviço
```bash
# 1. Deletar o agendamento
DELETE /appointments/{appointment_id}
# Resposta: 204 No Content ✅

# 2. Deletar o serviço (agora deve funcionar)
DELETE /services/{service_id}
# Resposta: 204 No Content ✅
```

## 🎯 Boas Práticas Aplicadas

1. **Validação Preemptiva**: Verifica antes de tentar a operação
2. **Mensagens Claras**: Explica o problema e sugere solução
3. **HTTP Status Correto**: 400 (Bad Request) em vez de 500
4. **Integridade Referencial**: Mantém a consistência do banco
5. **Transação Atômica**: `@Transactional` garante rollback em caso de erro

## 📊 Impacto

- ✅ Melhora a experiência do usuário com mensagens claras
- ✅ Evita erros de servidor (500) desnecessários
- ✅ Mantém a integridade dos dados
- ✅ Facilita troubleshooting e manutenção
- ✅ Segue as melhores práticas de RESTful APIs

## 🔗 Arquivos Modificados

1. `AppointmentsRepository.java` - Adicionado método `existsByServiceId()`
2. `ServicesService.java` - Adicionada validação no método `deleteService()`

---

**Data:** 2026-02-01  
**Tipo:** Bugfix  
**Prioridade:** Alta  
**Status:** ✅ Implementado

