package com.capics.service;

import com.capics.dto.ProductFamilyDto;
import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import com.capics.repository.ProductFamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFamilyServiceTest {

    @Mock
    private ProductFamilyRepository repository;

    private ProductFamilyService service;

    @BeforeEach
    void setUp() {
        service = new ProductFamilyService(repository);
    }

    @Test
    void findAll_ReturnsAllFamilies() {
        ProductFamily family1 = new ProductFamily();
        family1.setFamilyCode("SC40DAGG**");
        family1.setLineCode("SMT1001N");
        family1.setCycleTime(new BigDecimal("45.0"));
        family1.setOee(new BigDecimal("85.0"));
        family1.setWorkerCount(2);

        ProductFamily family2 = new ProductFamily();
        family2.setFamilyCode("SC40DAGG**");
        family2.setLineCode("SMT1002N");
        family2.setCycleTime(new BigDecimal("50.0"));
        family2.setOee(new BigDecimal("80.0"));
        family2.setWorkerCount(3);

        when(repository.findAll()).thenReturn(Arrays.asList(family1, family2));

        List<ProductFamilyDto> result = service.findAll();

        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void findById_WithValidId_ReturnsFamily() {
        ProductFamilyId id = new ProductFamilyId("SC40DAGG**", "SMT1001N");
        ProductFamily family = new ProductFamily();
        family.setFamilyCode("SC40DAGG**");
        family.setLineCode("SMT1001N");
        family.setCycleTime(new BigDecimal("45.0"));
        family.setOee(new BigDecimal("85.0"));
        family.setWorkerCount(2);

        when(repository.findById(id)).thenReturn(Optional.of(family));

        ProductFamilyDto result = service.findById("SC40DAGG**", "SMT1001N");

        assertNotNull(result);
        assertEquals("SC40DAGG**", result.getFamilyCode());
        assertEquals("SMT1001N", result.getLineCode());
        assertEquals(new BigDecimal("45.0"), result.getCycleTime());
    }

    @Test
    void findById_WithInvalidId_ThrowsException() {
        ProductFamilyId id = new ProductFamilyId("INVALID", "INVALID");
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.findById("INVALID", "INVALID"));
    }

    @Test
    void save_WithValidDto_SavesAndReturns() {
        ProductFamilyDto dto = new ProductFamilyDto();
        dto.setFamilyCode("SC40DAGG**");
        dto.setLineCode("SMT1001N");
        dto.setCycleTime(new BigDecimal("45.0"));
        dto.setOee(new BigDecimal("85.0"));
        dto.setWorkerCount(2);

        ProductFamily savedEntity = new ProductFamily();
        savedEntity.setFamilyCode("SC40DAGG**");
        savedEntity.setLineCode("SMT1001N");
        savedEntity.setCycleTime(new BigDecimal("45.0"));
        savedEntity.setOee(new BigDecimal("85.0"));
        savedEntity.setWorkerCount(2);

        when(repository.save(any(ProductFamily.class))).thenReturn(savedEntity);

        ProductFamilyDto result = service.save(dto);

        assertNotNull(result);
        assertEquals("SC40DAGG**", result.getFamilyCode());
        verify(repository).save(any(ProductFamily.class));
    }

    @Test
    void delete_WithValidId_Deletes() {
        ProductFamilyId id = new ProductFamilyId("SC40DAGG**", "SMT1001N");
        doNothing().when(repository).deleteById(id);

        service.delete("SC40DAGG**", "SMT1001N");

        verify(repository).deleteById(id);
    }
}
