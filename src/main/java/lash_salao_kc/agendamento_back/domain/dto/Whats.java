package lash_salao_kc.agendamento_back.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para envio de mensagens via WhatsApp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Whats {

    private String telefone;
    private String nome;
    private String data;
    private String hora;
    private String servico;
    private String clienteId;
}
