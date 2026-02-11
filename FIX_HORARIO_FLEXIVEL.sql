-- Script de Correção: Remove coluna horario_flexivel se existir com problemas
-- Execute este script ANTES de reiniciar a aplicação se houver problemas

-- Verificar estado atual da coluna
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'tb_tenant_working_hours'
  AND column_name = 'horario_flexivel';

-- Se a coluna existir com problemas, remova-a
ALTER TABLE tb_tenant_working_hours
DROP COLUMN IF EXISTS horario_flexivel;

-- Verificar histórico do Flyway
SELECT installed_rank, version, description, type, script, success, installed_on
FROM flyway_schema_history
WHERE version = '4'
ORDER BY installed_rank DESC;

-- Se a migration V4 foi executada com erro, remova-a do histórico
-- ATENÇÃO: Apenas execute se necessário
-- DELETE FROM flyway_schema_history WHERE version = '4';

-- Agora reinicie a aplicação e o Flyway aplicará corretamente a V4

