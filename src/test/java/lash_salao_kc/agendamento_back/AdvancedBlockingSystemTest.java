package lash_salao_kc.agendamento_back;

import lash_salao_kc.agendamento_back.config.TenantContext;
import lash_salao_kc.agendamento_back.domain.entity.BlockedTimeSlotEntity;
import lash_salao_kc.agendamento_back.domain.entity.ProfessionalEntity;
import lash_salao_kc.agendamento_back.domain.entity.TenantWorkingHoursEntity;
import lash_salao_kc.agendamento_back.exception.BusinessException;
import lash_salao_kc.agendamento_back.repository.AppointmentsRepository;
import lash_salao_kc.agendamento_back.repository.BlockedTimeSlotRepository;
import lash_salao_kc.agendamento_back.repository.TenantWorkingHoursRepository;
import lash_salao_kc.agendamento_back.service.AvailableTimeSlotsService;
import lash_salao_kc.agendamento_back.service.BlockedDayService;
import lash_salao_kc.agendamento_back.service.BlockedTimeSlotService;
import lash_salao_kc.agendamento_back.service.ProfessionalService;
import lash_salao_kc.agendamento_back.service.TenantWorkingHoursService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o sistema avançado de bloqueio de horários.
 */
@ExtendWith(MockitoExtension.class)
class AdvancedBlockingSystemTest {

    @Mock
    private TenantWorkingHoursRepository workingHoursRepository;

    @Mock
    private BlockedTimeSlotRepository blockedTimeSlotRepository;

    @Mock
    private AppointmentsRepository appointmentsRepository;

    @Mock
    private BlockedDayService blockedDayService;

    @Mock
    private ProfessionalService professionalService;

    @InjectMocks
    private TenantWorkingHoursService workingHoursService;

    @InjectMocks
    private BlockedTimeSlotService blockedTimeSlotService;

    @InjectMocks
    private AvailableTimeSlotsService availableTimeSlotsService;

