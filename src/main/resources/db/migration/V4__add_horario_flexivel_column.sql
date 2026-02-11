-- Migration V4: Adiciona coluna horario_flexivel na tabela tb_tenant_working_hours
--
-- Esta flag define como o sistema valida horários de agendamento:
-- - horario_flexivel = true: Permite agendamentos ultrapassarem horários bloqueados e horário final
-- - horario_flexivel = false: Horários bloqueados e horário final são limites rígidos
--
-- Padrão: false (comportamento mais restritivo para segurança)

-- Passo 1: Adicionar coluna como NULLABLE primeiro
ALTER TABLE tb_tenant_working_hours
ADD COLUMN IF NOT EXISTS horario_flexivel BOOLEAN;

-- Passo 2: Atualizar valores NULL existentes para FALSE (padrão seguro)
UPDATE tb_tenant_working_hours
SET horario_flexivel = false
WHERE horario_flexivel IS NULL;

-- Passo 3: Adicionar constraint NOT NULL após garantir que não há valores NULL
ALTER TABLE tb_tenant_working_hours
ALTER COLUMN horario_flexivel SET NOT NULL;

-- Passo 4: Adicionar valor padrão para novos registros
ALTER TABLE tb_tenant_working_hours
ALTER COLUMN horario_flexivel SET DEFAULT false;

-- Adiciona comentário explicativo na coluna
COMMENT ON COLUMN tb_tenant_working_hours.horario_flexivel IS
    'Define se agendamentos podem ultrapassar bloqueios e horário final. ' ||
    'true = agenda flexível (bloqueios não impedem continuidade), ' ||
    'false = agenda rígida (bloqueios são barreiras absolutas)';


