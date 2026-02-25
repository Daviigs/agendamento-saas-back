package lash_salao_kc.agendamento_back.scheduler;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.repository.AppointmentsRepository;
import lash_salao_kc.agendamento_back.service.TenantDateTimeService;
import lash_salao_kc.agendamento_back.service.TenantService;
import lash_salao_kc.agendamento_back.service.WhatsappService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AppointmentReminderScheduler.
 *
 * Cenários cobertos:
 * - Diferentes timezones (America/Sao_Paulo, America/New_York, Europe/London, UTC)
 * - Diferentes antecedências (30min, 60min, 120min, 1440min)
 * - Casos de borda: meia-noite, mudança de dia, appointment exato na janela
 * - Idempotência: não reenvia se reminderSent = true
 * - Segurança: bloqueia envio se diferença real foge da janela esperada
 */
@ExtendWith(MockitoExtension.class)
class AppointmentReminderSchedulerTest {

    @Mock
    private AppointmentsRepository appointmentsRepository;

    @Mock
    private WhatsappService whatsappService;

    @Mock
    private TenantService tenantService;

    @Mock
    private TenantDateTimeService tenantDateTimeService;

    @InjectMocks
    private AppointmentReminderScheduler scheduler;

    private TenantEntity tenant;

    @BeforeEach
    void setUp() {
        tenant = new TenantEntity();
        tenant.setId(UUID.randomUUID());
        tenant.setTenantKey("kc");
        tenant.setBusinessName("KC Lash Studio");
        tenant.setActive(true);
        tenant.setTimezone("America/Sao_Paulo");
        tenant.setTempoLembreteMinutos(120);
    }

    /**
     * Cria um AppointmentsEntity de teste.
     */
    private AppointmentsEntity createAppointment(LocalDate date, LocalTime startTime) {
        AppointmentsEntity appointment = new AppointmentsEntity();
        appointment.setId(UUID.randomUUID());
        appointment.setTenantId("kc");
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(startTime.plusMinutes(60));
        appointment.setUserName("Maria");
        appointment.setUserPhone("5511999999999");
        appointment.setReminderSent(false);
        return appointment;
    }

