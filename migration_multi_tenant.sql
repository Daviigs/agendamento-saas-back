-- ============================================================================
-- Script de Migração Multi-Tenant
-- ============================================================================
-- Este script migra dados existentes para a estrutura multi-tenant
-- Execute este script APENAS se você já tiver dados no banco SEM tenant_id
-- ============================================================================

-- ============================================================================
-- OPÇÃO 1: Se as colunas tenant_id NÃO EXISTEM ainda
-- ============================================================================

-- 1. Adicionar coluna tenant_id nas tabelas (se não existir)
ALTER TABLE tb_appointments ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);
ALTER TABLE tb_services ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);
ALTER TABLE tb_blocked_days ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

-- 2. Atualizar registros existentes para usar tenant "default"
UPDATE tb_appointments SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE tb_services SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE tb_blocked_days SET tenant_id = 'default' WHERE tenant_id IS NULL;

-- 3. Tornar as colunas NOT NULL após popular os dados
ALTER TABLE tb_appointments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE tb_services ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE tb_blocked_days ALTER COLUMN tenant_id SET NOT NULL;

-- 4. Criar índices para melhorar performance
CREATE INDEX IF NOT EXISTS idx_appointments_tenant ON tb_appointments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_date ON tb_appointments(tenant_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_phone ON tb_appointments(tenant_id, user_phone);
CREATE INDEX IF NOT EXISTS idx_services_tenant ON tb_services(tenant_id);
CREATE INDEX IF NOT EXISTS idx_blocked_days_tenant ON tb_blocked_days(tenant_id);

-- ============================================================================
-- OPÇÃO 2: Se as colunas JÁ EXISTEM mas estão vazias
-- ============================================================================

-- Apenas atualizar registros vazios
UPDATE tb_appointments SET tenant_id = 'default' WHERE tenant_id IS NULL OR tenant_id = '';
UPDATE tb_services SET tenant_id = 'default' WHERE tenant_id IS NULL OR tenant_id = '';
UPDATE tb_blocked_days SET tenant_id = 'default' WHERE tenant_id IS NULL OR tenant_id = '';

-- ============================================================================
-- CRIAR TABELA DE TENANTS (Opcional - para produção)
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_tenants (
    tenant_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT true,
    whatsapp_session VARCHAR(100),
    whatsapp_phone VARCHAR(20),
    business_start_time TIME DEFAULT '09:00:00',
    business_end_time TIME DEFAULT '18:00:00',
    slot_interval_minutes INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inserir tenants de exemplo
INSERT INTO tb_tenants (tenant_id, name, whatsapp_session, whatsapp_phone)
VALUES
    ('default', 'Salão Principal (Legado)', 'session-default', '+5511999999999'),
    ('kc', 'KC Lash Salon', 'session-kc', '+5511988888888'),
    ('bella', 'Bella Beauty Salon', 'session-bella', '+5511977777777')
ON CONFLICT (tenant_id) DO NOTHING;

-- ============================================================================
-- VERIFICAÇÕES (Execute para validar a migração)
-- ============================================================================

-- 1. Verificar se todos os registros têm tenant_id
SELECT 'tb_appointments' as tabela, COUNT(*) as total,
       COUNT(tenant_id) as com_tenant,
       COUNT(*) - COUNT(tenant_id) as sem_tenant
FROM tb_appointments
UNION ALL
SELECT 'tb_services', COUNT(*), COUNT(tenant_id), COUNT(*) - COUNT(tenant_id)
FROM tb_services
UNION ALL
SELECT 'tb_blocked_days', COUNT(*), COUNT(tenant_id), COUNT(*) - COUNT(tenant_id)
FROM tb_blocked_days;

-- 2. Contar registros por tenant
SELECT tenant_id, COUNT(*) as total_appointments
FROM tb_appointments
GROUP BY tenant_id;

SELECT tenant_id, COUNT(*) as total_services
FROM tb_services
GROUP BY tenant_id;

SELECT tenant_id, COUNT(*) as total_blocked_days
FROM tb_blocked_days
GROUP BY tenant_id;

-- 3. Verificar índices criados
SELECT indexname, tablename
FROM pg_indexes
WHERE tablename IN ('tb_appointments', 'tb_services', 'tb_blocked_days');

-- ============================================================================
-- ROLLBACK (Em caso de problema - USE COM CUIDADO!)
-- ============================================================================

-- ⚠️ ATENÇÃO: Isso irá REMOVER as colunas tenant_id e PERDER os dados!
-- Use apenas em ambiente de desenvolvimento/teste

-- DROP INDEX IF EXISTS idx_appointments_tenant;
-- DROP INDEX IF EXISTS idx_appointments_tenant_date;
-- DROP INDEX IF EXISTS idx_appointments_tenant_phone;
-- DROP INDEX IF EXISTS idx_services_tenant;
-- DROP INDEX IF EXISTS idx_blocked_days_tenant;

-- ALTER TABLE tb_appointments DROP COLUMN IF EXISTS tenant_id;
-- ALTER TABLE tb_services DROP COLUMN IF EXISTS tenant_id;
-- ALTER TABLE tb_blocked_days DROP COLUMN IF EXISTS tenant_id;

-- DROP TABLE IF EXISTS tb_tenants;

-- ============================================================================
-- SCRIPTS ÚTEIS PARA GERENCIAR TENANTS
-- ============================================================================

-- Criar novo tenant
INSERT INTO tb_tenants (tenant_id, name, whatsapp_session, whatsapp_phone)
VALUES ('salon3', 'Salão Zona Sul', 'session-salon3', '+5511966666666');

-- Desativar tenant
UPDATE tb_tenants SET active = false WHERE tenant_id = 'salon3';

-- Reativar tenant
UPDATE tb_tenants SET active = true WHERE tenant_id = 'salon3';

-- Copiar dados de um tenant para outro (útil para criar template)
-- Copiar serviços
INSERT INTO tb_services (service_id, tenant_id, name, duration, price)
SELECT gen_random_uuid(), 'novo_tenant', name, duration, price
FROM tb_services
WHERE tenant_id = 'template_tenant';

-- Copiar dias bloqueados recorrentes
INSERT INTO tb_blocked_days (blocked_day_id, tenant_id, day_of_week, reason, is_recurring)
SELECT gen_random_uuid(), 'novo_tenant', day_of_week, reason, is_recurring
FROM tb_blocked_days
WHERE tenant_id = 'template_tenant' AND is_recurring = true;

-- ============================================================================
-- QUERIES DE ANÁLISE
-- ============================================================================

-- Total de agendamentos por tenant no mês
SELECT
    tenant_id,
    COUNT(*) as total_agendamentos,
    DATE_TRUNC('month', appointment_date) as mes
FROM tb_appointments
WHERE appointment_date >= DATE_TRUNC('month', CURRENT_DATE)
GROUP BY tenant_id, DATE_TRUNC('month', appointment_date)
ORDER BY mes DESC, total_agendamentos DESC;

-- Serviços mais populares por tenant
SELECT
    a.tenant_id,
    s.name as servico,
    COUNT(*) as total_agendamentos,
    AVG(s.price) as preco_medio
FROM tb_appointments a
JOIN tb_services s ON a.service_id = s.service_id
GROUP BY a.tenant_id, s.name
ORDER BY a.tenant_id, total_agendamentos DESC;

-- Horários mais populares por tenant
SELECT
    tenant_id,
    start_time,
    COUNT(*) as total_agendamentos
FROM tb_appointments
GROUP BY tenant_id, start_time
ORDER BY tenant_id, total_agendamentos DESC;

-- ============================================================================
-- FIM DO SCRIPT
-- ============================================================================

