package com.capics.service;

import com.capics.dto.MrpPlanDto;
import com.capics.entity.MrpPlan;
import com.capics.repository.MrpPlanRepository;
import com.capics.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MrpPlanServiceTest {

    @Mock
    private MrpPlanRepository repository;

    @Mock
    private ProductRepository productRepository;

    private MrpPlanService service;

    @BeforeEach
    void setUp() {
        service = new MrpPlanService(repository, productRepository);
        lenient().when(productRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    void findAll_ReturnsAllPlans() {
        MrpPlan plan1 = new MrpPlan();
        plan1.setId(1L);
        plan1.setItemNumber("AI09AABB01L");
        plan1.setVersion("0303");
        plan1.setQuantityScheduled(new BigDecimal("100"));

        MrpPlan plan2 = new MrpPlan();
        plan2.setId(2L);
        plan2.setItemNumber("AI09AABD01L");
        plan2.setVersion("0303");
        plan2.setQuantityScheduled(new BigDecimal("200"));

        when(repository.findAll()).thenReturn(Arrays.asList(plan1, plan2));

        List<MrpPlanDto> result = service.findAll();

        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void findByVersion_ReturnsFilteredPlans() {
        MrpPlan plan1 = new MrpPlan();
        plan1.setId(1L);
        plan1.setItemNumber("AI09AABB01L");
        plan1.setVersion("0303");

        when(repository.findByVersion("0303")).thenReturn(Arrays.asList(plan1));

        List<MrpPlanDto> result = service.findByVersion("0303");

        assertEquals(1, result.size());
        assertEquals("0303", result.get(0).getVersion());
    }

    @Test
    void findById_WithValidId_ReturnsPlan() {
        MrpPlan plan = new MrpPlan();
        plan.setId(1L);
        plan.setItemNumber("AI09AABB01L");
        plan.setVersion("0303");
        plan.setQuantityScheduled(new BigDecimal("100"));

        when(repository.findById(1L)).thenReturn(Optional.of(plan));

        MrpPlanDto result = service.findById(1L);

        assertNotNull(result);
        assertEquals("AI09AABB01L", result.getItemNumber());
        assertEquals(new BigDecimal("100"), result.getQuantityScheduled());
    }

    @Test
    void findById_WithInvalidId_ThrowsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.findById(999L));
    }

    @Test
    void save_WithValidDto_SavesAndReturns() {
        MrpPlanDto dto = new MrpPlanDto();
        dto.setItemNumber("AI09AABB01L");
        dto.setVersion("0303");
        dto.setQuantityScheduled(new BigDecimal("100"));
        dto.setSite("SH");
        dto.setReleaseDate(LocalDate.now());

        MrpPlan savedEntity = new MrpPlan();
        savedEntity.setId(1L);
        savedEntity.setItemNumber("AI09AABB01L");
        savedEntity.setVersion("0303");
        savedEntity.setQuantityScheduled(new BigDecimal("100"));

        when(repository.save(any(MrpPlan.class))).thenReturn(savedEntity);

        MrpPlanDto result = service.save(dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(repository).save(any(MrpPlan.class));
    }

    @Test
    void delete_WithValidId_Deletes() {
        doNothing().when(repository).deleteById(1L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void getAllVersions_ReturnsDistinctVersions() {
        when(repository.findAllVersions()).thenReturn(Arrays.asList("0303", "0310", "0317"));

        List<String> versions = service.getAllVersions();

        assertEquals(3, versions.size());
        assertTrue(versions.contains("0303"));
        assertTrue(versions.contains("0310"));
        assertTrue(versions.contains("0317"));
    }

    @Test
    void getWeeklyReport_AggregatesByWeek() {
        MrpPlan plan1 = new MrpPlan();
        plan1.setId(1L);
        plan1.setItemNumber("AI09AABB01L");
        plan1.setReleaseDate(LocalDate.of(2026, 3, 16));
        plan1.setQuantityScheduled(new BigDecimal("100"));

        MrpPlan plan2 = new MrpPlan();
        plan2.setId(2L);
        plan2.setItemNumber("AI09AABB01L");
        plan2.setReleaseDate(LocalDate.of(2026, 3, 16));
        plan2.setQuantityScheduled(new BigDecimal("50"));

        when(repository.findByVersionOrderByItemNumberAndReleaseDate("0303"))
                .thenReturn(Arrays.asList(plan1, plan2));

        var result = service.getWeeklyReport("0303");

        assertFalse(result.isEmpty());
    }
}
