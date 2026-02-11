package lash_salao_kc.agendamento_back.service;

import lash_salao_kc.agendamento_back.domain.entity.TenantEntity;
import lash_salao_kc.agendamento_back.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Testes para TenantDateTimeService - Validação de horários no passado
 */
@ExtendWith(MockitoExtension.class)
class TenantDateTimeServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantDateTimeService tenantDateTimeService;

    private TenantEntity mockTenant;

    @BeforeEach
    void setUp() {
        mockTenant = new TenantEntity();
        mockTenant.setTenantKey("test-tenant");
        mockTenant.setBusinessName("Test Salon");
        mockTenant.setActive(true);
        mockTenant.setTimezone("America/Sao_Paulo");

        when(tenantRepository.findByTenantKeyAndActiveTrue(anyString()))
                .thenReturn(Optional.of(mockTenant));
    }

    @Test
    void testIsInPast_DateInPast_ReturnsTrue() {
        // Data no passado
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalTime anyTime = LocalTime.of(10, 0);

        assertTrue(tenantDateTimeService.isInPast(pastDate, anyTime, "test-tenant"));
    }

    @Test
    void testIsInPast_TodayButTimePassed_ReturnsTrue() {
        // Hoje, mas horário que já passou
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        LocalTime pastTime = LocalTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(1);

        assertTrue(tenantDateTimeService.isInPast(today, pastTime, "test-tenant"));
    }

    @Test
    void testIsInPast_FutureDate_ReturnsFalse() {
        // Data futura
        LocalDate futureDate = LocalDate.now().plusDays(1);
        LocalTime anyTime = LocalTime.of(10, 0);

        assertFalse(tenantDateTimeService.isInPast(futureDate, anyTime, "test-tenant"));
    }

    @Test
    void testIsInPast_TodayFutureTime_ReturnsFalse() {
        // Hoje, horário futuro
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        LocalTime futureTime = LocalTime.now(ZoneId.of("America/Sao_Paulo")).plusHours(2);

        assertFalse(tenantDateTimeService.isInPast(today, futureTime, "test-tenant"));
    }

    @Test
    void testIsDateInPast_PastDate_ReturnsTrue() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        assertTrue(tenantDateTimeService.isDateInPast(pastDate, "test-tenant"));
    }

    @Test
    void testIsDateInPast_Today_ReturnsFalse() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        assertFalse(tenantDateTimeService.isDateInPast(today, "test-tenant"));
    }

    @Test
    void testIsDateInPast_FutureDate_ReturnsFalse() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        assertFalse(tenantDateTimeService.isDateInPast(futureDate, "test-tenant"));
    }

    @Test
    void testIsToday_TodayDate_ReturnsTrue() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        assertTrue(tenantDateTimeService.isToday(today, "test-tenant"));
    }

    @Test
    void testIsToday_PastDate_ReturnsFalse() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertFalse(tenantDateTimeService.isToday(yesterday, "test-tenant"));
    }

    @Test
    void testIsToday_FutureDate_ReturnsFalse() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertFalse(tenantDateTimeService.isToday(tomorrow, "test-tenant"));
    }

    @Test
    void testGetTenantZoneId_ValidTimezone_ReturnsCorrectZone() {
        ZoneId zoneId = tenantDateTimeService.getTenantZoneId("test-tenant");
        assertEquals(ZoneId.of("America/Sao_Paulo"), zoneId);
    }

    @Test
    void testGetTenantZoneId_InvalidTimezone_ReturnsFallback() {
        mockTenant.setTimezone("Invalid/Timezone");
        ZoneId zoneId = tenantDateTimeService.getTenantZoneId("test-tenant");
        assertEquals(ZoneId.of("America/Sao_Paulo"), zoneId); // Fallback
    }

    @Test
    void testToZonedDateTime_ConvertsCorrectly() {
        LocalDate date = LocalDate.of(2026, 2, 15);
        LocalTime time = LocalTime.of(14, 30);

        ZonedDateTime zdt = tenantDateTimeService.toZonedDateTime(date, time, "test-tenant");

        assertEquals(date, zdt.toLocalDate());
        assertEquals(time, zdt.toLocalTime());
        assertEquals(ZoneId.of("America/Sao_Paulo"), zdt.getZone());
    }
}

