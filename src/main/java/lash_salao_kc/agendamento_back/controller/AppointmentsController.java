package lash_salao_kc.agendamento_back.controller;

import jakarta.validation.Valid;
import lash_salao_kc.agendamento_back.config.TenantContext;
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
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentsController {

    private final AppointmentsService appointmentsService;

    /**
     * POST /appointments
     * Cria um novo agendamento com um ou mais serviços
     */
    @PostMapping
    public ResponseEntity<AppointmentsEntity> createAppointment(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateAppointmentRequest request) {
        TenantContext.setTenantId(tenantId);
        AppointmentsEntity appointment = appointmentsService.createAppointment(
                request.getServiceIds(),
                request.getDate(),
                request.getStartTime(),
                request.getUserName(),
                request.getUserPhone(),
                tenantId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    /**
     * GET /appointments/available-slots?date=2026-01-15
     * Retorna todos os horários disponíveis para uma data específica
     */
    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TenantContext.setTenantId(tenantId);
        List<LocalTime> availableSlots = appointmentsService.getAvailableTimeSlots(date);
        return ResponseEntity.ok(availableSlots);
    }

    /**
     * GET /appointments/future?userPhone=5511999999999
     * Retorna todos os agendamentos futuros de um número de telefone
     */
    @GetMapping("/future")
    public ResponseEntity<List<AppointmentsEntity>> getFutureAppointments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String userPhone) {
        TenantContext.setTenantId(tenantId);
        List<AppointmentsEntity> appointments = appointmentsService.getFutureAppointmentsByPhone(userPhone);
        return ResponseEntity.ok(appointments);
    }

    /**
     * GET /appointments/past?userPhone=5511999999999
     * Retorna todos os agendamentos passados de um número de telefone
     */
    @GetMapping("/past")
    public ResponseEntity<List<AppointmentsEntity>> getPastAppointments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String userPhone) {
        TenantContext.setTenantId(tenantId);
        List<AppointmentsEntity> appointments = appointmentsService.getPastAppointmentsByPhone(userPhone);
        return ResponseEntity.ok(appointments);
    }

    /**
     * GET /appointments
     * GET /appointments?date=2026-01-15
     * Lista agendamentos. Se passar ?date= filtra por data, senão retorna todos
     */
    @GetMapping
    public ResponseEntity<List<AppointmentsEntity>> getAppointments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TenantContext.setTenantId(tenantId);
        if (date != null) {
            List<AppointmentsEntity> appointments = appointmentsService.getAppointmentsByDate(date);
            return ResponseEntity.ok(appointments);
        } else {
            List<AppointmentsEntity> appointments = appointmentsService.getAllAppointments();
            return ResponseEntity.ok(appointments);
        }
    }

    /**
     * GET /appointments/{appointmentId}
     * Busca um agendamento específico por ID
     */
    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentsEntity> getAppointmentById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID appointmentId) {
        TenantContext.setTenantId(tenantId);
        AppointmentsEntity appointment = appointmentsService.getAppointmentById(appointmentId);
        return ResponseEntity.ok(appointment);
    }

    /**
     * DELETE /appointments/{appointmentId}
     * Cancela um agendamento existente
     */
    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> cancelAppointment(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID appointmentId) {
        TenantContext.setTenantId(tenantId);
        appointmentsService.cancelAppointment(appointmentId);
        return ResponseEntity.noContent().build();
    }
}

