# Correção do Erro de Bloqueio de Horário Recorrente

## Problema
Ao tentar criar um bloqueio de horário recorrente, o sistema apresentava o seguinte erro:

```
jakarta.validation.ConstraintViolationException: Validation failed for classes [lash_salao_kc.agendamento_back.domain.entity.BlockedTimeSlotEntity] during persist time
ConstraintViolationImpl{interpolatedMessage='não deve ser nulo', propertyPath=professional, rootBeanClass=class lash_salao_kc.agendamento_back.domain.entity.BlockedTimeSlotEntity}
```

## Causa Raiz
A entidade `BlockedTimeSlotEntity` possui um campo `professional` marcado como `@NotNull`, mas os métodos de criação de bloqueio (`blockSpecificTimeSlot` e `blockRecurringTimeSlot`) não estavam atribuindo um profissional ao bloqueio, resultando em violação da constraint de validação.

## Solução Implementada

### 1. Atualização dos DTOs de Request

**BlockSpecificTimeSlotRequest.java**
- Adicionado campo `professionalId` do tipo UUID
- Adicionada validação `@NotNull` no campo

**BlockRecurringTimeSlotRequest.java**
- Adicionado campo `professionalId` do tipo UUID
- Adicionada validação `@NotNull` no campo

### 2. Atualização do Service

**BlockedTimeSlotService.java**
- Injetado `ProfessionalService` como dependência
- Atualizado método `blockSpecificTimeSlot` para:
  - Aceitar `UUID professionalId` como parâmetro
  - Buscar e validar o profissional usando `professionalService.getProfessionalById()`
  - Atribuir o profissional ao `BlockedTimeSlotEntity` antes de salvar
- Atualizado método `blockRecurringTimeSlot` para:
  - Aceitar `UUID professionalId` como parâmetro
  - Buscar e validar o profissional usando `professionalService.getProfessionalById()`
  - Atribuir o profissional ao `BlockedTimeSlotEntity` antes de salvar

### 3. Atualização do Controller

**BlockedTimeSlotController.java**
- Atualizado método `blockSpecificTimeSlot` para passar `request.getProfessionalId()` ao service
- Atualizado método `blockRecurringTimeSlot` para passar `request.getProfessionalId()` ao service

### 4. Atualização dos Testes

**AdvancedBlockingSystemTest.java**
- Adicionado mock de `ProfessionalService`
- Atualizado `testBlockSpecificTimeSlot_Success` para:
  - Criar um `ProfessionalEntity` de teste
  - Mockar `professionalService.getProfessionalById()`
  - Passar `professionalId` ao método sendo testado
  - Validar que o profissional foi atribuído corretamente
- Atualizado `testBlockRecurringTimeSlot_Success` com as mesmas mudanças

## Como Usar

Agora, ao criar um bloqueio de horário (específico ou recorrente), é necessário informar o ID do profissional:

**Exemplo de requisição para bloqueio específico:**
```json
POST /blocked-time-slots/specific
{
  "professionalId": "uuid-do-profissional",
  "date": "2026-02-15",
  "startTime": "14:00:00",
  "endTime": "16:00:00",
  "reason": "Reunião importante"
}
```

**Exemplo de requisição para bloqueio recorrente:**
```json
POST /blocked-time-slots/recurring
{
  "professionalId": "uuid-do-profissional",
  "dayOfWeek": "MONDAY",
  "startTime": "12:00:00",
  "endTime": "13:00:00",
  "reason": "Almoço"
}
```

## Impacto
- ✅ Correção do erro de validação
- ✅ Bloqueios agora são corretamente associados a profissionais
- ✅ Mantém a integridade referencial do banco de dados
- ✅ Permite filtrar bloqueios por profissional
- ⚠️ **Breaking Change**: Clientes da API precisam agora enviar o `professionalId` nas requisições

## Testes
Todos os testes unitários foram atualizados e validados. Execute:
```bash
./mvnw test
```

## Data da Correção
31/01/2026