    private static final String TEST_TENANT_ID = "kc";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TEST_TENANT_ID);
    }

    @Test
    void testConfigureWorkingHours_Success() {
        // Arrange
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        Integer slotInterval = 30;

        when(workingHoursRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(Optional.empty());
        when(workingHoursRepository.save(any(TenantWorkingHoursEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TenantWorkingHoursEntity result = workingHoursService.configureWorkingHours(
                startTime, endTime, slotInterval
        );

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(slotInterval, result.getSlotIntervalMinutes());
        verify(workingHoursRepository, times(1)).save(any(TenantWorkingHoursEntity.class));
    }

    @Test
    void testConfigureWorkingHours_InvalidHours() {
        // Arrange
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(9, 0); // Fim antes do início - inválido

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            workingHoursService.configureWorkingHours(startTime, endTime, 30);
        });

        verify(workingHoursRepository, never()).save(any());
    }

    @Test
    void testBlockSpecificTimeSlot_Success() {
        // Arrange
        UUID professionalId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 25);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);
        String reason = "Reunião";

        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setId(professionalId);
        professional.setProfessionalName("Test Professional");

        TenantWorkingHoursEntity workingHours = new TenantWorkingHoursEntity();
        workingHours.setTenantId(TEST_TENANT_ID);
        workingHours.setStartTime(LocalTime.of(9, 0));
        workingHours.setEndTime(LocalTime.of(18, 0));
        workingHours.setSlotIntervalMinutes(30);

        when(professionalService.getProfessionalById(professionalId))
                .thenReturn(professional);
        when(workingHoursRepository.findByTenantId(TEST_TENANT_ID))
                .thenReturn(Optional.of(workingHours));
        when(blockedTimeSlotRepository.findConflictingBlocksOnSpecificDate(
                eq(TEST_TENANT_ID), eq(date), eq(startTime), eq(endTime)))
                .thenReturn(new ArrayList<>());
        when(blockedTimeSlotRepository.save(any(BlockedTimeSlotEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BlockedTimeSlotEntity result = blockedTimeSlotService.blockSpecificTimeSlot(
                professionalId, date, startTime, endTime, reason
        );

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(professional, result.getProfessional());
        assertEquals(date, result.getSpecificDate());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(reason, result.getReason());
        assertFalse(result.isRecurring());
        verify(blockedTimeSlotRepository, times(1)).save(any(BlockedTimeSlotEntity.class));
    }

    @Test
    void testBlockRecurringTimeSlot_Success() {
        // Arrange
        UUID professionalId = UUID.randomUUID();
        DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(13, 0);
        String reason = "Almoço recorrente";

        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setId(professionalId);
        professional.setProfessionalName("Test Professional");

        TenantWorkingHoursEntity workingHours = new TenantWorkingHoursEntity();
        workingHours.setTenantId(TEST_TENANT_ID);
        workingHours.setStartTime(LocalTime.of(9, 0));
        workingHours.setEndTime(LocalTime.of(18, 0));
        workingHours.setSlotIntervalMinutes(30);

        when(professionalService.getProfessionalById(professionalId))
                .thenReturn(professional);
        when(workingHoursRepository.findByTenantId(TEST_TENANT_ID))
                .thenReturn(Optional.of(workingHours));
        when(blockedTimeSlotRepository.findConflictingRecurringBlocks(
                eq(TEST_TENANT_ID), eq(dayOfWeek), eq(startTime), eq(endTime)))
                .thenReturn(new ArrayList<>());
        when(blockedTimeSlotRepository.save(any(BlockedTimeSlotEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BlockedTimeSlotEntity result = blockedTimeSlotService.blockRecurringTimeSlot(
                professionalId, dayOfWeek, startTime, endTime, reason
        );

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(professional, result.getProfessional());
        assertEquals(dayOfWeek, result.getDayOfWeek());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(reason, result.getReason());
        assertTrue(result.isRecurring());
        assertNull(result.getSpecificDate());
        verify(blockedTimeSlotRepository, times(1)).save(any(BlockedTimeSlotEntity.class));
    }

    @Test
    void testGetAvailableTimeSlots_WithBlockedSlots() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 1, 20);

        TenantWorkingHoursEntity workingHours = new TenantWorkingHoursEntity();
        workingHours.setTenantId(TEST_TENANT_ID);
        workingHours.setStartTime(LocalTime.of(9, 0));
        workingHours.setEndTime(LocalTime.of(12, 0)); // Período curto para teste
        workingHours.setSlotIntervalMinutes(30);

        BlockedTimeSlotEntity blockedSlot = new BlockedTimeSlotEntity();
        blockedSlot.setTenantId(TEST_TENANT_ID);
        blockedSlot.setSpecificDate(date);
        blockedSlot.setStartTime(LocalTime.of(10, 0));
        blockedSlot.setEndTime(LocalTime.of(11, 0));
        blockedSlot.setRecurring(false);

        when(blockedDayService.isDateBlocked(date)).thenReturn(false);
        when(workingHoursRepository.findByTenantId(TEST_TENANT_ID))
                .thenReturn(Optional.of(workingHours));
        when(blockedTimeSlotRepository.findByTenantIdAndSpecificDate(TEST_TENANT_ID, date))
                .thenReturn(List.of(blockedSlot));
        when(blockedTimeSlotRepository.findByTenantIdAndDayOfWeekAndRecurring(
                eq(TEST_TENANT_ID), any(DayOfWeek.class), eq(true)))
                .thenReturn(new ArrayList<>());
        when(appointmentsRepository.findByTenantIdAndDate(TEST_TENANT_ID, date))
                .thenReturn(new ArrayList<>());

        // Act
        List<LocalTime> availableSlots = availableTimeSlotsService.getAvailableTimeSlots(date, TEST_TENANT_ID);

        // Assert
        assertNotNull(availableSlots);
        assertTrue(availableSlots.contains(LocalTime.of(9, 0)));
        assertTrue(availableSlots.contains(LocalTime.of(9, 30)));
        assertFalse(availableSlots.contains(LocalTime.of(10, 0))); // Bloqueado
        assertFalse(availableSlots.contains(LocalTime.of(10, 30))); // Bloqueado
        assertTrue(availableSlots.contains(LocalTime.of(11, 0)));
        assertTrue(availableSlots.contains(LocalTime.of(11, 30)));
    }

    @Test
    void testGetAvailableTimeSlots_DayFullyBlocked() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 1, 25);

        when(blockedDayService.isDateBlocked(date)).thenReturn(true);

        // Act
        List<LocalTime> availableSlots = availableTimeSlotsService.getAvailableTimeSlots(date, TEST_TENANT_ID);

        // Assert
        assertNotNull(availableSlots);
        assertTrue(availableSlots.isEmpty());
    }

    @Test
    void testIsIntervalWithinWorkingHours() {
        // Arrange
        TenantWorkingHoursEntity workingHours = new TenantWorkingHoursEntity();
        workingHours.setTenantId(TEST_TENANT_ID);
        workingHours.setStartTime(LocalTime.of(9, 0));
        workingHours.setEndTime(LocalTime.of(18, 0));

        when(workingHoursRepository.findByTenantId(TEST_TENANT_ID))
                .thenReturn(Optional.of(workingHours));

        // Act & Assert
        assertTrue(workingHoursService.isIntervalWithinWorkingHours(
                LocalTime.of(9, 0), LocalTime.of(10, 0), TEST_TENANT_ID));
        assertTrue(workingHoursService.isIntervalWithinWorkingHours(
                LocalTime.of(17, 0), LocalTime.of(18, 0), TEST_TENANT_ID));
        assertFalse(workingHoursService.isIntervalWithinWorkingHours(
                LocalTime.of(8, 0), LocalTime.of(9, 30), TEST_TENANT_ID)); // Início antes
        assertFalse(workingHoursService.isIntervalWithinWorkingHours(
                LocalTime.of(17, 0), LocalTime.of(19, 0), TEST_TENANT_ID)); // Fim depois
    }
}

