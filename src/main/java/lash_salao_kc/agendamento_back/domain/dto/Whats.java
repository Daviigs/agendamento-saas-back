package lash_salao_kc.agendamento_back.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Whats {
    private String clienteId; // Identifica qual tenant/cliente (para escolher a sessão WhatsApp correta)
    private String telefone;
    private String nome;
    private String data;
    private String hora;
    private String servico;
}
