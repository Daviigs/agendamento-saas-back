# 🐛 Bugfix: Erro de Chave Duplicada ao Atualizar Vínculos Profissional-Serviços

## 📋 Problema

Ao tentar atualizar os serviços vinculados a um profissional (removendo serviços), ocorria erro de violação de chave única:

```
ERRO: duplicar valor da chave viola a restrição de unicidade "ukef2d8i3en4i3ryr92lfc4uln0"
Detalhe: Chave (professional_id, service_id)=(44444444-4444-4444-4444-444444444444, e0e9c2da-910d-4b4a-a5f0-5e13820db16f) já existe.
```

### Causa Raiz

O método `linkServicesToProfessional()` executa duas operações:
1. **DELETE**: Remove vínculos antigos com `deleteByProfessionalId()`
2. **INSERT**: Cria novos vínculos com `saveAll()`

O problema ocorria porque o Hibernate não estava executando o DELETE imediatamente. Quando tentava inserir os novos vínculos, alguns registros antigos ainda existiam no banco, causando violação da constraint de unicidade `(professional_id, service_id)`.

## 🔧 Solução Implementada

### 1. Repository: `ProfessionalServiceRepository.java`

**Adicionado:**
- Anotação `@Modifying` nos métodos de delete
- Queries JPQL explícitas para forçar execução imediata

```java
@Modifying
@Query("DELETE FROM ProfessionalServiceEntity ps WHERE ps.professional.id = :professionalId")
void deleteByProfessionalId(@Param("professionalId") UUID professionalId);

@Modifying
@Query("DELETE FROM ProfessionalServiceEntity ps WHERE ps.service.id = :serviceId")
void deleteByServiceId(@Param("serviceId") UUID serviceId);
```

**Por quê?**
- `@Modifying`: Indica ao Spring Data JPA que a query modifica dados
- `@Query` com JPQL: Força execução como query nativa, mais previsível que métodos derivados

### 2. Service: `ProfessionalServiceService.java`

**Adicionado:**
- Injeção de `EntityManager`
- Chamada a `entityManager.flush()` após delete

```java
private final EntityManager entityManager;

// No método linkServicesToProfessional():
professionalServiceRepository.deleteByProfessionalId(professionalId);
entityManager.flush(); // ← NOVO: Força execução do DELETE
log.info("Vínculos antigos removidos para profissional: {}", professionalId);
```

**Por quê?**
- `flush()`: Força o Hibernate a executar todas as operações pendentes no banco IMEDIATAMENTE
- Garante que o DELETE seja completado antes de tentar INSERT

## ✅ Resultado

Agora o fluxo funciona corretamente:

1. ✅ DELETE executa e remove todos os vínculos antigos
2. ✅ Flush garante que o DELETE foi concluído no banco
3. ✅ INSERT cria os novos vínculos sem conflito
4. ✅ Commit da transação finaliza tudo

## 🧪 Como Testar

### Cenário 1: Remover Serviços
```bash
# Profissional tinha serviços A e B, agora só vai ter A
PUT /professionals/{professionalId}/services
{
  "serviceIds": ["uuid-servico-A"]
}
```

**Esperado:** ✅ Sucesso (200 OK)

### Cenário 2: Adicionar Serviços
```bash
# Profissional tinha serviço A, agora terá A e B
PUT /professionals/{professionalId}/services
{
  "serviceIds": ["uuid-servico-A", "uuid-servico-B"]
}
```

**Esperado:** ✅ Sucesso (200 OK)

### Cenário 3: Trocar Todos os Serviços
```bash
# Profissional tinha A e B, agora terá C e D
PUT /professionals/{professionalId}/services
{
  "serviceIds": ["uuid-servico-C", "uuid-servico-D"]
}
```

**Esperado:** ✅ Sucesso (200 OK)

## 📝 Arquivos Alterados

1. **ProfessionalServiceRepository.java**
   - Adicionado `@Modifying` e `@Query` nos métodos de delete

2. **ProfessionalServiceService.java**
   - Adicionado `EntityManager` como dependência
   - Adicionado `flush()` após operação de delete

## 🔍 Detalhes Técnicos

### Por que o problema ocorria?

O Hibernate usa **Write-Behind** (escrita atrasada) para otimizar operações:
- Agrupa múltiplas operações
- Executa tudo de uma vez no `commit()` ou `flush()`
- Isso pode causar problemas quando há dependência de ordem

### Por que `@Modifying` é necessário?

Sem `@Modifying`, o Spring Data JPA assume que a query é de leitura (SELECT), não executando corretamente operações de escrita (UPDATE/DELETE).

### Por que `flush()` é necessário?

Mesmo com `@Modifying`, o Hibernate pode ainda atrasar a execução. O `flush()` força execução IMEDIATA de todas as operações pendentes.

## ⚠️ Considerações

- **Performance:** O `flush()` adicional pode ter pequeno impacto, mas é necessário para correção
- **Transação:** Tudo ainda ocorre em uma única transação (@Transactional)
- **Rollback:** Se algo falhar, tudo é revertido normalmente
- **Compatibilidade:** Não afeta outros endpoints ou funcionalidades

## 🎯 Conclusão

O bug foi resolvido garantindo que as operações de DELETE sejam executadas antes das operações de INSERT, evitando conflitos de chave única. A solução é segura, mantém a integridade transacional e não afeta outras funcionalidades do sistema.

