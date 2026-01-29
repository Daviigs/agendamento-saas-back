package lash_salao_kc.agendamento_back.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de resposta com informações de serviço vinculado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSummary {

    private UUID id;
    private String name;
    private int duration;
    private double price;
}

