package lash_salao_kc.agendamento_back.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta com serviços vinculados a um profissional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalServicesResponse {

    private UUID professionalId;
    private String professionalName;
    private List<ServiceSummary> services;
}

