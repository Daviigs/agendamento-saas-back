package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.dto.Whats;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;

/**
 * Serviço responsável pela integração com API de WhatsApp.
 * Envia notificações de agendamento e lembretes automáticos.
 */
@Slf4j
@Service
public class WhatsappService {

    private static final String WHATSAPP_BASE_URL = "http://localhost:3001/whatsapp";
    private static final String APPOINTMENT_ENDPOINT = "/agendamento";
    private static final String REMINDER_ENDPOINT = "/lembrete";
    private static final String CANCELAMENTO_ENDPOINT = "/cancelamento";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envia notificação de agendamento criado via WhatsApp.
     *
     * @param dto Dados do agendamento para envio
     * @throws RuntimeException se houver erro na comunicação com a API
     */
    public void enviarAgendamento(Whats dto) {
        String url = WHATSAPP_BASE_URL + APPOINTMENT_ENDPOINT;

        try {
            restTemplate.postForEntity(url, dto, String.class);
            log.info("Mensagem de agendamento enviada com sucesso para {}", dto.getTelefone());
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem de agendamento: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Envia lembrete de agendamento próximo via WhatsApp.
     * Usado pelo scheduler automático.
     *
     * @param appointment Agendamento para o qual enviar lembrete
     * @throws RuntimeException se houver erro na comunicação com a API
     */
    public void enviarLembrete(AppointmentsEntity appointment) {
        String url = WHATSAPP_BASE_URL + REMINDER_ENDPOINT;

        String telefoneNormalizado = normalizarTelefone(appointment.getUserPhone());
        String servicosNomes = concatenarNomesServicos(appointment);
        double valorTotal = calcularValorTotal(appointment);
        String valorFormatado = formatarMoeda(valorTotal);

        Whats dto = buildReminderDto(appointment, telefoneNormalizado, servicosNomes, valorFormatado);

        try {
            restTemplate.postForEntity(url, dto, String.class);
            log.info("Lembrete enviado com sucesso para {}", appointment.getUserName());
        } catch (Exception e) {
            log.error("Erro ao enviar lembrete: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Envia notificação de cancelamento de agendamento via WhatsApp.
     *
     * @param appointment Agendamento cancelado
     * @throws RuntimeException se houver erro na comunicação com a API
     */
    public void enviarCancelamento(AppointmentsEntity appointment) {
        String url = WHATSAPP_BASE_URL + CANCELAMENTO_ENDPOINT;

        String telefoneNormalizado = normalizarTelefone(appointment.getUserPhone());
        String servicosNomes = concatenarNomesServicos(appointment);
        double valorTotal = calcularValorTotal(appointment);
        String valorFormatado = formatarMoeda(valorTotal);

        Whats dto = buildCancelamentoDto(appointment, telefoneNormalizado, servicosNomes, valorFormatado);

        try {
            restTemplate.postForEntity(url, dto, String.class);
            log.info("Mensagem de cancelamento enviada com sucesso para {}", appointment.getUserName());
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem de cancelamento: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Normaliza número de telefone removendo o prefixo '+' se presente.
     *
     * @param telefone Telefone original (ex: "+5511999999999")
     * @return Telefone normalizado (ex: "5511999999999")
     */
    private String normalizarTelefone(String telefone) {
        return telefone.startsWith("+") ? telefone.substring(1) : telefone;
    }

    /**
     * Concatena os nomes dos serviços de um agendamento.
     *
     * @param appointment Agendamento com lista de serviços
     * @return Nomes concatenados separados por vírgula
     */
    private String concatenarNomesServicos(AppointmentsEntity appointment) {
        return appointment.getServices().stream()
                .map(service -> service.getName())
                .reduce((s1, s2) -> s1 + ", " + s2)
                .orElse("");
    }

    /**
     * Calcula o valor total dos serviços de um agendamento.
     *
     * @param appointment Agendamento com lista de serviços
     * @return Valor total em reais
     */
    private double calcularValorTotal(AppointmentsEntity appointment) {
        return appointment.getServices().stream()
                .mapToDouble(service -> service.getPrice())
                .sum();
    }

    /**
     * Formata valor monetário no padrão brasileiro (R$ XX,XX).
     *
     * @param value Valor a ser formatado
     * @return String formatada no padrão brasileiro
     */
    private String formatarMoeda(double value) {
        return String.format("R$ %.2f", value).replace(".", ",");
    }

    /**
     * Constrói DTO de lembrete a partir do agendamento.
     */
    private Whats buildReminderDto(AppointmentsEntity appointment, String telefone, String servicos, String valor) {
        Whats dto = new Whats();
        dto.setTelefone(telefone);
        dto.setNome(appointment.getUserName());
        dto.setData(appointment.getDate().format(DATE_FORMATTER));
        dto.setHora(appointment.getStartTime().format(TIME_FORMATTER));
        dto.setServico(servicos);
        dto.setClienteId(appointment.getTenantId());
        dto.setValor(valor);
        return dto;
    }

    /**
     * Constrói DTO de cancelamento a partir do agendamento.
     */
    private Whats buildCancelamentoDto(AppointmentsEntity appointment, String telefone, String servicos, String valor) {
        Whats dto = new Whats();
        dto.setTelefone(telefone);
        dto.setNome(appointment.getUserName());
        dto.setData(appointment.getDate().format(DATE_FORMATTER));
        dto.setHora(appointment.getStartTime().format(TIME_FORMATTER));
        dto.setServico(servicos);
        dto.setClienteId(appointment.getTenantId());
        dto.setValor(valor);
        return dto;
    }
}

