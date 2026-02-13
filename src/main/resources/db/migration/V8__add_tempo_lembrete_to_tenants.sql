-- =====================================================
-- Migration: Add tempo_lembrete_minutos column to tb_tenants
-- Version: V8
-- Description: Adiciona coluna para configurar o tempo
--              de antecedência para envio de lembretes
--              de agendamentos (em minutos)
-- =====================================================

-- Adicionar coluna tempo_lembrete_minutos com valor padrão de 120 minutos (2 horas)
ALTER TABLE tb_tenants
ADD COLUMN IF NOT EXISTS tempo_lembrete_minut   os INTEGER DEFAULT 120 NOT NULL;

-- Atualizar registros existentes para garantir que todos tenham o valor padrão
UPDATE tb_tenants
SET tempo_lembrete_minutos = 120
WHERE tempo_lembrete_minutos IS NULL;

-- Adicionar constraint para garantir que o valor seja positivo e razoável
ALTER TABLE tb_tenants
ADD CONSTRAINT chk_tempo_lembrete_minutos CHECK (tempo_lembrete_minutos > 0 AND tempo_lembrete_minutos <= 1440);

-- Comentário da coluna
COMMENT ON COLUMN tb_tenants.tempo_lembrete_minutos IS 'Tempo em minutos de antecedência para envio de lembretes de agendamentos (padrão: 120 = 2 horas)';