    /**
     * Configura os mocks padrão para um cenário de teste.
     */
    private void setupMocks(ZonedDateTime nowZoned, List<AppointmentsEntity> appointments) {
        when(tenantService.getAllActiveTenants()).thenReturn(List.of("kc"));
        when(tenantService.getTenantByKey("kc")).thenReturn(tenant);
        when(tenantDateTimeService.now("kc")).thenReturn(nowZoned);

        // O repository recebe os parâmetros da janela calculada; retorna a lista fornecida
        when(appointmentsRepository.findAppointmentsToRemind(
                eq("kc"), any(LocalDate.class), any(LocalTime.class),
                any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(appointments);

        when(appointmentsRepository.save(any(AppointmentsEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ========================================================================
    // TESTES DE TIMEZONE
    // ========================================================================

    @Nested
    @DisplayName("Testes de timezone")
    class TimezoneTests {

        @Test
        @DisplayName("Deve calcular corretamente para America/Sao_Paulo (UTC-3)")
        void shouldCalculateCorrectlyForSaoPaulo() {
            // Cenário: 11:00 em São Paulo, antecedência 120min → busca agendamentos às 13:00 SP
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 11, 0, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(13, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            // Deve enviar o lembrete (diff = 120min = exatamente a antecedência)
            verify(whatsappService, times(1)).enviarLembrete(appointment);
            verify(appointmentsRepository, times(1)).save(argThat(a -> a.isReminderSent()));
        }

        @Test
        @DisplayName("Deve calcular corretamente para America/New_York (UTC-5)")
        void shouldCalculateCorrectlyForNewYork() {
            tenant.setTimezone("America/New_York");
            tenant.setTempoLembreteMinutos(60);

            ZoneId zone = ZoneId.of("America/New_York");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 14, 0, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(15, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointment);
        }

        @Test
        @DisplayName("Deve calcular corretamente para Europe/London (UTC+0/+1)")
        void shouldCalculateCorrectlyForLondon() {
            tenant.setTimezone("Europe/London");
            tenant.setTempoLembreteMinutos(30);

            ZoneId zone = ZoneId.of("Europe/London");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 9, 30, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(10, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointment);
        }

        @Test
        @DisplayName("Deve calcular corretamente para UTC")
        void shouldCalculateCorrectlyForUTC() {
            tenant.setTimezone("UTC");
            tenant.setTempoLembreteMinutos(120);

            ZoneId zone = ZoneId.of("UTC");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 10, 0, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(12, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointment);
        }
    }

    // ========================================================================
    // TESTES DE DIFERENTES ANTECEDÊNCIAS
    // ========================================================================

    @Nested
    @DisplayName("Testes de antecedência")
    class AntecedenciaTests {

        @Test
        @DisplayName("Antecedência de 30 minutos")
        void shouldWorkWith30MinAntecedencia() {
            tenant.setTempoLembreteMinutos(30);

            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 14, 30, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(15, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointment);
        }

        @Test
        @DisplayName("Antecedência de 1440 minutos (24 horas)")
        void shouldWorkWith1440MinAntecedencia() {
            tenant.setTempoLembreteMinutos(1440);

            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 23, 10, 0, 0, 0, zone);

            // Agendamento amanhã às 10:00
            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(10, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointment);
        }
    }

    // ========================================================================
    // TESTES DE CASOS DE BORDA
    // ========================================================================

    @Nested
    @DisplayName("Casos de borda")
    class EdgeCaseTests {

        @Test
        @DisplayName("Agendamento à meia-noite (mudança de dia)")
        void shouldHandleMidnightAppointment() {
            tenant.setTempoLembreteMinutos(120);

            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            // 22:00 do dia 23 + 120min = 00:00 do dia 24
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 23, 22, 0, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(0, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointment);
        }

        @Test
        @DisplayName("Agendamento no último minuto do dia (23:59)")
        void shouldHandleEndOfDayAppointment() {
            tenant.setTempoLembreteMinutos(60);

            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 22, 59, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(23, 59));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointment);
        }

        @Test
        @DisplayName("Nenhum agendamento para lembrar — nenhum envio")
        void shouldNotSendWhenNoAppointments() {
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 11, 0, 0, 0, zone);

            setupMocks(now, Collections.emptyList());

            scheduler.sendReminders();

            verify(whatsappService, never()).enviarLembrete(any());
            verify(appointmentsRepository, never()).save(any());
        }
    }

    // ========================================================================
    // TESTES DE IDEMPOTÊNCIA
    // ========================================================================

    @Nested
    @DisplayName("Testes de idempotência")
    class IdempotencyTests {

        @Test
        @DisplayName("Não busca agendamentos com reminderSent=true (filtrado pela query)")
        void shouldNotFetchAlreadySentReminders() {
            // A query no repository filtra reminderSent = false.
            // Se o repository retorna lista vazia, nenhum envio ocorre.
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 11, 0, 0, 0, zone);

            setupMocks(now, Collections.emptyList());

            scheduler.sendReminders();

            verify(whatsappService, never()).enviarLembrete(any());
        }

        @Test
        @DisplayName("Marca reminderSent=true após envio bem-sucedido")
        void shouldMarkReminderSentAfterSuccess() {
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 11, 0, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(13, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            ArgumentCaptor<AppointmentsEntity> captor = ArgumentCaptor.forClass(AppointmentsEntity.class);
            verify(appointmentsRepository).save(captor.capture());
            assertTrue(captor.getValue().isReminderSent());
        }

        @Test
        @DisplayName("Não marca reminderSent se envio falhar")
        void shouldNotMarkReminderSentOnFailure() {
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 11, 0, 0, 0, zone);

            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(13, 0));

            setupMocks(now, List.of(appointment));
            doThrow(new RuntimeException("WhatsApp API error"))
                    .when(whatsappService).enviarLembrete(any());

            scheduler.sendReminders();

            // save nunca é chamado porque a exceção ocorre antes
            verify(appointmentsRepository, never()).save(any());
            assertFalse(appointment.isReminderSent());
        }
    }

    // ========================================================================
    // TESTES DE VERIFICAÇÃO DE SEGURANÇA (UTC)
    // ========================================================================

    @Nested
    @DisplayName("Testes de verificação de segurança UTC")
    class SafetyCheckTests {

        @Test
        @DisplayName("Bloqueia envio se diferença real está fora da janela esperada")
        void shouldBlockIfDiffOutsideExpectedWindow() {
            // Simula cenário onde o agendamento está a 300 minutos, mas antecedência é 120
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            ZonedDateTime now = ZonedDateTime.of(2026, 2, 24, 8, 0, 0, 0, zone);

            // Agendamento às 13:00 → diff = 300min, esperado = 120min
            // Isso não deveria acontecer normalmente, mas se a query retornar por erro,
            // a verificação de segurança bloqueia.
            AppointmentsEntity appointment = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(13, 0));

            setupMocks(now, List.of(appointment));

            scheduler.sendReminders();

            // A verificação UTC bloqueia: diff=300min, esperado=[118-122]min
            verify(whatsappService, never()).enviarLembrete(any());
        }
    }

