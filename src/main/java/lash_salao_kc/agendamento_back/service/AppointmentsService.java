package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.repository.AppoitmentsRepository;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentsService {

    private final AppoitmentsRepository appoitmentsRepository;
    private final ServicesRepository servicesRepository;

    // Horários de funcionamento do salão
    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);  // 09:00
    private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);   // 18:00
    private static final int SLOT_INTERVAL_MINUTES = 30;                 // Intervalo de 30 minutos

    /**
     * Retorna todos os horários disponíveis para uma data específica
     * Horários disponíveis são de 09:00 às 18:00, pulando de 30 em 30 minutos
     * Remove os horários que já possuem agendamentos
     *
     * @param date Data para verificar disponibilidade
     * @return Lista de horários disponíveis (LocalTime)
     */
    public List<LocalTime> getAvailableTimeSlots(LocalDate date) {
        // Buscar todos os agendamentos da data
        List<AppointmentsEntity> appointments = appoitmentsRepository.findAll().stream()
                .filter(appointment -> appointment.getDate().equals(date))
                .toList();

        // Gerar todos os horários possíveis (09:00 às 18:00, de 30 em 30 minutos)
        List<LocalTime> allPossibleSlots = generateAllTimeSlots();

        // Filtrar os horários que não estão ocupados
        List<LocalTime> availableSlots = new ArrayList<>();

        for (LocalTime slot : allPossibleSlots) {
            if (isSlotAvailable(slot, appointments)) {
                availableSlots.add(slot);
            }
        }

        return availableSlots;
    }

    /**
     * Gera todos os horários possíveis de 09:00 às 18:00, pulando de 30 em 30 minutos
     * Exemplo: 09:00, 09:30, 10:00, 10:30, ..., 17:30, 18:00
     */
    private List<LocalTime> generateAllTimeSlots() {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime currentSlot = BUSINESS_START;

        while (currentSlot.isBefore(BUSINESS_END) || currentSlot.equals(BUSINESS_END)) {
            slots.add(currentSlot);
            currentSlot = currentSlot.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return slots;
    }

    /**
     * Verifica se um horário específico está disponível
     * Um horário está disponível se não conflita com nenhum agendamento existente
     *
     * Um horário conflita se estiver entre o startTime e endTime de algum agendamento
     */
    private boolean isSlotAvailable(LocalTime slot, List<AppointmentsEntity> appointments) {
        for (AppointmentsEntity appointment : appointments) {
            LocalTime appointmentStart = appointment.getStartTime();
            LocalTime appointmentEnd = appointment.getEndTime();

            // Verifica se o slot está dentro do intervalo do agendamento
            // slot >= appointmentStart && slot < appointmentEnd
            if ((slot.equals(appointmentStart) || slot.isAfter(appointmentStart))
                && slot.isBefore(appointmentEnd)) {
                return false; // Horário está ocupado
            }
        }
        return true; // Horário está disponível
    }

    /**
     * Busca todos os agendamentos de uma data específica
     *
     * @param date Data para buscar agendamentos
     * @return Lista de agendamentos da data
     */
    public List<AppointmentsEntity> getAppointmentsByDate(LocalDate date) {
        return appoitmentsRepository.findAll().stream()
                .filter(appointment -> appointment.getDate().equals(date))
                .toList();
    }

    /**
     * Busca todos os agendamentos
     *
     * @return Lista de todos os agendamentos
     */
    public List<AppointmentsEntity> getAllAppointments() {
        return appoitmentsRepository.findAll();
    }

    /**
     * Cria um novo agendamento
     * - Busca o serviço selecionado no banco
     * - Calcula o endTime baseado na duração do serviço
     * - Valida se o horário está disponível (não conflita com outros agendamentos)
     * - Valida se está dentro do horário de funcionamento
     * - Salva o agendamento, tornando aquele período indisponível
     *
     * @param serviceId ID do serviço selecionado
     * @param date Data do agendamento
     * @param startTime Horário de início selecionado
     * @param userName Nome do usuário que está agendando
     * @param userPhone Número de telefone do usuário
     * @return Agendamento criado
     * @throws RuntimeException se o serviço não existir, horário estiver ocupado ou fora do expediente
     */
    @Transactional
    public AppointmentsEntity createAppointment(UUID serviceId, LocalDate date, LocalTime startTime, String userName, String userPhone) {
        // 1. Buscar o serviço no banco de dados
        ServicesEntity service = servicesRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado com ID: " + serviceId));

        // 2. Calcular o horário de término baseado na duração do serviço
        LocalTime endTime = startTime.plusMinutes(service.getDuration());

        // 3. Validar se está dentro do horário de funcionamento
        validateBusinessHours(startTime, endTime);

        // 4. Validar se o horário está disponível (não conflita com outros agendamentos)
        validateNoConflicts(date, startTime, endTime);

        // 5. Criar o agendamento
        AppointmentsEntity appointment = new AppointmentsEntity();
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setService(service);
        appointment.setUserName(userName);
        appointment.setUserPhone(userPhone);

        // 6. Salvar no banco (esse período agora fica indisponível)
        return appoitmentsRepository.save(appointment);
    }

    /**
     * Valida se o horário está dentro do horário de funcionamento do salão
     */
    private void validateBusinessHours(LocalTime startTime, LocalTime endTime) {
        if (startTime.isBefore(BUSINESS_START)) {
            throw new RuntimeException(
                    String.format("Horário de início %s está antes do horário de abertura (%s)",
                            startTime, BUSINESS_START));
        }

        if (endTime.isAfter(BUSINESS_END)) {
            throw new RuntimeException(
                    String.format("Horário de término %s excede o horário de fechamento (%s). " +
                            "O salão fecha às %s", endTime, BUSINESS_END, BUSINESS_END));
        }
    }

    /**
     * Valida se não há conflitos com agendamentos existentes
     * Dois agendamentos conflitam se: startA < endB AND startB < endA
     */
    private void validateNoConflicts(LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<AppointmentsEntity> existingAppointments = appoitmentsRepository.findAll().stream()
                .filter(appointment -> appointment.getDate().equals(date))
                .toList();

        for (AppointmentsEntity existing : existingAppointments) {
            // Verifica se há conflito: startTime < existing.endTime AND endTime > existing.startTime
            boolean hasConflict = startTime.isBefore(existing.getEndTime())
                    && endTime.isAfter(existing.getStartTime());

            if (hasConflict) {
                throw new RuntimeException(
                        String.format("Horário selecionado (%s - %s) conflita com agendamento existente (%s - %s) de %s",
                                startTime, endTime,
                                existing.getStartTime(), existing.getEndTime(),
                                existing.getUserName()));
            }
        }
    }

}

