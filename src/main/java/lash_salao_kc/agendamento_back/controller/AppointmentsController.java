package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.domain.dto.CreateAppointmentRequest;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.service.AppointmentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentsController {

    private final AppointmentsService appointmentsService;

    /**
     * POST /appointments
     * Cria um novo agendamento com um ou mais serviços
     *
     * Request body:
     * {
     *   "serviceIds": ["uuid-do-servico-1", "uuid-do-servico-2"],
     *   "date": "2024-12-15",
     *   "startTime": "10:00",
     *   "userName": "João Silva",
     *   "userPhone": "5511999999999"
     * }
     *
     * Processo:
     * 1. Busca todos os serviços selecionados
     * 2. Calcula endTime = startTime + soma das durações dos serviços
     * 3. Valida se está dentro do horário de funcionamento (09:00 - 18:00)
     * 4. Valida se não conflita com outros agendamentos
     * 5. Salva o agendamento (esse período fica indisponível)
     *
     * @param request Dados do agendamento
     * @return Agendamento criado (201 Created)
     */
    @PostMapping
    public ResponseEntity<AppointmentsEntity> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentsEntity appointment = appointmentsService.createAppointment(
                request.getServiceIds(),
                request.getDate(),
                request.getStartTime(),
                request.getUserName(),
                request.getUserPhone(),
                request.getClienteId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    /**
     * GET /appointments/available-slots?date=2024-12-15
     * Retorna todos os horários disponíveis para uma data específica
     * Horários pulam de 30 em 30 minutos (09:00, 09:30, 10:00, ..., 18:00)
     *
     * @param date Data no formato YYYY-MM-DD (exemplo: 2024-12-15)
     * @return Lista de horários disponíveis (LocalTime)
     */
    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<LocalTime> availableSlots = appointmentsService.getAvailableTimeSlots(date);
        return ResponseEntity.ok(availableSlots);
    }

    /**
     * GET /appointments/future?userPhone={phone}
     * Retorna todos os agendamentos futuros de um número de telefone
     * Considera como "futuros" os agendamentos cuja data é >= data atual
     *
     * @param userPhone Número de telefone do usuário
     * @return Lista de agendamentos futuros ordenados por data e hora
     */
    @GetMapping("/future")
    public ResponseEntity<List<AppointmentsEntity>> getFutureAppointments(
            @RequestParam String userPhone) {
        List<AppointmentsEntity> appointments = appointmentsService.getFutureAppointmentsByPhone(userPhone);
        return ResponseEntity.ok(appointments);
    }

    /**
     * POST /appointments/past
     * Retorna todos os agendamentos passados de um número de telefone
     * Considera como "passados" os agendamentos cuja data é < data atual
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "userPhone": "5511999999999"
     * }
     *
     * @param request Dados da requisição (tenantId e userPhone)
     * @return Lista de agendamentos passados ordenados por data decrescente (mais recente primeiro)
     */
    @PostMapping("/past")
    public ResponseEntity<List<AppointmentsEntity>> getPastAppointments(@Valid @RequestBody GetAppointmentsByPhoneRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        List<AppointmentsEntity> appointments = appointmentsService.getPastAppointmentsByPhone(request.getUserPhone());
        return ResponseEntity.ok(appointments);
    }

    /**
     * POST /appointments/list
     * Lista agendamentos. Se passar date filtra por data, senão retorna todos
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "date": "2024-12-15" (opcional)
     * }
     *
     * @param request Dados da requisição (tenantId e data opcional)
     * @return Lista de agendamentos
     */
    @PostMapping("/list")
    public ResponseEntity<List<AppointmentsEntity>> getAppointments(@Valid @RequestBody GetAppointmentsByDateRequest request) {
        TenantContext.setTenantId(request.getTenantId());
        if (request.getDate() != null) {
            List<AppointmentsEntity> appointments = appointmentsService.getAppointmentsByDate(request.getDate());
            return ResponseEntity.ok(appointments);
        } else {
            List<AppointmentsEntity> appointments = appointmentsService.getAllAppointments();
            return ResponseEntity.ok(appointments);
        }
    }

    /**
     * POST /appointments/by-id
     * Busca um agendamento específico por ID
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "appointmentId": "uuid-do-agendamento"
     * }
     *
     * @param request Dados da requisição com appointmentId no formato String
     * @return Agendamento encontrado (200 OK)
     * @throws RuntimeException se o agendamento não for encontrado (retorna 500)
     */
    @PostMapping("/by-id")
    public ResponseEntity<AppointmentsEntity> getAppointmentById(@Valid @RequestBody TenantIdWithId request) {
        TenantContext.setTenantId(request.getTenantId());
        AppointmentsEntity appointment = appointmentsService.getAppointmentById(java.util.UUID.fromString(request.getId()));
        return ResponseEntity.ok(appointment);
    }

    /**
     * POST /appointments/cancel
     * Cancela um agendamento existente
     * Remove o agendamento do banco de dados, liberando o horário para novos agendamentos
     *
     * Request body:
     * {
     *   "tenantId": "cliente1",
     *   "appointmentId": "uuid-do-agendamento"
     * }
     *
     * @param request Dados da requisição com appointmentId
     * @return 204 No Content se cancelado com sucesso
     * @throws RuntimeException se o agendamento não for encontrado (retorna 500)
     */
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelAppointment(@Valid @RequestBody TenantIdWithId request) {
        TenantContext.setTenantId(request.getTenantId());
        appointmentsService.cancelAppointment(java.util.UUID.fromString(request.getId()));
        return ResponseEntity.noContent().build();
    }
}

