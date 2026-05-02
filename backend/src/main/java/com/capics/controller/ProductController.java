package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.FamilyLineDto;
import com.capics.dto.ProductDto;
import com.capics.dto.ProductFamilyDto;
import com.capics.service.FamilyLineService;
import com.capics.service.ProductFamilyService;
import com.capics.service.ProductService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductFamilyService familyService;
    private final ProductService productService;
    private final FamilyLineService familyLineService;

    public ProductController(ProductFamilyService familyService,
                             ProductService productService,
                             FamilyLineService familyLineService) {
        this.familyService = familyService;
        this.productService = productService;
        this.familyLineService = familyLineService;
    }

    @GetMapping("/families")
    public ResponseEntity<ApiResponse> getAllFamilies() {
        List<ProductFamilyDto> families = familyService.findAll();
        return ResponseEntity.ok(ApiResponse.success(families));
    }

    @GetMapping("/families/search")
    public ResponseEntity<ApiResponse> searchFamilies(@RequestParam(required = false) String keyword) {
        List<ProductFamilyDto> families = familyService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(families));
    }

    @GetMapping("/families/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> getFamilyById(@PathVariable String familyCode,
                                                      @PathVariable String lineCode) {
        ProductFamilyDto family = familyService.findById(familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success(family));
    }

    @PostMapping("/families")
    public ResponseEntity<ApiResponse> createFamily(@RequestBody ProductFamilyDto dto) {
        ProductFamilyDto saved = familyService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/families/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> updateFamily(@PathVariable String familyCode,
                                                     @PathVariable String lineCode,
                                                     @RequestBody ProductFamilyDto dto) {
        ProductFamilyDto updated = familyService.save(dto, familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/families/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> deleteFamily(@PathVariable String familyCode,
                                                     @PathVariable String lineCode) {
        familyService.delete(familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PostMapping("/families/import/check")
    public ResponseEntity<ApiResponse> checkFamilyImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = familyService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        }
        return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
    }

    @PostMapping("/families/import")
    public ResponseEntity<ApiResponse> importFamilies(@RequestParam("file") MultipartFile file,
                                                       @RequestParam("createdBy") String createdBy) {
        try {
            int count = familyService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("导入编码族失败: " + e.getMessage()));
        }
    }

    @GetMapping("/families/template")
    public ResponseEntity<Resource> downloadFamilyTemplate() throws IOException {
        return downloadTemplateFile("编码族导入模板.xlsx");
    }

    @GetMapping("/family-lines/template")
    public ResponseEntity<Resource> downloadFamilyLineTemplate() throws IOException {
        return downloadTemplateFile("编码族定线导入模板.xlsx");
    }

    @GetMapping("/family-lines")
    public ResponseEntity<ApiResponse> getAllFamilyLines() {
        List<FamilyLineDto> familyLines = familyLineService.findAll();
        return ResponseEntity.ok(ApiResponse.success(familyLines));
    }

    @GetMapping("/family-lines/search")
    public ResponseEntity<ApiResponse> searchFamilyLines(@RequestParam(required = false) String keyword) {
        List<FamilyLineDto> familyLines = familyLineService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(familyLines));
    }

    @GetMapping("/family-lines/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> getFamilyLineById(@PathVariable String familyCode,
                                                          @PathVariable String lineCode) {
        FamilyLineDto familyLine = familyLineService.findById(familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success(familyLine));
    }

    @PostMapping("/family-lines")
    public ResponseEntity<ApiResponse> createFamilyLine(@RequestBody FamilyLineDto dto) {
        String operator = resolveOperator(dto.getUpdatedBy(), dto.getCreatedBy());
        FamilyLineDto saved = familyLineService.save(dto, operator, dto.getFamilyCode(), dto.getLineCode());
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/family-lines/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> updateFamilyLine(@PathVariable String familyCode,
                                                         @PathVariable String lineCode,
                                                         @RequestBody FamilyLineDto dto) {
        String operator = resolveOperator(dto.getUpdatedBy(), dto.getCreatedBy());
        FamilyLineDto updated = familyLineService.save(dto, operator, familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @PostMapping("/family-lines/import/check")
    public ResponseEntity<ApiResponse> checkFamilyLineImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = familyLineService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        }
        return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
    }

    @PostMapping("/family-lines/import")
    public ResponseEntity<ApiResponse> importFamilyLines(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("createdBy") String createdBy) {
        try {
            int count = familyLineService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("导入编码族定线失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllProducts() {
        List<ProductDto> products = productService.findAll();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchProducts(@RequestParam(required = false) String keyword) {
        List<ProductDto> products = productService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{itemNumber}/{lineCode}")
    public ResponseEntity<ApiResponse> getProductById(@PathVariable String itemNumber,
                                                       @PathVariable String lineCode) {
        ProductDto product = productService.findById(itemNumber, lineCode);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createProduct(@RequestBody ProductDto dto) {
        ProductDto saved = productService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/{itemNumber}/{lineCode}")
    public ResponseEntity<ApiResponse> updateProduct(@PathVariable String itemNumber,
                                                      @PathVariable String lineCode,
                                                      @RequestBody ProductDto dto) {
        dto.setItemNumber(itemNumber);
        dto.setLineCode(lineCode);
        ProductDto updated = productService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/{itemNumber}/{lineCode}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable String itemNumber,
                                                      @PathVariable String lineCode) {
        productService.delete(itemNumber, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PostMapping("/import/check")
    public ResponseEntity<ApiResponse> checkProductImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = productService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        }
        return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importProducts(@RequestParam("file") MultipartFile file,
                                                       @RequestParam("createdBy") String createdBy) {
        try {
            int count = productService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("导入产品主数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadProductTemplate() throws IOException {
        ByteArrayResource resource = new ByteArrayResource(productService.buildTemplate());
        String encoded = URLEncoder.encode("产品主数据导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    private ResponseEntity<Resource> downloadTemplateFile(String fileName) throws IOException {
        File localFile = new File("import_templates", fileName);
        Resource resource = localFile.exists()
                ? new org.springframework.core.io.FileSystemResource(localFile)
                : new ClassPathResource("import_templates/" + fileName);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private String resolveOperator(String updatedBy, String createdBy) {
        if (updatedBy != null && !updatedBy.trim().isEmpty()) {
            return updatedBy;
        }
        if (createdBy != null && !createdBy.trim().isEmpty()) {
            return createdBy;
        }
        return "system";
    }
}
