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
                request.getUserPhone()
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
     * GET /appointments/past?userPhone={phone}
     * Retorna todos os agendamentos passados de um número de telefone
     * Considera como "passados" os agendamentos cuja data é < data atual
     *
     * @param userPhone Número de telefone do usuário
     * @return Lista de agendamentos passados ordenados por data decrescente (mais recente primeiro)
     */
    @GetMapping("/past")
    public ResponseEntity<List<AppointmentsEntity>> getPastAppointments(
            @RequestParam String userPhone) {
        List<AppointmentsEntity> appointments = appointmentsService.getPastAppointmentsByPhone(userPhone);
        return ResponseEntity.ok(appointments);
    }

    /**
     * GET /appointments
     * Lista agendamentos. Se passar ?date= filtra por data, senão retorna todos
     *
     * @param date Data opcional no formato YYYY-MM-DD
     * @return Lista de agendamentos
     */
    @GetMapping
    public ResponseEntity<List<AppointmentsEntity>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            List<AppointmentsEntity> appointments = appointmentsService.getAppointmentsByDate(date);
            return ResponseEntity.ok(appointments);
        } else {
            List<AppointmentsEntity> appointments = appointmentsService.getAllAppointments();
            return ResponseEntity.ok(appointments);
        }
    }

    /**
     * GET /appointments/id/{appointmentId}
     * Busca um agendamento específico por ID
     *
     * @param appointmentId ID do agendamento (UUID)
     * @return Agendamento encontrado (200 OK)
     * @throws RuntimeException se o agendamento não for encontrado (retorna 500)
     */
    @GetMapping("/id/{appointmentId}")
    public ResponseEntity<AppointmentsEntity> getAppointmentById(@PathVariable String appointmentId) {
        AppointmentsEntity appointment = appointmentsService.getAppointmentById(java.util.UUID.fromString(appointmentId));
        return ResponseEntity.ok(appointment);
    }

    /**
     * DELETE /appointments/{appointmentId}
     * Cancela um agendamento existente
     * Remove o agendamento do banco de dados, liberando o horário para novos agendamentos
     *
     * @param appointmentId ID do agendamento a ser cancelado (UUID)
     * @return 204 No Content se cancelado com sucesso
     * @throws RuntimeException se o agendamento não for encontrado (retorna 500)
     */
    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> cancelAppointment(@PathVariable String appointmentId) {
        appointmentsService.cancelAppointment(java.util.UUID.fromString(appointmentId));
        return ResponseEntity.noContent().build();
    }
}

