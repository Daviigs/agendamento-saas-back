package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.dto.Whats;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;

@Service
public class WhatsappSerivce {
    private static final Logger logger = LoggerFactory.getLogger(WhatsappSerivce.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.client.id:default}")
    private String whatsappClientId;

    public void enviarAgendamento(Whats dto) {

        String url = "http://localhost:3001/whatsapp/agendamento";

        restTemplate.postForEntity(url, dto, String.class);
    }

    public void enviarLembrete(AppointmentsEntity appointment) {
        String url = "http://localhost:3001/whatsapp/lembrete";

        // Remove o "+" caso venha com "+55", mantém apenas "55"
        String telefoneParaWhatsapp = appointment.getUserPhone().startsWith("+")
            ? appointment.getUserPhone().substring(1)
            : appointment.getUserPhone();

        Whats dto = new Whats();
        dto.setClienteId(whatsappClientId);
        dto.setTelefone(telefoneParaWhatsapp);
        dto.setNome(appointment.getUserName());
        dto.setData(appointment.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dto.setHora(appointment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        dto.setServico(appointment.getService().getName());

        restTemplate.postForEntity(url, dto, String.class);
    }
}
