package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ProductBatchCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductBatchUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBatchResponse;
import com.petbuddy.petbuddystore.dto.response.ProductImportResponse;
import com.petbuddy.petbuddystore.mapper.ProductBatchMapper;
import com.petbuddy.petbuddystore.model.Category;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.ProductBatch;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.service.CategoryService;
import com.petbuddy.petbuddystore.service.ProductBatchService;
import com.petbuddy.petbuddystore.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductBatchServiceImpl implements ProductBatchService {

    static final int MAX_BATCH_CREATE_LIMIT = 10;

    ProductBatchRepository productBatchRepository;
    ProductService productService;
    ProductBatchMapper productBatchMapper;
    CategoryService categoryService;

    @Override
    @Transactional
    public List<ProductBatchResponse> createBatches(UUID productId, List<ProductBatchCreationRequest> requests) {
        validateBatchCreateRequests(requests);

        Product product = productService.getActiveProductEntityById(productId);

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new AppException(ErrorCode.PRODUCT_DELETED);
        }

        List<ProductBatch> batches = new ArrayList<>();

        long currentBatchCount = productBatchRepository.countByProduct_ProductId(productId);

        for (int i = 0; i < requests.size(); i++) {
            ProductBatchCreationRequest request = requests.get(i);

            ProductBatch batch = productBatchMapper.toProductBatch(request);
            batch.setProduct(product);
            batch.setStatus(ProductStatus.ACTIVE);
            batch.setDeletedAt(null);
            batch.setBatchCode(generateBatchCode(product, currentBatchCount + i + 1));
            batches.add(batch);
        }
        List<ProductBatch> savedBatches = productBatchRepository.saveAllAndFlush(batches);
        return savedBatches.stream().map(this::buildBatchResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductBatchResponse> getBatchesByProduct(UUID productId, String keyword, ProductStatus status, String sortBy, Pageable pageable) {
        Product product = productService.getProductEntityById(productId);
        Pageable sortedPageable = buildPageable(pageable, sortBy);

        Specification<ProductBatch> spec = buildBatchSpec(product.getProductId(), keyword, status);

        return productBatchRepository.findAll(spec, sortedPageable).map(this::buildBatchResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductBatchResponse getBatchDetail(UUID batchId) {
        return buildBatchResponse(getBatchEntityById(batchId));
    }

    @Override
    @Transactional
    public ProductBatchResponse updateBatch(UUID batchId, ProductBatchUpdateRequest request) {
        ProductBatch batch = getBatchEntityById(batchId);
        if (request.getStockQuantity() != null) {
            if (request.getStockQuantity() < 0) {throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);}
            batch.setStockQuantity(request.getStockQuantity());
        }
        if (request.getExpiryDate() != null) {batch.setExpiryDate(request.getExpiryDate());}
        if (request.getStatus() != null) {updateStatus(batch, request.getStatus());}
        return buildBatchResponse(productBatchRepository.save(batch));
    }

    @Override
    @Transactional
    public void deleteDeletedBatchesOlderThan90Days() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        List<ProductBatch> oldDeletedBatches = productBatchRepository.findByStatusAndDeletedAtBefore(ProductStatus.DELETED, threshold);
        productBatchRepository.deleteAll(oldDeletedBatches);
    }

    private void validateBatchCreateRequests(List<ProductBatchCreationRequest> requests) {
        if (requests == null || requests.isEmpty()) {throw new AppException(ErrorCode.BATCH_REQUIRED);}
        if (requests.size() > MAX_BATCH_CREATE_LIMIT) {throw new AppException(ErrorCode.BATCH_LIMIT_EXCEEDED);}
    }

    private void updateStatus(ProductBatch batch, ProductStatus status) {
        batch.setStatus(status);
        if (status == ProductStatus.DELETED) {batch.setDeletedAt(LocalDateTime.now());
        } else {batch.setDeletedAt(null);}
    }

    private String generateBatchCode(Product product, long nextNumber) {
        String code;
        do {
            long letterPart = (nextNumber - 1) / 999;
            long numberPart = (nextNumber - 1) % 999 + 1;
            char c1 = (char) ('A' + (letterPart / 26) % 26);
            char c2 = (char) ('A' + letterPart % 26);
            code = product.getProductCode() + "-" + c1 + c2 + String.format("%03d", numberPart);
            nextNumber++;
        } while (productBatchRepository.existsByBatchCode(code));return code;
    }

    private ProductBatch getBatchEntityById(UUID batchId) {
        return productBatchRepository.findById(batchId).orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_FOUND));
    }

    private Pageable buildPageable(Pageable pageable, String sortBy) {
        Sort sort = switch (sortBy == null ? "date_desc" : sortBy) {
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "date_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "stock_asc" -> Sort.by(Sort.Direction.ASC, "stockQuantity");
            case "stock_desc" -> Sort.by(Sort.Direction.DESC, "stockQuantity");
            case "expiry_asc" -> Sort.by(Sort.Direction.ASC, "expiryDate");
            case "expiry_desc" -> Sort.by(Sort.Direction.DESC, "expiryDate");
            default -> throw new AppException(ErrorCode.INVALID_SORT_OPTION);
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Specification<ProductBatch> buildBatchSpec(UUID productId, String keyword, ProductStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("product").get("productId"), productId));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("batchCode")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (status != null) {predicates.add(cb.equal(root.get("status"), status));}
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ProductBatchResponse buildBatchResponse(ProductBatch batch) {
        ProductBatchResponse response = productBatchMapper.toProductBatchResponse(batch);
        if (batch.getProduct() != null) {
            response.setProductId(batch.getProduct().getProductId());
            response.setProductCode(batch.getProduct().getProductCode());
            response.setProductName(batch.getProduct().getName());
        }
        return response;
    }
    @Override
    @Transactional
    public ProductImportResponse importProductsAndBatches(MultipartFile file, boolean confirm) {
        validateExcelFile(file);
        log.info("Import file size: {}", file.getSize());
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<PendingImportRow> validRows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet.getRow(0), formatter);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmpty(row, formatter)) {continue;}
                int rowNumber = i + 1;
                String name = cell(row, 0, formatter);
                String description = cell(row, 1, formatter);
                BigDecimal price = parseDecimal(cell(row, 2, formatter));
                String brandName = cell(row, 3, formatter);
                String categoryName = cell(row, 4, formatter);
                BigDecimal stockDecimal = parseDecimal(cell(row, 5, formatter));
                Integer stockQuantity = stockDecimal == null ? null : stockDecimal.intValue();
                LocalDate expiryDate = parseDate(row.getCell(6), cell(row, 6, formatter));

                if (name == null || name.isBlank()) {errors.add("Row " + rowNumber + ": Product name is required.");continue;}
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {errors.add("Row " + rowNumber + ": Product price is invalid.");continue;}
                if (categoryName == null || categoryName.isBlank()) {errors.add("Row " + rowNumber + ": Category name is required.");continue;}
                if (stockQuantity == null || stockQuantity < 0) {errors.add("Row " + rowNumber + ": Stock quantity is invalid.");continue;}
                if (expiryDate != null && !expiryDate.isAfter(LocalDate.now())) {errors.add("Row " + rowNumber + ": Expiry date is invalid.");continue;}

                Category category;
                try {category = categoryService.getActiveCategoryEntityByName(categoryName);
                } catch (AppException e) {
                    errors.add("Row " + rowNumber + ": Category '" + categoryName + "' does not exist or is inactive.");continue;
                }

                Product product = productService.getProductEntityByName(name);

                if (product != null) {
                    if (product.getStatus() == ProductStatus.DELETED) {
                        errors.add("Row " + rowNumber + ": Product '" + name + "' is deleted.");continue;
                    }

                    if (hasDifferentProductInfo(product, description, price, brandName, category)) {
                        warnings.add("Row " + rowNumber + ": Product '" + name + "' already exists. Old product information will be kept, only new batch will be created.");
                    }
                }
                validRows.add(new PendingImportRow(rowNumber, name, description, price, brandName, category, stockQuantity, expiryDate));
            }

            if (!errors.isEmpty() || !confirm) {
                return ProductImportResponse.builder().canImport(errors.isEmpty()).createdProducts(0).createdBatches(0).warnings(warnings).errors(errors).build();
            }
            int createdProducts = 0;
            int createdBatches = 0;
            Map<UUID, Long> batchCounter = new HashMap<>();
            for (PendingImportRow row : validRows) {
                Product product = productService.getProductEntityByName(row.name());

                if (product == null) {
                    product = productService.createProductFromImport(row.name(),row.description(),row.price(),row.brandName(),row.category());
                    createdProducts++;
                }
                long nextNumber = batchCounter.computeIfAbsent(product.getProductId(), productId -> productBatchRepository.countByProduct_ProductId(productId) + 1);
                ProductBatch batch = ProductBatch.builder()
                        .batchCode(generateBatchCode(product, nextNumber))
                        .product(product)
                        .stockQuantity(row.stockQuantity())
                        .expiryDate(row.expiryDate())
                        .status(ProductStatus.ACTIVE)
                        .deletedAt(null).build();

                productBatchRepository.save(batch);
                batchCounter.put(product.getProductId(), nextNumber + 1);
                createdBatches++;
            }
            return ProductImportResponse.builder()
                    .canImport(true)
                    .createdProducts(createdProducts)
                    .createdBatches(createdBatches)
                    .warnings(warnings)
                    .errors(errors).build();

        } catch (AppException e) {throw e;
        } catch (Exception e) {log.error("Import products and batches failed", e);
            throw new AppException(ErrorCode.IMPORT_FAILED);
        }
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) {return null;}
        String value = formatter.formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }

    private BigDecimal parseDecimal(String value) {
        try {if (value == null) {return null;}
            String normalizedValue = value.replace(",", "").replace(" ", "").trim();
            return new BigDecimal(normalizedValue);
        } catch (Exception e) {return null;}
    }

    private LocalDate parseDate(Cell cell, String value) {
        try {
            if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (value == null) {return null;}
            List<DateTimeFormatter> formats = List.of(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
            );
            for (DateTimeFormatter formatter : formats) {
                try {return LocalDate.parse(value, formatter);
                } catch (Exception ignored) {// Try next date format
                }
            }
            return null;
        } catch (Exception e) {return null;}
    }

    private boolean isEmpty(Row row, DataFormatter formatter) {
        for (int i = 0; i <= 6; i++) {
            if (cell(row, i, formatter) != null) {return false;}
        }
        return true;
    }

    private void validateHeader(Row header, DataFormatter formatter) {if (header == null) {throw new AppException(ErrorCode.INVALID_EXCEL_TEMPLATE);}

        String[] expectedHeaders = {"Name", "Description", "Price", "BrandName", "CategoryName", "StockQuantity", "ExpiryDate"};
        for (int i = 0; i < expectedHeaders.length; i++) {
            String actual = cell(header, i, formatter);
            if (actual == null || !expectedHeaders[i].equalsIgnoreCase(actual)) {
                throw new AppException(ErrorCode.INVALID_EXCEL_TEMPLATE);
            }
        }
    }

    private boolean hasDifferentProductInfo(Product product, String description, BigDecimal price, String brandName, Category category) {
        return !Objects.equals(product.getDescription(), description)
                || product.getPrice().compareTo(price) != 0
                || !Objects.equals(product.getBrandName(), brandName)
                || !Objects.equals(product.getCategory().getCategoryId(), category.getCategoryId());
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {throw new AppException(ErrorCode.FILE_REQUIRED);}
        if (file.getSize() > 10 * 1024 * 1024) {throw new AppException(ErrorCode.FILE_TOO_LARGE);}
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {throw new AppException(ErrorCode.INVALID_FILE_TYPE);}
    }
    private record PendingImportRow(int rowNumber, String name, String description, BigDecimal price, String brandName, Category category, Integer stockQuantity, LocalDate expiryDate) {}
}