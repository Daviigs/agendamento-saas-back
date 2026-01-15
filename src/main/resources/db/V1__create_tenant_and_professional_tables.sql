-- ========================================
-- OBJETIVO 1: Tabela de Tenants
-- ========================================

-- Criação da tabela de tenants (salões/clientes)
CREATE TABLE tb_tenants (
    tenant_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_key VARCHAR(50) NOT NULL UNIQUE,
    business_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tenant_key_format CHECK (tenant_key ~ '^[a-z0-9\-_]+$')
);

-- Índices para otimização
CREATE INDEX idx_tenants_key ON tb_tenants(tenant_key);
CREATE INDEX idx_tenants_active ON tb_tenants(active);

-- Comentários
COMMENT ON TABLE tb_tenants IS 'Cadastro de tenants (salões/clientes) do sistema multi-tenant';
COMMENT ON COLUMN tb_tenants.tenant_key IS 'Chave única do tenant usada no header X-Tenant-Id (ex: kc, mjs)';
COMMENT ON COLUMN tb_tenants.business_name IS 'Nome comercial do salão/empresa';
COMMENT ON COLUMN tb_tenants.active IS 'Indica se o tenant está ativo no sistema';

-- ========================================
-- OBJETIVO 2: Tabela de Profissionais
-- ========================================

-- Criação da tabela de profissionais
CREATE TABLE tb_professionals (
    professional_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    professional_name VARCHAR(255) NOT NULL,
    professional_email VARCHAR(255),
    professional_phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_professional_tenant FOREIGN KEY (tenant_id) REFERENCES tb_tenants(tenant_id) ON DELETE CASCADE
);

-- Índices para otimização
CREATE INDEX idx_professionals_tenant ON tb_professionals(tenant_id);
CREATE INDEX idx_professionals_active ON tb_professionals(tenant_id, active);

-- Comentários
COMMENT ON TABLE tb_professionals IS 'Cadastro de profissionais vinculados aos tenants';
COMMENT ON COLUMN tb_professionals.tenant_id IS 'Tenant ao qual o profissional pertence';
COMMENT ON COLUMN tb_professionals.professional_name IS 'Nome do profissional';
COMMENT ON COLUMN tb_professionals.specialty IS 'Especialidade do profissional (ex: Cílios, Unhas, etc)';
COMMENT ON COLUMN tb_professionals.active IS 'Indica se o profissional está ativo';

-- ========================================
-- OBJETIVO 2: Migração dos Horários de Trabalho
-- ========================================

-- Adiciona coluna professional_id na tabela de horários de trabalho
ALTER TABLE tb_tenant_working_hours ADD COLUMN professional_id UUID;

-- Remove a constraint de unique no tenant_id (agora pode ter múltiplos profissionais)
ALTER TABLE tb_tenant_working_hours DROP CONSTRAINT IF EXISTS tb_tenant_working_hours_tenant_id_key;

-- Adiciona foreign key para professional
ALTER TABLE tb_tenant_working_hours ADD CONSTRAINT fk_working_hours_professional
    FOREIGN KEY (professional_id) REFERENCES tb_professionals(professional_id) ON DELETE CASCADE;

-- Cria índice composto para busca otimizada
CREATE INDEX idx_working_hours_tenant_professional ON tb_tenant_working_hours(tenant_id, professional_id);

-- Comentário
COMMENT ON COLUMN tb_tenant_working_hours.professional_id IS 'Profissional ao qual o horário de trabalho pertence';

-- ========================================
-- OBJETIVO 2: Migração dos Bloqueios de Horários
-- ========================================

-- Adiciona coluna professional_id na tabela de bloqueios de horários
ALTER TABLE tb_blocked_time_slots ADD COLUMN professional_id UUID;

-- Adiciona foreign key para professional
ALTER TABLE tb_blocked_time_slots ADD CONSTRAINT fk_blocked_slots_professional
    FOREIGN KEY (professional_id) REFERENCES tb_professionals(professional_id) ON DELETE CASCADE;

