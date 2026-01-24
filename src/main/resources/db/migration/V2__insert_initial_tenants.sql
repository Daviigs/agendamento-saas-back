-- ========================================
-- OBJETIVO: Inserir tenants iniciais no sistema
-- ========================================
-- Insere o tenant 'kc' (Lash Salão KC)
INSERT INTO tb_tenants (tenant_key, business_name, contact_email, contact_phone, active)
VALUES ('kc', 'Lash Salão KC', 'contato@lashkc.com.br', '(11) 99999-9999', TRUE)
ON CONFLICT (tenant_key) DO NOTHING;

-- Comentário
COMMENT ON TABLE tb_tenants IS 'Cadastro de tenants (salões/clientes) do sistema multi-tenant';

