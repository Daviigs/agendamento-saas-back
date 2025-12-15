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
     * Cria um novo agendamento
     *
     * Request body:
     * {
     *   "serviceId": "uuid-do-servico",
     *   "date": "2024-12-15",
     *   "startTime": "10:00",
     *   "userName": "João Silva"
     * }
     *
     * Processo:
     * 1. Busca o serviço selecionado
     * 2. Calcula endTime = startTime + duração do serviço
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
                request.getServiceId(),
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
}

