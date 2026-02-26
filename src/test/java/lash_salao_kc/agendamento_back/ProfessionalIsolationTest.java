package lash_salao_kc.agendamento_back;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.*;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.repository.*;
import lash_salao_kc.agendamento_back.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para isolamento entre profissionais.
 *
 * Garante que:
 * 1. Bloqueios de um profissional NÃO afetam outros profissionais
 * 2. Agendamentos de um profissional NÃO impedem agendamentos de outros
 * 3. O último horário disponível é calculado corretamente para cada profissional
 */
@ExtendWith(MockitoExtension.class)
class ProfessionalIsolationTest {

    private static final String TEST_TENANT_ID = "barbearia-teste";
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 3, 2); // Uma segunda-feira

    // UUIDs fixos para os profissionais
    private static final UUID PROFESSIONAL_A_ID = UUID.randomUUID();
    private static final UUID PROFESSIONAL_B_ID = UUID.randomUUID();

    private ProfessionalEntity professionalA;
    private ProfessionalEntity professionalB;
    private TenantEntity tenant;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TEST_TENANT_ID);

        tenant = new TenantEntity();
        tenant.setId(UUID.randomUUID());

        professionalA = new ProfessionalEntity();
        professionalA.setId(PROFESSIONAL_A_ID);
        professionalA.setProfessionalName("Profissional A");
        professionalA.setTenant(tenant);
        professionalA.setActive(true);

        professionalB = new ProfessionalEntity();
        professionalB.setId(PROFESSIONAL_B_ID);
        professionalB.setProfessionalName("Profissional B");
        professionalB.setTenant(tenant);
        professionalB.setActive(true);
    }

    /**
     * Cria um TenantWorkingHoursEntity com horários padrão 09:00–19:00, intervalo 30 min, modo rígido.
     */
    private TenantWorkingHoursEntity createWorkingHours(LocalTime start, LocalTime end, int interval) {
        TenantWorkingHoursEntity wh = new TenantWorkingHoursEntity();
        wh.setTenantId(TEST_TENANT_ID);
        wh.setStartTime(start);
        wh.setEndTime(end);
        wh.setSlotIntervalMinutes(interval);
        wh.setActive(true);
        wh.setHorarioFlexivel(false);
        return wh;
    }

    /**
     * Cria um BlockedTimeSlotEntity para um profissional específico.
     */
    private BlockedTimeSlotEntity createBlock(ProfessionalEntity professional, LocalTime start, LocalTime end) {
        BlockedTimeSlotEntity block = new BlockedTimeSlotEntity();
        block.setId(UUID.randomUUID());
        block.setTenantId(TEST_TENANT_ID);
        block.setProfessional(professional);
        block.setStartTime(start);
        block.setEndTime(end);
        block.setReason("Bloqueio de teste");
        block.setRecurring(false);
        block.setSpecificDate(TEST_DATE);
        return block;
    }

    /**
     * Cria um AppointmentsEntity para um profissional específico.
     */
    private AppointmentsEntity createAppointment(ProfessionalEntity professional, LocalTime start, LocalTime end, String userName) {
        AppointmentsEntity appointment = new AppointmentsEntity();
        appointment.setId(UUID.randomUUID());
        appointment.setTenantId(TEST_TENANT_ID);
        appointment.setProfessional(professional);
        appointment.setDate(TEST_DATE);
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setUserName(userName);
        appointment.setUserPhone("11999999999");
        appointment.setServices(new ArrayList<>());
        return appointment;
    }

    // ===================================================================
    // TESTES DO BlockedTimeSlotService - Isolamento de bloqueios
    // ===================================================================

    @Nested
    @DisplayName("BlockedTimeSlotService - Isolamento por profissional")
    class BlockedTimeSlotServiceTests {

        @Mock
        private BlockedTimeSlotRepository blockedTimeSlotRepository;

        @Mock
        private TenantWorkingHoursService workingHoursService;

        @Mock
        private ProfessionalService professionalService;

        @InjectMocks
        private BlockedTimeSlotService blockedTimeSlotService;

        @Test
        @DisplayName("Bloqueio do Profissional A NÃO deve afetar Profissional B - isIntervalBlockedForProfessional")
        void blockFromProfessionalA_shouldNotAffectProfessionalB_interval() {
            // Arrange: Profissional A tem bloqueio das 10:00 às 11:00
            BlockedTimeSlotEntity blockA = createBlock(professionalA, LocalTime.of(10, 0), LocalTime.of(11, 0));

            when(blockedTimeSlotRepository.findByProfessionalIdAndSpecificDate(PROFESSIONAL_A_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>(List.of(blockA)));
            when(blockedTimeSlotRepository.findByProfessionalIdAndDayOfWeekAndRecurring(
                    PROFESSIONAL_A_ID, TEST_DATE.getDayOfWeek(), true))
                    .thenReturn(new ArrayList<>());

            // Profissional B não tem bloqueios
            when(blockedTimeSlotRepository.findByProfessionalIdAndSpecificDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(blockedTimeSlotRepository.findByProfessionalIdAndDayOfWeekAndRecurring(
                    PROFESSIONAL_B_ID, TEST_DATE.getDayOfWeek(), true))
                    .thenReturn(new ArrayList<>());

            // Act & Assert: Profissional A está bloqueado
            assertTrue(
                    blockedTimeSlotService.isIntervalBlockedForProfessional(
                            PROFESSIONAL_A_ID, TEST_DATE, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                    "Profissional A deveria estar bloqueado das 10:00 às 11:00"
            );

            // Act & Assert: Profissional B NÃO está bloqueado no mesmo horário
            assertFalse(
                    blockedTimeSlotService.isIntervalBlockedForProfessional(
                            PROFESSIONAL_B_ID, TEST_DATE, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                    "Profissional B NÃO deveria estar bloqueado - o bloqueio é do Profissional A"
            );
        }

        @Test
        @DisplayName("Bloqueio do Profissional A NÃO deve afetar Profissional B - isTimeSlotBlockedForProfessional")
        void blockFromProfessionalA_shouldNotAffectProfessionalB_timeSlot() {
            // Arrange: Profissional A tem bloqueio das 14:00 às 15:00
            BlockedTimeSlotEntity blockA = createBlock(professionalA, LocalTime.of(14, 0), LocalTime.of(15, 0));

            when(blockedTimeSlotRepository.findByProfessionalIdAndSpecificDate(PROFESSIONAL_A_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>(List.of(blockA)));
            when(blockedTimeSlotRepository.findByProfessionalIdAndDayOfWeekAndRecurring(
                    PROFESSIONAL_A_ID, TEST_DATE.getDayOfWeek(), true))
                    .thenReturn(new ArrayList<>());

            when(blockedTimeSlotRepository.findByProfessionalIdAndSpecificDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(blockedTimeSlotRepository.findByProfessionalIdAndDayOfWeekAndRecurring(
                    PROFESSIONAL_B_ID, TEST_DATE.getDayOfWeek(), true))
                    .thenReturn(new ArrayList<>());

            // Act & Assert
            assertTrue(
                    blockedTimeSlotService.isTimeSlotBlockedForProfessional(
                            PROFESSIONAL_A_ID, TEST_DATE, LocalTime.of(14, 30)),
                    "14:30 deveria estar bloqueado para Profissional A"
            );

            assertFalse(
                    blockedTimeSlotService.isTimeSlotBlockedForProfessional(
                            PROFESSIONAL_B_ID, TEST_DATE, LocalTime.of(14, 30)),
                    "14:30 NÃO deveria estar bloqueado para Profissional B"
            );
        }
    }

    // ===================================================================
    // TESTES DO AvailableTimeSlotsService - Horários disponíveis
    // ===================================================================

    @Nested
    @DisplayName("AvailableTimeSlotsService - Isolamento de horários disponíveis")
    class AvailableTimeSlotsServiceTests {

        @Mock
        private TenantWorkingHoursService workingHoursService;

        @Mock
        private BlockedTimeSlotService blockedTimeSlotService;

        @Mock
        private BlockedDayService blockedDayService;

        @Mock
        private AppointmentsRepository appointmentsRepository;

        @Mock
        private ServicesService servicesService;

        @Mock
        private TenantDateTimeService tenantDateTimeService;

        @InjectMocks
        private AvailableTimeSlotsService availableTimeSlotsService;

        @Test
        @DisplayName("Profissional B deve ter horários disponíveis mesmo com bloqueio do Profissional A")
        void professionalB_shouldHaveAvailableSlots_whenProfessionalA_hasBlock() {
            // Arrange: Horário de trabalho 09:00–13:00, intervalo 30 min
            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(13, 0), 30);

            // Profissional A tem bloqueio das 10:00 às 11:00
            BlockedTimeSlotEntity blockA = createBlock(professionalA, LocalTime.of(10, 0), LocalTime.of(11, 0));

            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);

            // Working hours para Profissional B
            when(workingHoursService.getWorkingHoursByProfessional(PROFESSIONAL_B_ID)).thenReturn(wh);

            // Profissional B NÃO tem bloqueios
            when(blockedTimeSlotService.getBlockedTimeSlotsForProfessionalAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());

            // Profissional B NÃO tem agendamentos
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());

            // Data no futuro (não está no passado)
            when(tenantDateTimeService.isDateInPast(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);
            when(tenantDateTimeService.isToday(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);

            // Act
            List<LocalTime> slotsB = availableTimeSlotsService.getAvailableTimeSlotsForProfessional(
                    PROFESSIONAL_B_ID, TEST_DATE, null);

            // Assert: Profissional B deve ter TODOS os horários disponíveis (09:00 até 12:30)
            // Grade fixa com intervalo 30: 09:00, 09:30, 10:00, 10:30, 11:00, 11:30, 12:00, 12:30
            assertTrue(slotsB.contains(LocalTime.of(10, 0)),
                    "10:00 deveria estar disponível para Profissional B");
            assertTrue(slotsB.contains(LocalTime.of(10, 30)),
                    "10:30 deveria estar disponível para Profissional B");
            assertTrue(slotsB.contains(LocalTime.of(11, 0)),
                    "11:00 deveria estar disponível para Profissional B");
            assertEquals(8, slotsB.size(),
                    "Profissional B deveria ter 8 slots disponíveis (09:00 a 12:30)");
        }

        @Test
        @DisplayName("Profissional A deve ter horários bloqueados filtrados corretamente")
        void professionalA_shouldHaveBlockedSlotsFiltered() {
            // Arrange: Horário de trabalho 09:00–13:00, intervalo 30 min
            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(13, 0), 30);

            // Profissional A tem bloqueio das 10:00 às 11:00
            BlockedTimeSlotEntity blockA = createBlock(professionalA, LocalTime.of(10, 0), LocalTime.of(11, 0));

            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);

            // Working hours para Profissional A
            when(workingHoursService.getWorkingHoursByProfessional(PROFESSIONAL_A_ID)).thenReturn(wh);

            // Profissional A TEM bloqueio
            when(blockedTimeSlotService.getBlockedTimeSlotsForProfessionalAndDate(PROFESSIONAL_A_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>(List.of(blockA)));

            // Sem agendamentos
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_A_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());

            when(tenantDateTimeService.isDateInPast(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);
            when(tenantDateTimeService.isToday(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);

            // Act
            List<LocalTime> slotsA = availableTimeSlotsService.getAvailableTimeSlotsForProfessional(
                    PROFESSIONAL_A_ID, TEST_DATE, null);

            // Assert: 10:00 e 10:30 devem estar bloqueados para Profissional A
            assertFalse(slotsA.contains(LocalTime.of(10, 0)),
                    "10:00 deveria estar bloqueado para Profissional A");
            assertFalse(slotsA.contains(LocalTime.of(10, 30)),
                    "10:30 deveria estar bloqueado para Profissional A");

            // Outros horários devem estar disponíveis
            assertTrue(slotsA.contains(LocalTime.of(9, 0)));
            assertTrue(slotsA.contains(LocalTime.of(9, 30)));
            assertTrue(slotsA.contains(LocalTime.of(11, 0)));
            assertTrue(slotsA.contains(LocalTime.of(11, 30)));
            assertTrue(slotsA.contains(LocalTime.of(12, 0)));
            assertTrue(slotsA.contains(LocalTime.of(12, 30)));

            assertEquals(6, slotsA.size(), "Profissional A deveria ter 6 slots (8 - 2 bloqueados)");
        }

        @Test
        @DisplayName("Dois profissionais podem atender no mesmo horário")
        void twoProfessionals_canBookSameTime() {
            // Arrange: Horário de trabalho 09:00–13:00, intervalo 30 min
            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(13, 0), 30);

            // Profissional A tem agendamento das 10:00 às 10:30
            AppointmentsEntity appointmentA = createAppointment(
                    professionalA, LocalTime.of(10, 0), LocalTime.of(10, 30), "Cliente A");

            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);

            // --- Profissional A ---
            when(workingHoursService.getWorkingHoursByProfessional(PROFESSIONAL_A_ID)).thenReturn(wh);
            when(blockedTimeSlotService.getBlockedTimeSlotsForProfessionalAndDate(PROFESSIONAL_A_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_A_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>(List.of(appointmentA)));

            // --- Profissional B ---
            when(workingHoursService.getWorkingHoursByProfessional(PROFESSIONAL_B_ID)).thenReturn(wh);
            when(blockedTimeSlotService.getBlockedTimeSlotsForProfessionalAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());

            when(tenantDateTimeService.isDateInPast(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);
            when(tenantDateTimeService.isToday(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);

            // Act
            List<LocalTime> slotsA = availableTimeSlotsService.getAvailableTimeSlotsForProfessional(
                    PROFESSIONAL_A_ID, TEST_DATE, null);
            List<LocalTime> slotsB = availableTimeSlotsService.getAvailableTimeSlotsForProfessional(
                    PROFESSIONAL_B_ID, TEST_DATE, null);

            // Assert: 10:00 deve estar ocupado para A mas disponível para B
            assertFalse(slotsA.contains(LocalTime.of(10, 0)),
                    "10:00 deveria estar ocupado para Profissional A");
            assertTrue(slotsB.contains(LocalTime.of(10, 0)),
                    "10:00 deveria estar disponível para Profissional B");
        }

        @Test
        @DisplayName("Último horário disponível deve ser calculado corretamente (fechamento 19:00)")
        void lastAvailableSlot_shouldBeCorrectForClosingAt19() {
            // Arrange: Horário de trabalho 09:00–19:00, intervalo 30 min
            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(19, 0), 30);

            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);
            when(workingHoursService.getWorkingHoursByProfessional(PROFESSIONAL_B_ID)).thenReturn(wh);
            when(blockedTimeSlotService.getBlockedTimeSlotsForProfessionalAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(tenantDateTimeService.isDateInPast(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);
            when(tenantDateTimeService.isToday(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);

            // Act
            List<LocalTime> slots = availableTimeSlotsService.getAvailableTimeSlotsForProfessional(
                    PROFESSIONAL_B_ID, TEST_DATE, null);

            // Assert: Com intervalo 30 min e fechamento 19:00, último slot = 18:30
            LocalTime expectedLastSlot = LocalTime.of(18, 30);
            assertTrue(slots.contains(expectedLastSlot),
                    "O último horário disponível deveria ser 18:30 (fechamento 19:00, intervalo 30 min)");

            // 18:30 deve ser o último
            assertEquals(expectedLastSlot, slots.get(slots.size() - 1),
                    "18:30 deveria ser o último horário da lista");

            // Não deve conter 19:00
            assertFalse(slots.contains(LocalTime.of(19, 0)),
                    "19:00 (horário de fechamento) NÃO deveria estar nos horários disponíveis");
        }

        @Test
        @DisplayName("Último horário com bloqueio de outro profissional NÃO deve afetar slots")
        void lastAvailableSlot_shouldNotBeAffectedByOtherProfessionalBlock() {
            // Arrange: Horário de trabalho 09:00–19:00, intervalo 30 min
            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(19, 0), 30);

            // Profissional A bloqueia 17:00–18:00
            BlockedTimeSlotEntity blockA = createBlock(professionalA, LocalTime.of(17, 0), LocalTime.of(18, 0));

            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);
            when(workingHoursService.getWorkingHoursByProfessional(PROFESSIONAL_B_ID)).thenReturn(wh);

            // Profissional B NÃO tem bloqueios
            when(blockedTimeSlotService.getBlockedTimeSlotsForProfessionalAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(tenantDateTimeService.isDateInPast(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);
            when(tenantDateTimeService.isToday(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);

            // Act
            List<LocalTime> slotsB = availableTimeSlotsService.getAvailableTimeSlotsForProfessional(
                    PROFESSIONAL_B_ID, TEST_DATE, null);

            // Assert: Profissional B deve ter 17:00 e 17:30 disponíveis (bloqueio é do A)
            assertTrue(slotsB.contains(LocalTime.of(17, 0)),
                    "17:00 deveria estar disponível para Profissional B (bloqueio é do Profissional A)");
            assertTrue(slotsB.contains(LocalTime.of(17, 30)),
                    "17:30 deveria estar disponível para Profissional B");
            assertTrue(slotsB.contains(LocalTime.of(18, 0)),
                    "18:00 deveria estar disponível para Profissional B");
            assertTrue(slotsB.contains(LocalTime.of(18, 30)),
                    "18:30 deveria ser o último horário disponível para Profissional B");

            // O último slot deve ser 18:30
            assertEquals(LocalTime.of(18, 30), slotsB.get(slotsB.size() - 1),
                    "Último slot do Profissional B deveria ser 18:30, não 17:00");
        }

        @Test
        @DisplayName("Horários disponíveis com duração de serviço - não deve ultrapassar fechamento")
        void availableSlots_withServiceDuration_shouldNotExceedClosingTime() {
            // Arrange: Horário 09:00–19:00, intervalo 30 min, serviço de 60 min
            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(19, 0), 30);

            UUID serviceId = UUID.randomUUID();
            ServicesEntity service = new ServicesEntity();
            service.setId(serviceId);
            service.setName("Corte + Barba");
            service.setDuration(60);
            service.setPrice(50.0);

            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);
            when(workingHoursService.getWorkingHoursByProfessional(PROFESSIONAL_B_ID)).thenReturn(wh);
            when(blockedTimeSlotService.getBlockedTimeSlotsForProfessionalAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());
            when(servicesService.findById(serviceId)).thenReturn(service);
            when(tenantDateTimeService.isDateInPast(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);
            when(tenantDateTimeService.isToday(TEST_DATE, TEST_TENANT_ID)).thenReturn(false);

            // Act
            List<LocalTime> slots = availableTimeSlotsService.getAvailableTimeSlotsForProfessional(
                    PROFESSIONAL_B_ID, TEST_DATE, List.of(serviceId));

            // Assert: Com serviço de 60 min e fechamento 19:00, último slot válido = 18:00
            // (18:00 + 60 min = 19:00 que não ultrapassa)
            assertTrue(slots.contains(LocalTime.of(18, 0)),
                    "18:00 deveria estar disponível (18:00 + 60min = 19:00, exatamente no fechamento)");
            assertFalse(slots.contains(LocalTime.of(18, 30)),
                    "18:30 NÃO deveria estar disponível (18:30 + 60min = 19:30, ultrapassaria o fechamento)");
        }
    }

    // ===================================================================
    // TESTES DO AppointmentsService - Validação de conflitos
    // ===================================================================

    @Nested
    @DisplayName("AppointmentsService - Validação de conflitos isolada por profissional")
    class AppointmentsServiceTests {

        @Mock
        private AppointmentsRepository appointmentsRepository;

        @Mock
        private ServicesRepository servicesRepository;

        @Mock
        private WhatsappService whatsAppService;

        @Mock
        private BlockedDayService blockedDayService;

        @Mock
        private AvailableTimeSlotsService availableTimeSlotsService;

        @Mock
        private TenantWorkingHoursService workingHoursService;

        @Mock
        private BlockedTimeSlotService blockedTimeSlotService;

        @Mock
        private ProfessionalRepository professionalRepository;

        @Mock
        private TenantRepository tenantRepository;

        @Mock
        private ProfessionalServiceService professionalServiceService;

        @Mock
        private TenantDateTimeService tenantDateTimeService;

        @InjectMocks
        private AppointmentsService appointmentsService;

        @Test
        @DisplayName("Criar agendamento para Profissional B deve funcionar mesmo com bloqueio do Profissional A")
        void createAppointment_shouldSucceed_whenBlockIsFromDifferentProfessional() {
            // Arrange
            LocalTime startTime = LocalTime.of(10, 0);
            UUID serviceId = UUID.randomUUID();

            ServicesEntity service = new ServicesEntity();
            service.setId(serviceId);
            service.setName("Corte");
            service.setDuration(30);
            service.setPrice(30.0);
            service.setTenantId(TEST_TENANT_ID);

            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(19, 0), 30);

            TenantEntity tenantEntity = new TenantEntity();
            tenantEntity.setId(UUID.randomUUID());

            when(tenantRepository.findByTenantKeyAndActiveTrue(TEST_TENANT_ID))
                    .thenReturn(Optional.of(tenantEntity));
            when(professionalRepository.findActiveByIdAndTenantId(PROFESSIONAL_B_ID, tenantEntity.getId()))
                    .thenReturn(Optional.of(professionalB));
            when(tenantDateTimeService.isInPast(any(LocalDate.class), any(LocalTime.class), eq(TEST_TENANT_ID)))
                    .thenReturn(false);
            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);
            when(servicesRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(professionalServiceService.professionalExecutesAllServices(eq(PROFESSIONAL_B_ID), anyList()))
                    .thenReturn(true);
            when(workingHoursService.getCurrentTenantWorkingHours()).thenReturn(wh);
            when(workingHoursService.getWorkingHours(TEST_TENANT_ID)).thenReturn(wh);
            when(workingHoursService.isIntervalWithinWorkingHours(
                    eq(LocalTime.of(10, 0)), eq(LocalTime.of(10, 30)), eq(TEST_TENANT_ID)))
                    .thenReturn(true);

            // Profissional B NÃO tem bloqueios
            when(blockedTimeSlotService.isIntervalBlockedForProfessional(
                    eq(PROFESSIONAL_B_ID), eq(TEST_DATE), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(false);

            // Profissional B NÃO tem agendamentos
            when(appointmentsRepository.findByProfessionalIdAndDate(PROFESSIONAL_B_ID, TEST_DATE))
                    .thenReturn(new ArrayList<>());

            when(appointmentsRepository.save(any(AppointmentsEntity.class)))
                    .thenAnswer(invocation -> {
                        AppointmentsEntity saved = invocation.getArgument(0);
                        saved.setId(UUID.randomUUID());
                        return saved;
                    });

            // Act
            AppointmentsEntity result = appointmentsService.createAppointment(
                    PROFESSIONAL_B_ID,
                    List.of(serviceId),
                    TEST_DATE,
                    startTime,
                    "Cliente Teste",
                    "11999999999",
                    TEST_TENANT_ID
            );

            // Assert
            assertNotNull(result, "Agendamento deveria ser criado com sucesso");
            assertNotNull(result.getId(), "Agendamento deveria ter um ID");
            assertEquals(PROFESSIONAL_B_ID, result.getProfessional().getId());
            assertEquals(startTime, result.getStartTime());
            verify(appointmentsRepository, times(1)).save(any(AppointmentsEntity.class));
        }

        @Test
        @DisplayName("Criar agendamento deve falhar quando o PRÓPRIO profissional tem bloqueio no horário")
        void createAppointment_shouldFail_whenProfessionalHasBlockAtSameTime() {
            // Arrange
            LocalTime startTime = LocalTime.of(10, 0);
            UUID serviceId = UUID.randomUUID();

            ServicesEntity service = new ServicesEntity();
            service.setId(serviceId);
            service.setName("Corte");
            service.setDuration(30);
            service.setPrice(30.0);

            TenantWorkingHoursEntity wh = createWorkingHours(LocalTime.of(9, 0), LocalTime.of(19, 0), 30);

            TenantEntity tenantEntity = new TenantEntity();
            tenantEntity.setId(UUID.randomUUID());

            when(tenantRepository.findByTenantKeyAndActiveTrue(TEST_TENANT_ID))
                    .thenReturn(Optional.of(tenantEntity));
            when(professionalRepository.findActiveByIdAndTenantId(PROFESSIONAL_A_ID, tenantEntity.getId()))
                    .thenReturn(Optional.of(professionalA));
            when(tenantDateTimeService.isInPast(any(LocalDate.class), any(LocalTime.class), eq(TEST_TENANT_ID)))
                    .thenReturn(false);
            when(blockedDayService.isDateBlocked(TEST_DATE)).thenReturn(false);
            when(servicesRepository.findById(serviceId)).thenReturn(Optional.of(service));
            when(professionalServiceService.professionalExecutesAllServices(eq(PROFESSIONAL_A_ID), anyList()))
                    .thenReturn(true);
            when(workingHoursService.getCurrentTenantWorkingHours()).thenReturn(wh);
            when(workingHoursService.getWorkingHours(TEST_TENANT_ID)).thenReturn(wh);
            when(workingHoursService.isIntervalWithinWorkingHours(
                    eq(LocalTime.of(10, 0)), eq(LocalTime.of(10, 30)), eq(TEST_TENANT_ID)))
                    .thenReturn(true);

            // Profissional A TEM bloqueio no horário
            when(blockedTimeSlotService.isIntervalBlockedForProfessional(
                    eq(PROFESSIONAL_A_ID), eq(TEST_DATE), eq(LocalTime.of(10, 0)), eq(LocalTime.of(10, 30))))
                    .thenReturn(true);

            // Act & Assert
            assertThrows(BusinessException.class, () ->
                    appointmentsService.createAppointment(
                            PROFESSIONAL_A_ID,
                            List.of(serviceId),
                            TEST_DATE,
                            startTime,
                            "Cliente Teste",
                            "11999999999",
                            TEST_TENANT_ID
                    ),
                    "Deveria lançar BusinessException quando o profissional tem bloqueio no horário"
            );

            verify(appointmentsRepository, never()).save(any());
        }
    }
}