    // ========================================================================
    // TESTES DE MÚLTIPLOS TENANTS
    // ========================================================================

    @Nested
    @DisplayName("Testes de múltiplos tenants")
    class MultiTenantTests {

        @Test
        @DisplayName("Processa múltiplos tenants com timezones diferentes")
        void shouldProcessMultipleTenantsWithDifferentTimezones() {
            TenantEntity tenantKc = new TenantEntity();
            tenantKc.setId(UUID.randomUUID());
            tenantKc.setTenantKey("kc");
            tenantKc.setBusinessName("KC");
            tenantKc.setActive(true);
            tenantKc.setTimezone("America/Sao_Paulo");
            tenantKc.setTempoLembreteMinutos(120);

            TenantEntity tenantMjs = new TenantEntity();
            tenantMjs.setId(UUID.randomUUID());
            tenantMjs.setTenantKey("mjs");
            tenantMjs.setBusinessName("MJS");
            tenantMjs.setActive(true);
            tenantMjs.setTimezone("America/New_York");
            tenantMjs.setTempoLembreteMinutos(60);

            when(tenantService.getAllActiveTenants()).thenReturn(List.of("kc", "mjs"));
            when(tenantService.getTenantByKey("kc")).thenReturn(tenantKc);
            when(tenantService.getTenantByKey("mjs")).thenReturn(tenantMjs);

            ZonedDateTime nowSP = ZonedDateTime.of(2026, 2, 24, 11, 0, 0, 0,
                    ZoneId.of("America/Sao_Paulo"));
            ZonedDateTime nowNY = ZonedDateTime.of(2026, 2, 24, 9, 0, 0, 0,
                    ZoneId.of("America/New_York"));

            when(tenantDateTimeService.now("kc")).thenReturn(nowSP);
            when(tenantDateTimeService.now("mjs")).thenReturn(nowNY);

            AppointmentsEntity appointmentKc = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(13, 0));
            appointmentKc.setTenantId("kc");

            AppointmentsEntity appointmentMjs = createAppointment(
                    LocalDate.of(2026, 2, 24), LocalTime.of(10, 0));
            appointmentMjs.setTenantId("mjs");

            when(appointmentsRepository.findAppointmentsToRemind(
                    eq("kc"), any(), any(), any(), any()))
                    .thenReturn(List.of(appointmentKc));

            when(appointmentsRepository.findAppointmentsToRemind(
                    eq("mjs"), any(), any(), any(), any()))
                    .thenReturn(List.of(appointmentMjs));

            when(appointmentsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            scheduler.sendReminders();

            verify(whatsappService, times(1)).enviarLembrete(appointmentKc);
            verify(whatsappService, times(1)).enviarLembrete(appointmentMjs);
        }
    }
}

