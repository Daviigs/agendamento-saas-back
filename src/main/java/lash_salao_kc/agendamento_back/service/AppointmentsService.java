package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.dto.Whats;
import lash_salao_kc.agendamento_back.domain.entity.AppointmentsEntity;
import lash_salao_kc.agendamento_back.domain.entity.ProfessionalEntity;
import lash_salao_kc.agendamento_back.domain.entity.ServicesEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.exception.AppointmentConflictException;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.exception.ResourceNotFoundException;
import lash_salao_kc.agendamento_back.repository.AppointmentsRepository;
import lash_salao_kc.agendamento_back.repository.ProfessionalRepository;
import lash_salao_kc.agendamento_back.repository.ServicesRepository;
import lash_salao_kc.agendamento_back.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pela gestão de agendamentos.
 * Implementa regras de negócio para criação, consulta e cancelamento de agendamentos,
 * incluindo validações de horário, conflitos e integração com WhatsApp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentsService {

    private final AppointmentsRepository appointmentsRepository;
    private final ServicesRepository servicesRepository;
    private final WhatsappService whatsAppService;
    private final BlockedDayService blockedDayService;
    private final AvailableTimeSlotsService availableTimeSlotsService;
    private final TenantWorkingHoursService workingHoursService;
    private final BlockedTimeSlotService blockedTimeSlotService;
    private final ProfessionalRepository professionalRepository;
    private final TenantRepository tenantRepository;
    private final ProfessionalServiceService professionalServiceService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Retorna todos os horários disponíveis para agendamento de um profissional em uma data específica.
     * Utiliza o novo sistema que considera:
     * - Horário de trabalho do profissional
     * - Bloqueios de horários do profissional
     * - Bloqueios recorrentes do profissional
     * - Bloqueios de dia inteiro
     * - Agendamentos existentes do profissional
     *
     * @param professionalId ID do profissional
     * @param date Data para consulta de horários disponíveis
     * @return Lista de horários disponíveis
     */
    public List<LocalTime> getAvailableTimeSlots(UUID professionalId, LocalDate date) {
        String tenantId = TenantContext.getTenantId();

        // Valida tenant
        TenantEntity tenant = tenantRepository.findByTenantKeyAndActiveTrue(tenantId)
                .orElseThrow(() -> new BusinessException(
                        String.format("Tenant '%s' não encontrado ou inativo", tenantId)));

        // Valida profissional pertence ao tenant e está ativo
        ProfessionalEntity professional = professionalRepository
                .findActiveByIdAndTenantId(professionalId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                        "Profissional não encontrado, inativo ou não pertence ao tenant"));

        return availableTimeSlotsService.getAvailableTimeSlotsForProfessional(professionalId, date);
    }

    /**
     * Busca todos os agendamentos de uma data específica do tenant atual.
     *
     * @param date Data para filtrar agendamentos
     * @return Lista de agendamentos da data
     */
    public List<AppointmentsEntity> getAppointmentsByDate(LocalDate date) {
        String tenantId = TenantContext.getTenantId();
        return appointmentsRepository.findByTenantIdAndDate(tenantId, date);
    }

    /**
     * Busca todos os agendamentos do tenant atual.
     *
     * @return Lista com todos os agendamentos
     */
    public List<AppointmentsEntity> getAllAppointments() {
        String tenantId = TenantContext.getTenantId();
        return appointmentsRepository.findByTenantId(tenantId);
    }

    /**
     * Busca um agendamento específico por ID.
     *
     * @param appointmentId ID do agendamento
     * @return Agendamento encontrado
     * @throws ResourceNotFoundException se o agendamento não for encontrado
     */
    public AppointmentsEntity getAppointmentById(UUID appointmentId) {
        return appointmentsRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", appointmentId));
    }

    /**
     * Busca todos os agendamentos futuros (incluindo hoje) de um cliente por telefone.
     * Retorna ordenado por data e hora crescente.
     *
     * @param userPhone Número de telefone do cliente
     * @return Lista de agendamentos futuros do cliente
     */
    public List<AppointmentsEntity> getFutureAppointmentsByPhone(String userPhone) {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now();

        return appointmentsRepository.findByTenantIdAndUserPhone(tenantId, userPhone).stream()
                .filter(appointment -> !appointment.getDate().isBefore(today))
                .sorted(this::compareAppointmentsByDateAndTime)
                .toList();
    }

    /**
     * Busca todos os agendamentos passados de um cliente por telefone.
     * Retorna ordenado por data e hora decrescente (mais recentes primeiro).
     *
     * @param userPhone Número de telefone do cliente
     * @return Lista de agendamentos passados do cliente
     */
    public List<AppointmentsEntity> getPastAppointmentsByPhone(String userPhone) {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now();

        return appointmentsRepository.findByTenantIdAndUserPhone(tenantId, userPhone).stream()
                .filter(appointment -> appointment.getDate().isBefore(today))
                .sorted(this::compareAppointmentsByDateAndTimeDescending)
                .toList();
    }

    /**
     * Compara agendamentos por data e hora (ordem crescente).
     */
    private int compareAppointmentsByDateAndTime(AppointmentsEntity a1, AppointmentsEntity a2) {
        int dateComparison = a1.getDate().compareTo(a2.getDate());
        return dateComparison != 0 ? dateComparison : a1.getStartTime().compareTo(a2.getStartTime());
    }

    /**
     * Compara agendamentos por data e hora (ordem decrescente).
     */
    private int compareAppointmentsByDateAndTimeDescending(AppointmentsEntity a1, AppointmentsEntity a2) {
        int dateComparison = a2.getDate().compareTo(a1.getDate());
        return dateComparison != 0 ? dateComparison : a2.getStartTime().compareTo(a1.getStartTime());
    }

    /**
     * Cria um novo agendamento com um ou mais serviços.
     *
     * Validações realizadas:
     * - Tenant existe e está ativo
     * - Profissional pertence ao tenant
     * - Profissional está ativo
     * - Profissional possui horário configurado
     * - Data não está bloqueada
     * - Serviços existem
     * - Horário está dentro do expediente do profissional
     * - Não há bloqueios de horário para o profissional
     * - Não há conflito com outros agendamentos do profissional
     *
     * Após criação bem-sucedida, envia notificação via WhatsApp.
     *
     * @param professionalId ID do profissional
     * @param serviceIds     Lista de IDs dos serviços a serem agendados
     * @param date           Data do agendamento
     * @param startTime      Horário de início
     * @param userName       Nome do cliente
     * @param userPhone      Telefone do cliente
     * @param clienteId      ID do tenant (cliente)
     * @return Agendamento criado e salvo
     * @throws BusinessException             se validações falharem
     * @throws ResourceNotFoundException     se algum recurso não for encontrado
     * @throws AppointmentConflictException se houver conflito de horário
     */
    @Transactional
    public AppointmentsEntity createAppointment(
            UUID professionalId,
            List<UUID> serviceIds,
            LocalDate date,
            LocalTime startTime,
            String userName,
            String userPhone,
            String clienteId) {

        // Valida tenant
        TenantEntity tenant = tenantRepository.findByTenantKeyAndActiveTrue(clienteId)
                .orElseThrow(() -> new BusinessException(
                        String.format("Tenant '%s' não encontrado ou inativo", clienteId)));

        // Valida profissional pertence ao tenant e está ativo
        ProfessionalEntity professional = professionalRepository
                .findActiveByIdAndTenantId(professionalId, tenant.getId())
                .orElseThrow(() -> new BusinessException(
                        "Profissional não encontrado, inativo ou não pertence ao tenant"));

        validateDateNotBlocked(date);

        List<ServicesEntity> services = fetchServices(serviceIds);

        // NOVA REGRA: Valida se o profissional executa TODOS os serviços
        validateProfessionalExecutesServices(professionalId, serviceIds);

        int totalDuration = calculateTotalDuration(services);
        LocalTime endTime = startTime.plusMinutes(totalDuration);

        validateBusinessHours(startTime, endTime);
        validateNoTimeSlotBlocks(date, startTime, endTime);
        validateNoConflicts(professionalId, date, startTime, endTime);

        AppointmentsEntity appointment = buildAppointment(
                date, startTime, endTime, services, userName, userPhone, clienteId, professional
        );

        sendWhatsappNotification(appointment, services, clienteId);

        log.info("Salvando agendamento no banco...");
        AppointmentsEntity savedAppointment = appointmentsRepository.save(appointment);
        log.info("Agendamento salvo com sucesso! ID: {}", savedAppointment.getId());

        return savedAppointment;
    }

    /**
     * Valida se a data não está bloqueada.
     *
     * @throws BusinessException se a data estiver bloqueada
     */
    private void validateDateNotBlocked(LocalDate date) {
        if (blockedDayService.isDateBlocked(date)) {
            throw new BusinessException("Não é possível agendar nesta data. O salão estará fechado.");
        }
    }

    /**
     * Valida se o profissional executa TODOS os serviços do agendamento.
     * REGRA DE NEGÓCIO NOVA: Profissionais apenas executam serviços vinculados a eles.
     *
     * @param professionalId ID do profissional
     * @param serviceIds     Lista de IDs dos serviços
     * @throws BusinessException se o profissional não executar algum serviço
     */
    private void validateProfessionalExecutesServices(UUID professionalId, List<UUID> serviceIds) {
        // Se não há vínculos configurados ainda (sistema legado), permite o agendamento
        // Isso garante retrocompatibilidade
        if (!professionalServiceService.professionalExecutesAllServices(professionalId, serviceIds)) {
            log.warn("Profissional {} não executa todos os serviços solicitados: {}",
                    professionalId, serviceIds);
            throw new BusinessException(
                    "O profissional selecionado não está habilitado para executar todos os serviços deste agendamento. " +
                    "Por favor, selecione outro profissional ou ajuste os serviços.");
        }
    }

    /**
     * Valida se não há bloqueios de horário no período desejado.
     *
     * @throws BusinessException se houver bloqueio de horário
     */
    private void validateNoTimeSlotBlocks(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (blockedTimeSlotService.isIntervalBlocked(date, startTime, endTime)) {
            throw new BusinessException(
                    String.format("Não é possível agendar entre %s e %s. Este horário está bloqueado.",
                            startTime, endTime));
        }
    }

    /**
     * Busca todos os serviços pelos IDs informados.
     *
     * @throws ResourceNotFoundException se algum serviço não for encontrado
     */
    private List<ServicesEntity> fetchServices(List<UUID> serviceIds) {
        List<ServicesEntity> services = new ArrayList<>();

        for (UUID serviceId : serviceIds) {
            ServicesEntity service = servicesRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Serviço", serviceId));
            services.add(service);
        }

        return services;
    }

    /**
     * Calcula a duração total em minutos de múltiplos serviços.
     */
    private int calculateTotalDuration(List<ServicesEntity> services) {
        return services.stream()
                .mapToInt(ServicesEntity::getDuration)
                .sum();
    }

    /**
     * Calcula o valor total em reais de múltiplos serviços.
     */
    private double calculateTotalValue(List<ServicesEntity> services) {
        return services.stream()
                .mapToDouble(ServicesEntity::getPrice)
                .sum();
    }

    /**
     * Formata valor monetário no padrão brasileiro (R$ XX,XX).
     */
    private String formatCurrency(double value) {
        return String.format("R$ %.2f", value).replace(".", ",");
    }

    /**
     * Constrói a entidade de agendamento com todos os dados necessários.
     */
    private AppointmentsEntity buildAppointment(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<ServicesEntity> services,
            String userName,
            String userPhone,
            String clienteId,
            ProfessionalEntity professional) {

        AppointmentsEntity appointment = new AppointmentsEntity();
        appointment.setTenantId(clienteId);
        appointment.setProfessional(professional);
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setServices(services);
        appointment.setUserName(userName);
        appointment.setUserPhone(userPhone);

        return appointment;
    }

    /**
     * Envia notificação de agendamento via WhatsApp.
     * Em caso de falha, apenas registra o erro sem impactar o agendamento.
     */
    private void sendWhatsappNotification(
            AppointmentsEntity appointment,
            List<ServicesEntity> services,
            String clienteId) {

        try {
            String telefoneParaWhatsapp = normalizePhoneNumber(appointment.getUserPhone());
            String servicosNomes = concatenateServiceNames(services);
            double totalValue = calculateTotalValue(services);
            String valorFormatado = formatCurrency(totalValue);

            Whats whatsDto = buildWhatsappDto(
                    telefoneParaWhatsapp,
                    appointment.getUserName(),
                    appointment.getDate(),
                    appointment.getStartTime(),
                    servicosNomes,
                    clienteId,
                    valorFormatado
            );

            log.info("Enviando mensagem WhatsApp para: {} (clienteId: {})",
                    telefoneParaWhatsapp, clienteId.toLowerCase());
            whatsAppService.enviarAgendamento(whatsDto);
            log.info("WhatsApp enviado com sucesso");
        } catch (Exception e) {
            log.error("Erro ao enviar WhatsApp (continuando com o agendamento): {}", e.getMessage());
        }
    }

    /**
     * Normaliza número de telefone removendo o prefixo '+' se presente.
     */
    private String normalizePhoneNumber(String phone) {
        return phone.startsWith("+") ? phone.substring(1) : phone;
    }

    /**
     * Concatena nomes de múltiplos serviços separados por vírgula.
     */
    private String concatenateServiceNames(List<ServicesEntity> services) {
        return services.stream()
                .map(ServicesEntity::getName)
                .reduce((s1, s2) -> s1 + ", " + s2)
                .orElse("");
    }

    /**
     * Constrói objeto DTO para envio de mensagem WhatsApp.
     */
    private Whats buildWhatsappDto(
            String telefone,
            String nome,
            LocalDate date,
            LocalTime time,
            String servico,
            String clienteId,
            String valor) {

        Whats whatsDto = new Whats();
        whatsDto.setTelefone(telefone);
        whatsDto.setNome(nome);
        whatsDto.setData(date.format(DATE_FORMATTER));
        whatsDto.setHora(time.format(TIME_FORMATTER));
        whatsDto.setServico(servico);
        whatsDto.setClienteId(clienteId.toLowerCase());
        whatsDto.setValor(valor);

        return whatsDto;
    }

    /**
     * Valida se o horário está dentro do expediente do tenant.
     * Utiliza o horário de trabalho configurado para o tenant.
     *
     * @throws BusinessException se o horário for inválido
     */
    private void validateBusinessHours(LocalTime startTime, LocalTime endTime) {
        String tenantId = TenantContext.getTenantId();

        if (!workingHoursService.isIntervalWithinWorkingHours(startTime, endTime, tenantId)) {
            var workingHours = workingHoursService.getWorkingHours(tenantId);
            throw new BusinessException(
                    String.format("Horário de agendamento (%s às %s) está fora do expediente de trabalho (%s às %s)",
                            startTime, endTime, workingHours.getStartTime(), workingHours.getEndTime()));
        }
    }

    /**
     * Valida que não há conflitos de horário para o profissional específico.
     *
     * @throws AppointmentConflictException se houver conflito de horário
     */
    private void validateNoConflicts(UUID professionalId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        // Busca apenas agendamentos do profissional específico na data
        List<AppointmentsEntity> existingAppointments = appointmentsRepository
                .findByProfessionalIdAndDate(professionalId, date);

        for (AppointmentsEntity existing : existingAppointments) {
            if (hasTimeConflict(startTime, endTime, existing)) {
                throw new AppointmentConflictException(
                        startTime, endTime,
                        existing.getStartTime(), existing.getEndTime(),
                        existing.getUserName());
            }
        }
    }

    /**
     * Verifica se há conflito de horário entre dois agendamentos.
     */
    private boolean hasTimeConflict(LocalTime startTime, LocalTime endTime, AppointmentsEntity existing) {
        return startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime());
    }

    /**
     * Cancela um agendamento existente.
     *
     * @param appointmentId ID do agendamento a ser cancelado
     * @throws ResourceNotFoundException se o agendamento não for encontrado
     */
    @Transactional
    public void cancelAppointment(UUID appointmentId) {
        AppointmentsEntity appointment = getAppointmentById(appointmentId);

        // Envia notificação de cancelamento via WhatsApp
        try {
            whatsAppService.enviarCancelamento(appointment);
            log.info("Notificação de cancelamento enviada para {}", appointment.getUserPhone());
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de cancelamento (prosseguindo com cancelamento): {}", e.getMessage());
        }

        appointmentsRepository.delete(appointment);
    }
}



