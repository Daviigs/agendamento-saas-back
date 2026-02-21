-- ========================================
-- OBJETIVO: Preencher originalAmount para agendamentos existentes
-- ========================================
-- Este script calcula e preenche o originalAmount para agendamentos
-- que foram criados antes da implementação dos campos financeiros

-- Atualiza agendamentos que não têm originalAmount definido
UPDATE tb_appointments
SET
    original_amount = (
        SELECT COALESCE(SUM(s.price), 0)
        FROM tb_appointment_services tas
        JOIN tb_services s ON s.service_id = tas.service_id
        WHERE tas.appointment_id = tb_appointments.appointment_id
    ),
    total_amount = COALESCE(total_amount, (
        SELECT COALESCE(SUM(s.price), 0)
        FROM tb_appointment_services tas
        JOIN tb_services s ON s.service_id = tas.service_id
        WHERE tas.appointment_id = tb_appointments.appointment_id
    )),
    discount_amount = COALESCE(discount_amount, 0)
WHERE original_amount IS NULL;

-- Recalcula o desconto para agendamentos que já tinham totalAmount mas não tinham originalAmount
UPDATE tb_appointments
SET discount_amount = original_amount - COALESCE(total_amount, 0)
WHERE discount_amount = 0
  AND original_amount IS NOT NULL
  AND total_amount IS NOT NULL
  AND original_amount != total_amount;

-- Mostra resultado da correção
SELECT
    COUNT(*) as total_agendamentos,
    COUNT(CASE WHEN original_amount IS NOT NULL THEN 1 END) as com_original_amount,
    COUNT(CASE WHEN total_amount IS NOT NULL THEN 1 END) as com_total_amount,
    COUNT(CASE WHEN discount_amount > 0 THEN 1 END) as com_desconto,
    ROUND(AVG(original_amount), 2) as valor_medio_original,
    ROUND(SUM(discount_amount), 2) as total_descontos
FROM tb_appointments;

SELECT '✅ Dados financeiros dos agendamentos existentes atualizados!' as status;

