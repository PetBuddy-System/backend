package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.enums.ProductUnit;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ImportRowRequest;
import com.petbuddy.petbuddystore.dto.request.ProductBatchCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductBatchUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBatchResponse;
import com.petbuddy.petbuddystore.dto.response.ProductImportResponse;
import com.petbuddy.petbuddystore.mapper.ProductBatchMapper;
import com.petbuddy.petbuddystore.model.Category;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.ProductBatch;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.*;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    static final int IMAGE_COL_START = 11;
    static final int IMAGE_COL_END = 14;

    ProductBatchRepository productBatchRepository;
    ProductService productService;
    ProductBatchMapper productBatchMapper;
    CategoryService categoryService;
    FileService fileService;
    AuditService auditService;
    UserRepository userRepository;

    @Override
    @Transactional
    public List<ProductBatchResponse> createBatches(UUID productId, List<ProductBatchCreationRequest> requests) {
        Product product = productService.getActiveProductEntityById(productId);
        for (ProductBatchCreationRequest request : requests) {
            if (request.getBasePrice() != null && product.getSalePrice() != null) {
                if (request.getBasePrice().compareTo(product.getSalePrice()) > 0) {
                    throw new AppException(ErrorCode.BASE_PRICE_GREATER_THAN_SALE_PRICE);
                }
            }
        }
        long sequence = product.getLastBatchSequence();
        List<ProductBatch> batches = new ArrayList<>();
        for (ProductBatchCreationRequest request : requests) {
            sequence++;
            ProductBatch batch = productBatchMapper.toProductBatch(request);
            batch.setProduct(product);
            batch.setBatchCode(generateBatchCode(product, sequence));
            batches.add(batch);
        }
        product.setLastBatchSequence(sequence);
        List<ProductBatch> savedBatches = productBatchRepository.saveAllAndFlush(batches);

        User currentUser = getCurrentUser();
        for (ProductBatch batch : savedBatches) {auditService.logBatchCreate(batch, "CREATE_BATCH", null, currentUser);
        }
        return savedBatches.stream()
                .map(productBatchMapper::toProductBatchResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductBatchResponse> getBatchesByProduct(UUID productId, String keyword, ProductStatus status, String sortBy, Pageable pageable) {
        Product product = productService.getProductEntityById(productId);
        Pageable sortedPageable = buildPageable(pageable, sortBy);
        Specification<ProductBatch> spec = buildBatchSpec(product.getProductId(), keyword, status);
        return productBatchRepository.findAll(spec, sortedPageable)
                .map(productBatchMapper::toProductBatchResponse);
    }

    @Override
    @Transactional
    public ProductBatchResponse updateBatch(UUID batchId, ProductBatchUpdateRequest request) {
        ProductBatch batch = getBatchEntityById(batchId);
        Product product = batch.getProduct();
        if (request.getBasePrice() != null && product.getSalePrice() != null) {
            if (request.getBasePrice().compareTo(product.getSalePrice()) > 0) {
                throw new AppException(ErrorCode.BASE_PRICE_GREATER_THAN_SALE_PRICE);
            }
        }
        ProductBatch oldBatch = productBatchMapper.cloneBatch(batch);
        productBatchMapper.updateBatch(batch, request);
        if (request.getStatus() == ProductStatus.DELETED) {
            batch.setDeletedAt(LocalDateTime.now());
        }
        ProductBatch savedBatch = productBatchRepository.save(batch);
        User currentUser = getCurrentUser();
        auditService.logBatchUpdate(oldBatch, savedBatch, request.getReason(), request.getNote(), currentUser);
        return productBatchMapper.toProductBatchResponse(savedBatch);
    }

    @Override
    @Transactional
    public void deleteDeletedBatchesOlderThan90Days() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        List<ProductBatch> oldDeletedBatches = productBatchRepository.findByStatusAndDeletedAtBefore(ProductStatus.DELETED, threshold);
        productBatchRepository.deleteAll(oldDeletedBatches);
    }

    @Override
    @Transactional
    public ProductImportResponse importProductsAndBatches(MultipartFile file) {
        fileService.validateExcelFile(file);
        log.info("Import file size: {}", file.getSize());

        List<ProductImportResponse.Error> errors = new ArrayList<>();
        List<ImportRowRequest> validRows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet.getRow(0));

            Map<Integer, List<byte[]>> rowImagesMap = getImagesFromSheet(workbook);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmpty(row)) continue;

                int rowNum = i + 1;
                String name = getCellString(row, 0);
                String description = getCellString(row, 1);
                BigDecimal salePrice = getCellBigDecimal(row, 2);
                String brandName = getCellString(row, 3);
                String categoryName = getCellString(row, 4);
                Integer stockQuantity = getCellInteger(row, 5);
                LocalDate expiryDate = getCellLocalDate(row, 6);
                String ingredients = getCellString(row, 7);
                String usageInstructions = getCellString(row, 8);
                BigDecimal basePrice = getCellBigDecimal(row, 9);
                String unitStr = getCellString(row, 10);
                if (name == null || name.isBlank()) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_NAME_REQUIRED"));
                } else if (name.length() > 255) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_NAME_TOO_LONG"));
                }
                if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_PRICE_INVALID"));
                } else if (salePrice.compareTo(new BigDecimal("999999999")) > 0) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_PRICE_TOO_HIGH"));
                }
                if (categoryName == null || categoryName.isBlank()) {
                    errors.add(new ProductImportResponse.Error(rowNum, "CATEGORY_NAME_REQUIRED"));
                } else if (categoryName.length() > 100) {
                    errors.add(new ProductImportResponse.Error(rowNum, "CATEGORY_NAME_TOO_LONG"));
                }
                if (stockQuantity == null || stockQuantity < 0) {
                    errors.add(new ProductImportResponse.Error(rowNum, "STOCK_QUANTITY_INVALID"));
                } else if (stockQuantity > 999999) {
                    errors.add(new ProductImportResponse.Error(rowNum, "STOCK_QUANTITY_TOO_HIGH"));
                }
                if (unitStr == null || unitStr.isBlank()) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_UNIT_REQUIRED"));
                }

                if (description != null && description.length() > 5000) {
                    errors.add(new ProductImportResponse.Error(rowNum, "DESCRIPTION_TOO_LONG"));
                }

                if (brandName != null && brandName.length() > 100) {
                    errors.add(new ProductImportResponse.Error(rowNum, "BRAND_NAME_TOO_LONG"));
                }

                if (ingredients != null && ingredients.length() > 2000) {
                    errors.add(new ProductImportResponse.Error(rowNum, "INGREDIENTS_TOO_LONG"));
                }

                if (usageInstructions != null && usageInstructions.length() > 2000) {
                    errors.add(new ProductImportResponse.Error(rowNum, "USAGE_INSTRUCTIONS_TOO_LONG"));
                }

                if (expiryDate != null && !expiryDate.isAfter(LocalDate.now())) {
                    errors.add(new ProductImportResponse.Error(rowNum, "EXPIRY_DATE_INVALID"));
                }

                if (basePrice != null) {
                    if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(new ProductImportResponse.Error(rowNum, "BASE_PRICE_INVALID"));
                    }
                    if (salePrice != null && basePrice.compareTo(salePrice) >= 0) {
                        errors.add(new ProductImportResponse.Error(rowNum, "BASE_PRICE_MUST_BE_LESS_THAN_SALE_PRICE"));
                    }
                }
                boolean hasRowError = errors.stream().anyMatch(e -> e.row() == rowNum);
                if (hasRowError) continue;

                Category category;
                try {
                    category = categoryService.getActiveCategoryEntityByName(categoryName);
                } catch (AppException e) {
                    errors.add(new ProductImportResponse.Error(rowNum, "CATEGORY_NOT_FOUND"));
                    continue;
                }

                Product existingProduct = productService.getProductEntityByName(name);
                if (existingProduct != null && existingProduct.getStatus() == ProductStatus.INACTIVE) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_INACTIVE"));
                    continue;
                }

                // Parse unit
                ProductUnit unit;
                try {
                    unit = ProductUnit.valueOf(unitStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_UNIT_INVALID"));
                    continue;
                }

                List<byte[]> rowImages = rowImagesMap.getOrDefault(i, Collections.emptyList());
                validRows.add(new ImportRowRequest(rowNum, name, description, salePrice, brandName, category,
                        stockQuantity, expiryDate, ingredients, usageInstructions, basePrice, unit, rowImages));
            }

            if (!errors.isEmpty()) {
                return ProductImportResponse.builder()
                        .success(false)
                        .createdProducts(0)
                        .createdBatches(0)
                        .errors(errors)
                        .build();
            }

            int createdProducts = 0;
            int createdBatches = 0;
            Map<UUID, Long> batchCounter = new HashMap<>();
            User currentUser = getCurrentUser();

            for (ImportRowRequest rowData : validRows) {
                Product product = productService.getProductEntityByName(rowData.getName());
                if (product == null) {
                    List<MediaFile> mediaFiles = uploadImages(rowData.getImages(), rowData.getName());
                    product = productService.createProductFromImport(
                            rowData.getName(), rowData.getDescription(), rowData.getSalePrice(), rowData.getBrandName(),
                            rowData.getCategory(), rowData.getIngredients(), rowData.getUsageInstructions(),
                            rowData.getUnit(),
                            mediaFiles);
                    createdProducts++;
                }

                Product finalProduct = product;
                long nextNumber = batchCounter.computeIfAbsent(product.getProductId(), id -> finalProduct.getLastBatchSequence() + 1);

                ProductBatch batch = ProductBatch.builder()
                        .batchCode(generateBatchCode(product, nextNumber))
                        .product(product)
                        .stockQuantity(rowData.getStockQuantity())
                        .expiryDate(rowData.getExpiryDate())
                        .basePrice(rowData.getBasePrice() != null ? rowData.getBasePrice() : BigDecimal.ZERO)
                        .status(ProductStatus.ACTIVE)
                        .build();

                productBatchRepository.save(batch);
                productService.updateLastBatchSequence(product, nextNumber);
                batchCounter.put(product.getProductId(), nextNumber + 1);
                createdBatches++;

                auditService.logBatchCreate(batch, "CREATE_BATCH_IMPORT", null, currentUser);
            }

            return ProductImportResponse.builder()
                    .success(true)
                    .createdProducts(createdProducts)
                    .createdBatches(createdBatches)
                    .errors(Collections.emptyList())
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Import products and batches failed", e);
            throw new AppException(ErrorCode.IMPORT_FAILED);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String userId = authentication.getName();
        if (userId == null || userId.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with userId: '{}'", userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });
    }

    private Map<Integer, List<byte[]>> getImagesFromSheet(Workbook workbook) {
        Map<Integer, Map<Integer, byte[]>> rowColImageMap = new TreeMap<>();

        if (workbook instanceof XSSFWorkbook) {
            XSSFWorkbook xssfWorkbook = (XSSFWorkbook) workbook;
            XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                return new HashMap<>();
            }
            for (XSSFShape shape : drawing.getShapes()) {
                if (shape instanceof XSSFPicture) {
                    XSSFPicture picture = (XSSFPicture) shape;
                    XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getAnchor();
                    int row = anchor.getRow1();
                    int col = anchor.getCol1();
                    if (col >= IMAGE_COL_START && col <= IMAGE_COL_END) {
                        byte[] imageData = picture.getPictureData().getData();
                        rowColImageMap.computeIfAbsent(row, k -> new HashMap<>()).putIfAbsent(col, imageData);
                    }
                }
            }
        }
        Map<Integer, List<byte[]>> result = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, byte[]>> rowEntry : rowColImageMap.entrySet()) {
            int row = rowEntry.getKey();
            Map<Integer, byte[]> colMap = rowEntry.getValue();
            List<byte[]> sortedImages = new ArrayList<>();
            for (int col = IMAGE_COL_START; col <= IMAGE_COL_END; col++) {
                if (colMap.containsKey(col)) {
                    sortedImages.add(colMap.get(col));
                }
            }
            if (!sortedImages.isEmpty()) {
                result.put(row, sortedImages);
            }
        }
        return result;
    }

    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        String value = new DataFormatter().formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }

    private BigDecimal getCellBigDecimal(Row row, int index) {
        String value = getCellString(row, index);
        if (value == null) return null;
        try {
            return new BigDecimal(value.replace(",", "").replace(" ", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getCellInteger(Row row, int index) {
        String value = getCellString(row, index);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.replace(",", "").replace(" ", "").split("\\.")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getCellLocalDate(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = getCellString(row, index);
        if (value == null) return null;
        for (DateTimeFormatter df : List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"))) {
            try {
                return LocalDate.parse(value, df);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isEmpty(Row row) {
        for (int i = 0; i <= 10; i++) {
            if (getCellString(row, i) != null) return false;
        }
        return true;
    }

    private void validateHeader(Row header) {
        if (header == null) throw new AppException(ErrorCode.INVALID_EXCEL_TEMPLATE);
        String[] expected = {"Name","Description","SalePrice","BrandName","CategoryName","StockQuantity","ExpiryDate","Ingredients","UsageInstructions","BasePrice","Unit","Image1","Image2","Image3","Image4"};
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equalsIgnoreCase(getCellString(header, i)))
                throw new AppException(ErrorCode.INVALID_EXCEL_TEMPLATE);
        }
    }

    private String generateBatchCode(Product product, long sequence) {
        long lp = (sequence - 1) / 999, np = (sequence - 1) % 999 + 1;
        char c1 = (char) ('A' + (lp / 26) % 26), c2 = (char) ('A' + lp % 26);
        return product.getProductCode() + "-" + c1 + c2 + String.format("%03d", np);
    }

    private ProductBatch getBatchEntityById(UUID batchId) {
        return productBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_FOUND));
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
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), ProductStatus.DELETED));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<MediaFile> uploadImages(List<byte[]> imagesData, String productName) {
        if (imagesData == null || imagesData.isEmpty()) return Collections.emptyList();
        List<MediaFile> mediaFiles = new ArrayList<>();
        for (byte[] data : imagesData) {
            try {
                MediaFile mediaFile = fileService.uploadProductImageFromBytes(data);
                mediaFiles.add(mediaFile);
            } catch (Exception e) {
                log.warn("Skipped image upload for '{}': {}", productName, e.getMessage());
            }
        }
        return mediaFiles;
    }
}