-- Cria índices compostos para busca otimizada
CREATE INDEX idx_blocked_slots_tenant_professional ON tb_blocked_time_slots(tenant_id, professional_id);
CREATE INDEX idx_blocked_slots_prof_specific_date ON tb_blocked_time_slots(professional_id, specific_date) WHERE is_recurring = FALSE;
CREATE INDEX idx_blocked_slots_prof_recurring ON tb_blocked_time_slots(professional_id, day_of_week) WHERE is_recurring = TRUE;

-- Comentário
COMMENT ON COLUMN tb_blocked_time_slots.professional_id IS 'Profissional ao qual o bloqueio de horário pertence';

-- ========================================
-- OBJETIVO 2: Migração dos Agendamentos
-- ========================================

-- Adiciona coluna professional_id na tabela de agendamentos
ALTER TABLE tb_appointments ADD COLUMN professional_id UUID;

-- Adiciona foreign key para professional
ALTER TABLE tb_appointments ADD CONSTRAINT fk_appointments_professional
    FOREIGN KEY (professional_id) REFERENCES tb_professionals(professional_id) ON DELETE RESTRICT;

-- Cria índices compostos para busca otimizada
CREATE INDEX idx_appointments_tenant_professional ON tb_appointments(tenant_id, professional_id);
CREATE INDEX idx_appointments_prof_date ON tb_appointments(professional_id, appointment_date);
CREATE INDEX idx_appointments_prof_date_time ON tb_appointments(professional_id, appointment_date, start_time, end_time);

-- Comentário
COMMENT ON COLUMN tb_appointments.professional_id IS 'Profissional responsável pelo agendamento';

-- ========================================
-- DADOS INICIAIS: Tenants e Profissionais
-- ========================================

-- Inserindo tenants existentes
INSERT INTO tb_tenants (tenant_id, tenant_key, business_name, contact_email, active)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'kc', 'KC Lash Studio', 'contato@kclash.com', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'mjs', 'MJS Beauty', 'contato@mjsbeauty.com', TRUE);

-- Inserindo profissionais padrão para cada tenant
-- KC Lash Studio
INSERT INTO tb_professionals (professional_id, tenant_id, professional_name, active)
VALUES
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'KC - Profissional Principal', TRUE);

-- MJS Beauty
INSERT INTO tb_professionals (professional_id, tenant_id, professional_name, active)
VALUES
    ('44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 'MJS - Profissional Principal', TRUE);

-- ========================================
-- MIGRAÇÃO DE DADOS EXISTENTES
-- ========================================

-- Atualiza horários de trabalho existentes vinculando ao profissional padrão
UPDATE tb_tenant_working_hours
SET professional_id = '33333333-3333-3333-3333-333333333333'
WHERE tenant_id = 'kc' AND professional_id IS NULL;

UPDATE tb_tenant_working_hours
SET professional_id = '44444444-4444-4444-4444-444444444444'
WHERE tenant_id = 'mjs' AND professional_id IS NULL;

-- Atualiza bloqueios de horários existentes vinculando ao profissional padrão
UPDATE tb_blocked_time_slots
SET professional_id = '33333333-3333-3333-3333-333333333333'
WHERE tenant_id = 'kc' AND professional_id IS NULL;

UPDATE tb_blocked_time_slots
SET professional_id = '44444444-4444-4444-4444-444444444444'
WHERE tenant_id = 'mjs' AND professional_id IS NULL;

-- Atualiza agendamentos existentes vinculando ao profissional padrão
UPDATE tb_appointments
SET professional_id = '33333333-3333-3333-3333-333333333333'
WHERE tenant_id = 'kc' AND professional_id IS NULL;

UPDATE tb_appointments
SET professional_id = '44444444-4444-4444-4444-444444444444'
WHERE tenant_id = 'mjs' AND professional_id IS NULL;

-- ========================================
-- CONSTRAINTS OBRIGATÓRIAS
-- ========================================

-- Torna professional_id obrigatório após migração
ALTER TABLE tb_tenant_working_hours ALTER COLUMN professional_id SET NOT NULL;
ALTER TABLE tb_blocked_time_slots ALTER COLUMN professional_id SET NOT NULL;
ALTER TABLE tb_appointments ALTER COLUMN professional_id SET NOT NULL;

-- Garante que cada profissional tenha apenas uma configuração de horário
ALTER TABLE tb_tenant_working_hours ADD CONSTRAINT uq_professional_working_hours UNIQUE (professional_id);

