-- ========================================
-- FEATURE: Exceções de Bloqueios Recorrentes
-- ========================================
--
-- Permite liberar uma data específica que cai em um dia bloqueado recorrente.
--
-- Exemplo de uso:
-- - Todos os domingos são bloqueados (recorrente)
-- - Mas o dono quer trabalhar no domingo 15/02/2026
-- - Cria-se uma exceção para essa data específica
--
-- Ordem de prioridade para validação de agendamento:
-- 1. ❌ Dia bloqueado pontual (específico) - MAIOR PRIORIDADE
-- 2. ✅ Dia liberado por exceção
-- 3. ❌ Dia bloqueado recorrente
-- 4. ✅ Dia permitido
-- ========================================

CREATE TABLE tb_blocked_day_exceptions (
    exception_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    exception_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_tenant_exception_date UNIQUE (tenant_id, exception_date)
);

-- Índices para otimização
CREATE INDEX idx_blocked_day_exceptions_tenant ON tb_blocked_day_exceptions(tenant_id);
CREATE INDEX idx_blocked_day_exceptions_date ON tb_blocked_day_exceptions(tenant_id, exception_date);

-- Comentários
COMMENT ON TABLE tb_blocked_day_exceptions IS 'Exceções para liberar datas específicas de bloqueios recorrentes';
COMMENT ON COLUMN tb_blocked_day_exceptions.tenant_id IS 'ID do tenant (cliente multi-tenant)';
COMMENT ON COLUMN tb_blocked_day_exceptions.exception_date IS 'Data específica liberada como exceção ao bloqueio recorrente';
COMMENT ON COLUMN tb_blocked_day_exceptions.reason IS 'Motivo da liberação (ex: "Trabalho extra", "Reposição")';

