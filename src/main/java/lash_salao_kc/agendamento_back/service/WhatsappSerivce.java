package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.dto.Whats;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;

@Service
public class WhatsappSerivce {
    private final RestTemplate restTemplate = new RestTemplate();

    public void enviarAgendamento(Whats dto) {

        String url = "http://localhost:3001/whatsapp/agendamento";

        restTemplate.postForEntity(url, dto, String.class);
    }

    public void enviarLembrete(AppointmentsEntity appointment) {
        String url = "http://localhost:3001/whatsapp/lembrete";

        try {
            // Remove o "+" caso venha com "+55", mantém apenas "55"
            String telefoneParaWhatsapp = appointment.getUserPhone().startsWith("+")
                ? appointment.getUserPhone().substring(1)
                : appointment.getUserPhone();

            // Concatena os nomes de todos os serviços
            String servicosNomes = appointment.getServices().stream()
                    .map(service -> service.getName())
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .orElse("Serviço não especificado");

            Whats dto = new Whats();
            dto.setClienteId(appointment.getTenantId()); // ✅ ENVIA O TENANT/CLIENT ID
            dto.setTelefone(telefoneParaWhatsapp);
            dto.setNome(appointment.getUserName());
            dto.setData(appointment.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setHora(appointment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            dto.setServico(servicosNomes);

            restTemplate.postForEntity(url, dto, String.class);
            logger.info("Lembrete enviado com sucesso para o WhatsApp - Cliente: {}", appointment.getUserName());
        } catch (Exception e) {
            logger.error("Erro ao enviar lembrete para o WhatsApp: {}", e.getMessage());
            logger.warn("O lembrete não pôde ser enviado. Verifique se o serviço do WhatsApp está rodando em {}", url);
        }
    }
}
