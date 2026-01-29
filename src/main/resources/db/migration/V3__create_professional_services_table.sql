-- ============================================================
-- MIGRATION: Vínculo entre Profissionais e Serviços
-- Data: 2026-01-29
-- Descrição: Adiciona tabela para relacionar profissionais
--            com os serviços que eles executam.
--            Implementa a regra: serviços pertencem ao tenant,
--            profissionais executam serviços.
-- ============================================================

-- Cria tabela de vínculo profissional ↔ serviço
CREATE TABLE IF NOT EXISTS tb_professional_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL,
    service_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_professional_services_professional
        FOREIGN KEY (professional_id)
        REFERENCES tb_professionals(professional_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_professional_services_service
        FOREIGN KEY (service_id)
        REFERENCES tb_services(service_id)
        ON DELETE CASCADE,

    -- Unique constraint: um profissional não pode ter o mesmo serviço vinculado duas vezes
    CONSTRAINT uk_professional_service
        UNIQUE (professional_id, service_id)
);

-- Índices para otimização de queries
CREATE INDEX idx_professional_services_professional
    ON tb_professional_services(professional_id);

CREATE INDEX idx_professional_services_service
    ON tb_professional_services(service_id);

-- Comentários
COMMENT ON TABLE tb_professional_services IS
    'Vínculo N:N entre profissionais e serviços. Define quais serviços cada profissional pode executar.';

COMMENT ON COLUMN tb_professional_services.professional_id IS
    'ID do profissional que executa o serviço';

COMMENT ON COLUMN tb_professional_services.service_id IS
    'ID do serviço que o profissional pode executar';

COMMENT ON COLUMN tb_professional_services.created_at IS
    'Data/hora de criação do vínculo';

-- ============================================================
-- OBSERVAÇÃO: Esta migration é retrocompatível.
-- Agendamentos antigos continuarão funcionando normalmente.
-- A validação só será aplicada para novos agendamentos
-- quando houver vínculos configurados.
-- ============================================================

