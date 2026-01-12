package lash_salao_kc.agendamento_back.config;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Constantes da aplicação centralizadas
 */
public final class AppConstants {

    private AppConstants() {
        // Classe utilitária - não deve ser instanciada
    }

    // Horários de funcionamento
    public static final LocalTime BUSINESS_START_TIME = LocalTime.of(9, 0);
    public static final LocalTime BUSINESS_END_TIME = LocalTime.of(18, 0);
    public static final LocalTime LAST_APPOINTMENT_START_TIME = LocalTime.of(16, 0);
    public static final int APPOINTMENT_SLOT_INTERVAL_MINUTES = 30;

    // Formatadores de data e hora
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // URLs de serviços externos
    public static final String WHATSAPP_BASE_URL = "http://localhost:3001/whatsapp";
    public static final String WHATSAPP_AGENDAMENTO_ENDPOINT = "/agendamento";
    public static final String WHATSAPP_LEMBRETE_ENDPOINT = "/lembrete";

    // Configurações de lembretes
    public static final int REMINDER_HOURS_BEFORE_APPOINTMENT = 2;
    public static final long REMINDER_SCHEDULER_INTERVAL_MS = 60000; // 1 minuto

    // Headers HTTP
    public static final String TENANT_HEADER_NAME = "X-Tenant-Id";
    public static final String CLIENT_HEADER_NAME = "X-Client-Id";
    public static final String DEFAULT_TENANT_ID = "default";
}

