-- Script SQL para criar as tabelas do sistema de bloqueio avançado de horários

-- Tabela de horários de trabalho por tenant (profissional)
CREATE TABLE tb_tenant_working_hours (
    working_hours_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL UNIQUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_interval_minutes INTEGER NOT NULL DEFAULT 30,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_working_hours_valid CHECK (start_time < end_time),
    CONSTRAINT chk_slot_interval_valid CHECK (slot_interval_minutes > 0 AND slot_interval_minutes <= 120)
);

-- Índice para busca rápida por tenant
CREATE INDEX idx_tenant_working_hours_tenant ON tb_tenant_working_hours(tenant_id);

-- Tabela de bloqueios de horários específicos
CREATE TABLE tb_blocked_time_slots (
    blocked_slot_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    specific_date DATE,
    day_of_week VARCHAR(20),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(500) NOT NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_blocked_slot_time_valid CHECK (start_time < end_time),
    CONSTRAINT chk_blocked_slot_type CHECK (
        (is_recurring = FALSE AND specific_date IS NOT NULL AND day_of_week IS NULL) OR
        (is_recurring = TRUE AND specific_date IS NULL AND day_of_week IS NOT NULL)
    )
);

-- Índices para otimização de consultas
CREATE INDEX idx_blocked_slots_tenant ON tb_blocked_time_slots(tenant_id);
CREATE INDEX idx_blocked_slots_specific_date ON tb_blocked_time_slots(tenant_id, specific_date) WHERE is_recurring = FALSE;
CREATE INDEX idx_blocked_slots_recurring ON tb_blocked_time_slots(tenant_id, day_of_week) WHERE is_recurring = TRUE;
CREATE INDEX idx_blocked_slots_date_time ON tb_blocked_time_slots(tenant_id, specific_date, start_time, end_time);

-- Comentários nas tabelas
COMMENT ON TABLE tb_tenant_working_hours IS 'Horários de trabalho personalizados por profissional (tenant)';
COMMENT ON TABLE tb_blocked_time_slots IS 'Bloqueios de intervalos de horários específicos ou recorrentes';

-- Comentários nas colunas
COMMENT ON COLUMN tb_tenant_working_hours.tenant_id IS 'ID único do profissional/colaborador';
COMMENT ON COLUMN tb_tenant_working_hours.start_time IS 'Horário de início do expediente';
COMMENT ON COLUMN tb_tenant_working_hours.end_time IS 'Horário de término do expediente';
COMMENT ON COLUMN tb_tenant_working_hours.slot_interval_minutes IS 'Intervalo entre slots de agendamento (minutos)';

COMMENT ON COLUMN tb_blocked_time_slots.tenant_id IS 'ID do profissional dono do bloqueio';
COMMENT ON COLUMN tb_blocked_time_slots.specific_date IS 'Data específica do bloqueio (NULL se recorrente)';
COMMENT ON COLUMN tb_blocked_time_slots.day_of_week IS 'Dia da semana do bloqueio recorrente (NULL se específico)';
COMMENT ON COLUMN tb_blocked_time_slots.start_time IS 'Horário de início do bloqueio';
COMMENT ON COLUMN tb_blocked_time_slots.end_time IS 'Horário de término do bloqueio';
COMMENT ON COLUMN tb_blocked_time_slots.is_recurring IS 'TRUE para bloqueios recorrentes, FALSE para bloqueios específicos';

-- Exemplos de inserção de dados iniciais
-- Configuração de horários de trabalho
INSERT INTO tb_tenant_working_hours (working_hours_id, tenant_id, start_time, end_time, slot_interval_minutes, active)
VALUES
    (gen_random_uuid(), 'kc', '09:00:00', '18:00:00', 30, TRUE),
    (gen_random_uuid(), 'mjs', '07:00:00', '16:00:00', 30, TRUE);

-- Exemplo de bloqueio específico (almoço em uma data específica)
-- INSERT INTO tb_blocked_time_slots (blocked_slot_id, tenant_id, specific_date, day_of_week, start_time, end_time, reason, is_recurring)
-- VALUES (gen_random_uuid(), 'kc', '2026-01-20', NULL, '12:00:00', '13:00:00', 'Almoço', FALSE);

-- Exemplo de bloqueio recorrente (almoço todas as segundas-feiras)
-- INSERT INTO tb_blocked_time_slots (blocked_slot_id, tenant_id, specific_date, day_of_week, start_time, end_time, reason, is_recurring)
-- VALUES (gen_random_uuid(), 'kc', NULL, 'MONDAY', '12:00:00', '13:00:00', 'Almoço recorrente', TRUE);

