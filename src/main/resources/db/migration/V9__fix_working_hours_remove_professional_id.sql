    -- V9: Corrige tb_tenant_working_hours para que o horário pertença ao TENANT, não ao profissional.
    -- Regra: professional_id deve ser NULL — o horário de funcionamento é global por tenant.

    -- Passo 1 (CRÍTICO): Tornar a coluna professional_id nullable ANTES de qualquer operação.
    -- Isso remove a constraint NOT NULL que impede inserções com professional_id = NULL.
    ALTER TABLE tb_tenant_working_hours ALTER COLUMN professional_id DROP NOT NULL;

    -- Passo 2: Para cada tenant, manter apenas 1 registro (o primeiro via DISTINCT ON)
    -- e limpar o professional_id desse registro.
    UPDATE tb_tenant_working_hours
    SET professional_id = NULL
    WHERE working_hours_id IN (
        SELECT DISTINCT ON (tenant_id) working_hours_id
        FROM tb_tenant_working_hours
        ORDER BY tenant_id, working_hours_id
    );

    -- Passo 3: Remover registros duplicados (manter apenas 1 por tenant)
    DELETE FROM tb_tenant_working_hours
    WHERE working_hours_id NOT IN (
        SELECT DISTINCT ON (tenant_id) working_hours_id
        FROM tb_tenant_working_hours
        ORDER BY tenant_id, working_hours_id
    );



