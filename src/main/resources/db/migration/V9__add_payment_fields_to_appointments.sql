-- ========================================
-- OBJETIVO: Adicionar campos de rastreamento financeiro aos agendamentos
-- ========================================
-- Este script adiciona campos para controle de pagamento aos agendamentos,
-- permitindo rastreamento financeiro completo dos serviços prestados.
-- Inclui suporte para descontos e diferenças entre valor calculado e pago.

-- Adiciona coluna para status do pagamento
ALTER TABLE tb_appointments ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Adiciona coluna para método de pagamento (opcional, preenchido após pagamento)
ALTER TABLE tb_appointments ADD COLUMN payment_method VARCHAR(20);

-- Adiciona coluna para valor original calculado (soma dos serviços - não muda)
ALTER TABLE tb_appointments ADD COLUMN original_amount DECIMAL(10,2);

-- Adiciona coluna para valor total efetivamente pago (pode ter desconto/acréscimo)
ALTER TABLE tb_appointments ADD COLUMN total_amount DECIMAL(10,2);

-- Adiciona coluna para registrar o valor do desconto aplicado
ALTER TABLE tb_appointments ADD COLUMN discount_amount DECIMAL(10,2) DEFAULT 0;

-- Adiciona coluna para data/hora do pagamento
ALTER TABLE tb_appointments ADD COLUMN paid_at TIMESTAMP;

-- Cria índice para consultas por status de pagamento
CREATE INDEX idx_appointments_payment_status ON tb_appointments(payment_status);

-- Cria índice composto para relatórios financeiros (tenant + status + data)
CREATE INDEX idx_appointments_financial ON tb_appointments(tenant_id, payment_status, appointment_date);

-- Comentários para documentação
COMMENT ON COLUMN tb_appointments.payment_status IS 'Status do pagamento: PENDING, PAID ou CANCELLED';
COMMENT ON COLUMN tb_appointments.payment_method IS 'Método de pagamento: CASH, PIX, CARD ou TRANSFER';
COMMENT ON COLUMN tb_appointments.original_amount IS 'Valor original calculado (soma dos serviços) - não muda';
COMMENT ON COLUMN tb_appointments.total_amount IS 'Valor efetivamente pago - pode ter desconto/acréscimo';
COMMENT ON COLUMN tb_appointments.discount_amount IS 'Valor do desconto aplicado (originalAmount - totalAmount). Negativo se houver acréscimo';
COMMENT ON COLUMN tb_appointments.paid_at IS 'Data e hora em que o pagamento foi confirmado';


