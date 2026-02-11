-- =====================================================
-- Migration: Add timezone column to tb_tenants
-- Version: V7
-- Description: Adiciona coluna timezone para suportar
--              validação de agendamentos no passado
--              considerando o timezone do tenant
-- =====================================================

-- Adicionar coluna timezone com valor padrão
ALTER TABLE tb_tenants
ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo';

-- Atualizar registros existentes que possam estar com timezone NULL
UPDATE tb_tenants
SET timezone = 'America/Sao_Paulo'
WHERE timezone IS NULL;

-- Comentário da coluna
COMMENT ON COLUMN tb_tenants.timezone IS 'Timezone do tenant para cálculos de data/hora (ex: America/Sao_Paulo, America/New_York)';

