package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.CreateAppointmentRequest;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.service.AppointmentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de agendamentos.
 * Expõe endpoints para criação, consulta e cancelamento de agendamentos.
 *
 * NOTA: Não é necessário receber X-Tenant-Id nos métodos pois o TenantInterceptor
 * já valida e injeta o tenant no contexto antes dos métodos serem chamados.
 */
@Slf4j
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentsController extends BaseController {

    private final AppointmentsService appointmentsService;

    /**
     * Cria um novo agendamento.
     *
     * @param request Dados do agendamento
     * @return Agendamento criado (201 Created)
     */
    @PostMapping
    public ResponseEntity<AppointmentsEntity> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        log.info("=== Iniciando criação de agendamento ===");
        String tenantId = getTenantFromContext();
        log.info("Tenant ID: {}", tenantId);
        log.info("Request: professionalId={}, serviceIds={}, date={}, startTime={}, userName={}, userPhone={}",
                request.getProfessionalId(), request.getServiceIds(), request.getDate(), request.getStartTime(),
                request.getUserName(), request.getUserPhone());

        AppointmentsEntity appointment = appointmentsService.createAppointment(
                request.getProfessionalId(),
                request.getServiceIds(),
                request.getDate(),
                request.getStartTime(),
                request.getUserName(),
                request.getUserPhone(),
                tenantId
        );

        log.info("Agendamento criado com sucesso: ID={}", appointment.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    /**
     * Retorna horários disponíveis para agendamento de um profissional em uma data específica.
     *
     * @param professionalId ID do profissional
     * @param date Data para consulta
     * @return Lista de horários disponíveis (200 OK)
     */
    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<LocalTime> availableSlots = appointmentsService.getAvailableTimeSlots(professionalId, date);
        return ResponseEntity.ok(availableSlots);
    }

    /**
     * Retorna agendamentos futuros de um cliente por telefone.
     *
     * @param userPhone Telefone do cliente
     * @return Lista de agendamentos futuros (200 OK)
     */
    @GetMapping("/future")
    public ResponseEntity<List<AppointmentsEntity>> getFutureAppointments(@RequestParam String userPhone) {
        List<AppointmentsEntity> appointments = appointmentsService.getFutureAppointmentsByPhone(userPhone);
        return ResponseEntity.ok(appointments);
    }

    /**
     * Retorna agendamentos passados de um cliente por telefone.
     *
     * @param userPhone Telefone do cliente
     * @return Lista de agendamentos passados (200 OK)
     */
    @GetMapping("/past")
    public ResponseEntity<List<AppointmentsEntity>> getPastAppointments(@RequestParam String userPhone) {
        List<AppointmentsEntity> appointments = appointmentsService.getPastAppointmentsByPhone(userPhone);
        return ResponseEntity.ok(appointments);
    }

    /**
     * Retorna agendamentos do tenant.
     * Se informada uma data, filtra por essa data específica.
     *
     * @param date Data para filtro (opcional)
     * @return Lista de agendamentos (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<AppointmentsEntity>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AppointmentsEntity> appointments = date != null
                ? appointmentsService.getAppointmentsByDate(date)
                : appointmentsService.getAllAppointments();

        return ResponseEntity.ok(appointments);
    }

    /**
     * Retorna um agendamento específico por ID.
     *
     * @param appointmentId ID do agendamento
     * @return Agendamento encontrado (200 OK)
     */
    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentsEntity> getAppointmentById(@PathVariable UUID appointmentId) {
        AppointmentsEntity appointment = appointmentsService.getAppointmentById(appointmentId);
        return ResponseEntity.ok(appointment);
    }

    /**
     * Cancela um agendamento existente.
     *
     * @param appointmentId ID do agendamento a cancelar
     * @return Resposta vazia (204 No Content)
     */
    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> cancelAppointment(@PathVariable UUID appointmentId) {
        appointmentsService.cancelAppointment(appointmentId);
        return ResponseEntity.noContent().build();
    }
}

