package com.capics.service;

import com.capics.dto.ProductDto;
import com.capics.entity.Product;
import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import com.capics.entity.ProductId;
import com.capics.repository.ProductFamilyRepository;
import com.capics.repository.ProductRepository;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductFamilyRepository familyRepository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, familyRepository);
    }

    @Test
    void findAll_ReturnsAllProducts() {
        Product product1 = new Product();
        product1.setItemNumber("SC40DAGG04");
        product1.setLineCode("SMT1001N");
        product1.setCycleTime(new BigDecimal("45.0"));
        product1.setOee(new BigDecimal("85.0"));
        product1.setWorkerCount(2);

        Product product2 = new Product();
        product2.setItemNumber("SC40DAGG04");
        product2.setLineCode("SMT1002N");
        product2.setCycleTime(new BigDecimal("50.0"));
        product2.setOee(new BigDecimal("80.0"));
        product2.setWorkerCount(3);

        when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));

        List<ProductDto> result = service.findAll();

        assertEquals(2, result.size());
        verify(productRepository).findAll();
    }

    @Test
    void findById_WithValidId_ReturnsProduct() {
        ProductId id = new ProductId("SC40DAGG04", "SMT1001N");
        Product product = new Product();
        product.setItemNumber("SC40DAGG04");
        product.setLineCode("SMT1001N");
        product.setFamilyCode("SC40DAGG**");
        product.setCycleTime(new BigDecimal("45.0"));
        product.setOee(new BigDecimal("85.0"));
        product.setWorkerCount(2);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ProductDto result = service.findById("SC40DAGG04", "SMT1001N");

        assertNotNull(result);
        assertEquals("SC40DAGG04", result.getItemNumber());
        assertEquals("SMT1001N", result.getLineCode());
        assertEquals(new BigDecimal("45.0"), result.getCycleTime());
    }

    @Test
    void findById_WithInvalidId_ThrowsException() {
        ProductId id = new ProductId("INVALID", "INVALID");
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.findById("INVALID", "INVALID"));
    }

    @Test
    void save_WithValidDto_SavesAndReturns() {
        ProductDto dto = new ProductDto();
        dto.setItemNumber("SC40DAGG04");
        dto.setLineCode("SMT1001N");
        dto.setFamilyCode("SC40DAGG**");
        dto.setCycleTime(new BigDecimal("45.0"));
        dto.setOee(new BigDecimal("85.0"));
        dto.setWorkerCount(2);

        Product savedEntity = new Product();
        savedEntity.setItemNumber("SC40DAGG04");
        savedEntity.setLineCode("SMT1001N");
        savedEntity.setFamilyCode("SC40DAGG**");
        savedEntity.setCycleTime(new BigDecimal("45.0"));
        savedEntity.setOee(new BigDecimal("85.0"));
        savedEntity.setWorkerCount(2);

        when(productRepository.save(any(Product.class))).thenReturn(savedEntity);

        ProductDto result = service.save(dto);

        assertNotNull(result);
        assertEquals("SC40DAGG04", result.getItemNumber());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void importFromExcel_WithMatchingFamily_CreatesProductsForEachLine() {
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

        when(familyRepository.findByFamilyCode("SC40DAGG**"))
                .thenReturn(Arrays.asList(family1, family2));

        var families = familyRepository.findByFamilyCode("SC40DAGG**");
        assertEquals(2, families.size());
        verifyNoInteractions(productRepository);
    }
}
