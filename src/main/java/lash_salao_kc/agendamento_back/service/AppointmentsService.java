package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.dto.Whats;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.repository.AppoitmentsRepository;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentsService {

    private final AppoitmentsRepository appoitmentsRepository;
    private final ServicesRepository servicesRepository;
    private final WhatsappSerivce whatsAppService;
    private final BlockedDayService blockedDayService;

    // Horários de funcionamento do salão
    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);  // 09:00
    private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);   // 18:00
    private static final int SLOT_INTERVAL_MINUTES = 30;                 // Intervalo de 30 minutos

    /**
     * Retorna todos os horários disponíveis para uma data específica
     * Horários disponíveis são de 09:00 às 18:00, pulando de 30 em 30 minutos
     * Remove os horários que já possuem agendamentos
     * Retorna vazio se a data estiver bloqueada (feriado ou dia de folga)
     *
     * @param date Data para verificar disponibilidade
     * @return Lista de horários disponíveis (LocalTime) ou lista vazia se o dia estiver bloqueado
     */
    public List<LocalTime> getAvailableTimeSlots(LocalDate date) {
        // Verifica se a data está bloqueada
        if (blockedDayService.isDateBlocked(date)) {
            return new ArrayList<>(); // Retorna lista vazia se o dia estiver bloqueado
        }

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
     * Busca um agendamento específico por ID
     *
     * @param appointmentId ID do agendamento
     * @return Agendamento encontrado
     * @throws RuntimeException se o agendamento não for encontrado
     */
    public AppointmentsEntity getAppointmentById(UUID appointmentId) {
        return appoitmentsRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado com ID: " + appointmentId));
    }

    /**
     * Busca todos os agendamentos futuros de um número de telefone
     * Considera como "futuros" os agendamentos cuja data é maior ou igual à data atual
     *
     * @param userPhone Número de telefone do usuário
     * @return Lista de agendamentos futuros ordenados por data e hora
     */
    public List<AppointmentsEntity> getFutureAppointmentsByPhone(String userPhone) {
        LocalDate today = LocalDate.now();

        return appoitmentsRepository.findAll().stream()
                .filter(appointment -> appointment.getUserPhone().equals(userPhone))
                .filter(appointment -> appointment.getDate().isAfter(today) || appointment.getDate().equals(today))
                .sorted((a1, a2) -> {
                    // Ordena por data e depois por horário
                    int dateComparison = a1.getDate().compareTo(a2.getDate());
                    if (dateComparison != 0) {
                        return dateComparison;
                    }
                    return a1.getStartTime().compareTo(a2.getStartTime());
                })
                .toList();
    }

    /**
     * Busca todos os agendamentos passados de um número de telefone
     * Considera como "passados" os agendamentos cuja data é menor que a data atual
     *
     * @param userPhone Número de telefone do usuário
     * @return Lista de agendamentos passados ordenados por data e hora (mais recente primeiro)
     */
    public List<AppointmentsEntity> getPastAppointmentsByPhone(String userPhone) {
        LocalDate today = LocalDate.now();

        return appoitmentsRepository.findAll().stream()
                .filter(appointment -> appointment.getUserPhone().equals(userPhone))
                .filter(appointment -> appointment.getDate().isBefore(today))
                .sorted((a1, a2) -> {
                    // Ordena por data decrescente (mais recente primeiro) e depois por horário
                    int dateComparison = a2.getDate().compareTo(a1.getDate());
                    if (dateComparison != 0) {
                        return dateComparison;
                    }
                    return a2.getStartTime().compareTo(a1.getStartTime());
                })
                .toList();
    }

    /**
     * Cria um novo agendamento com múltiplos serviços
     * - Valida se a data não está bloqueada (feriado ou dia de folga)
     * - Busca todos os serviços selecionados
     * - Calcula o endTime baseado na soma das durações dos serviços
     * - Valida se o horário está disponível (não conflita com outros agendamentos)
     * - Valida se está dentro do horário de funcionamento
     * - Salva o agendamento, tornando aquele período indisponível
     *
     * @param serviceIds Lista de IDs dos serviços selecionados
     * @param date Data do agendamento
     * @param startTime Horário de início selecionado
     * @param userName Nome do usuário que está agendando
     * @param userPhone Número de telefone do usuário
     * @param clienteId ID do cliente (KC ou MJS)
     * @return Agendamento criado
     * @throws RuntimeException se o serviço não existir, horário estiver ocupado ou fora do expediente
     */
    @Transactional
    public AppointmentsEntity createAppointment(List<UUID> serviceIds, LocalDate date, LocalTime startTime, String userName, String userPhone, String clienteId) {
        // 1. Validar se a data está bloqueada (feriado ou dia de folga)
        if (blockedDayService.isDateBlocked(date)) {
            throw new RuntimeException("Não é possível agendar nesta data. O salão estará fechado.");
        }

        // 2. Buscar todos os serviços no banco de dados
        List<ServicesEntity> services = new ArrayList<>();
        int totalDuration = 0;

        for (UUID serviceId : serviceIds) {
            ServicesEntity service = servicesRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Serviço não encontrado com ID: " + serviceId));
            services.add(service);
            totalDuration += service.getDuration();
        }

        // 3. Calcular o horário de término baseado na soma das durações dos serviços
        LocalTime endTime = startTime.plusMinutes(totalDuration);

        // 4. Validar se está dentro do horário de funcionamento
        validateBusinessHours(startTime, endTime);

        // 5. Validar se o horário está disponível (não conflita com outros agendamentos)
        validateNoConflicts(date, startTime, endTime);

        // 6. Criar o agendamento
        AppointmentsEntity appointment = new AppointmentsEntity();
        appointment.setTenantId(tenantId);
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setServices(services);
        appointment.setUserName(userName);
        appointment.setUserPhone(userPhone);

        // 🔔 ENVIA WHATSAPP (simples)
        // Remove o "+" caso venha com "+55", mantém apenas "55"
        String telefoneParaWhatsapp = userPhone.startsWith("+") ? userPhone.substring(1) : userPhone;

        // Concatena os nomes dos serviços
        String servicosNomes = services.stream()
                .map(ServicesEntity::getName)
                .reduce((s1, s2) -> s1 + ", " + s2)
                .orElse("");

        Whats whatsDto = new Whats();
        whatsDto.setTelefone(telefoneParaWhatsapp);
        whatsDto.setNome(userName);
        whatsDto.setData(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        whatsDto.setHora(startTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        whatsDto.setServico(servicosNomes);
        whatsDto.setClienteId(tenantId);

        whatsAppService.enviarAgendamento(whatsDto);

        // 7. Salvar no banco (esse período agora fica indisponível)
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

    /**
     * Cancela um agendamento pelo ID
     * Remove o agendamento do banco de dados, liberando o horário para novos agendamentos
     *
     * @param appointmentId ID do agendamento a ser cancelado
     * @throws RuntimeException se o agendamento não for encontrado
     */
    @Transactional
    public void cancelAppointment(UUID appointmentId) {
        // 1. Buscar o agendamento no banco
        AppointmentsEntity appointment = appoitmentsRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado com ID: " + appointmentId));

        // 2. Deletar o agendamento (libera o horário)
        appoitmentsRepository.delete(appointment);
    }

}

