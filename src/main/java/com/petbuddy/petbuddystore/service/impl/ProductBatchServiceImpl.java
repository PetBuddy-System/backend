package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
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
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.service.CategoryService;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.ProductBatchService;
import com.petbuddy.petbuddystore.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
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
    static final int IMAGE_COL_START = 10;
    static final int IMAGE_COL_END = 13;

    ProductBatchRepository productBatchRepository;
    ProductService productService;
    ProductBatchMapper productBatchMapper;
    CategoryService categoryService;
    FileService fileService;

    @Override
    @Transactional
    public List<ProductBatchResponse> createBatches(UUID productId, List<ProductBatchCreationRequest> requests) {
        validateBatchCreateRequests(requests);
        Product product = productService.getActiveProductEntityById(productId);
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
        return productBatchRepository.saveAllAndFlush(batches).stream()
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

        if (request.getStockQuantity() != null) {
            batch.setStockQuantity(request.getStockQuantity());
        }

        if (request.getCost() != null) {
            batch.setCost(request.getCost());
        }

        if (request.getExpiryDate() != null) {
            batch.setExpiryDate(request.getExpiryDate());
        }

        if (request.getStatus() != null) {
            updateStatus(batch, request.getStatus());
        }

        return productBatchMapper.toProductBatchResponse(productBatchRepository.save(batch));
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
                BigDecimal price = getCellBigDecimal(row, 2);
                String brandName = getCellString(row, 3);
                String categoryName = getCellString(row, 4);
                Integer stockQuantity = getCellInteger(row, 5);
                LocalDate expiryDate = getCellLocalDate(row, 6);
                String ingredients = getCellString(row, 7);
                String usageInstructions = getCellString(row, 8);
                BigDecimal cost = getCellBigDecimal(row, 9);

                if (name == null || name.isBlank()) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_NAME_REQUIRED"));
                }
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(new ProductImportResponse.Error(rowNum, "PRODUCT_PRICE_INVALID"));
                }
                if (categoryName == null || categoryName.isBlank()) {
                    errors.add(new ProductImportResponse.Error(rowNum, "CATEGORY_NAME_REQUIRED"));
                }
                if (stockQuantity == null || stockQuantity < 0) {
                    errors.add(new ProductImportResponse.Error(rowNum, "STOCK_QUANTITY_INVALID"));
                }
                if (expiryDate != null && !expiryDate.isAfter(LocalDate.now())) {
                    errors.add(new ProductImportResponse.Error(rowNum, "EXPIRY_DATE_INVALID"));
                }
                if (cost != null && cost.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(new ProductImportResponse.Error(rowNum, "COST_INVALID"));
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

                List<byte[]> rowImages = rowImagesMap.getOrDefault(i, Collections.emptyList());
                validRows.add(new ImportRowRequest(rowNum, name, description, price, brandName, category,
                        stockQuantity, expiryDate, ingredients, usageInstructions, cost, rowImages));
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

            for (ImportRowRequest rowData : validRows) {
                Product product = productService.getProductEntityByName(rowData.getName());
                if (product == null) {
                    List<MediaFile> mediaFiles = uploadImages(rowData.getImages(), rowData.getName());
                    product = productService.createProductFromImport(
                            rowData.getName(), rowData.getDescription(), rowData.getPrice(), rowData.getBrandName(),
                            rowData.getCategory(), rowData.getIngredients(), rowData.getUsageInstructions(), mediaFiles);
                    createdProducts++;
                }

                Product finalProduct = product;
                long nextNumber = batchCounter.computeIfAbsent(product.getProductId(), id -> finalProduct.getLastBatchSequence() + 1);

                ProductBatch batch = ProductBatch.builder()
                        .batchCode(generateBatchCode(product, nextNumber))
                        .product(product)
                        .stockQuantity(rowData.getStockQuantity())
                        .expiryDate(rowData.getExpiryDate())
                        .cost(rowData.getCost() != null ? rowData.getCost() : BigDecimal.ZERO)
                        .status(ProductStatus.ACTIVE)
                        .build();

                productBatchRepository.save(batch);
                productService.updateLastBatchSequence(product, nextNumber);
                batchCounter.put(product.getProductId(), nextNumber + 1);
                createdBatches++;
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
        try { return new BigDecimal(value.replace(",", "").replace(" ", "")); } catch (Exception e) { return null; }
    }

    private Integer getCellInteger(Row row, int index) {
        String value = getCellString(row, index);
        if (value == null) return null;
        try { return Integer.parseInt(value.replace(",", "").replace(" ", "").split("\\.")[0]); } catch (Exception e) { return null; }
    }

    private LocalDate getCellLocalDate(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = getCellString(row, index);
        if (value == null) return null;
        for (DateTimeFormatter df : List.of(DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("MM/dd/yyyy"), DateTimeFormatter.ofPattern("yyyy-MM-dd"))) {
            try { return LocalDate.parse(value, df); } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isEmpty(Row row) {
        for (int i = 0; i <= 9; i++) {
            if (getCellString(row, i) != null) return false;
        }
        return true;
    }

    private void validateHeader(Row header) {
        if (header == null) throw new AppException(ErrorCode.INVALID_EXCEL_TEMPLATE);
        String[] expected = {"Name","Description","Price","BrandName","CategoryName","StockQuantity","ExpiryDate","Ingredients","UsageInstructions","Cost","Image1","Image2","Image3","Image4"};
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equalsIgnoreCase(getCellString(header, i))) throw new AppException(ErrorCode.INVALID_EXCEL_TEMPLATE);
        }
    }

    private void validateBatchCreateRequests(List<ProductBatchCreationRequest> requests) {
        if (requests == null || requests.isEmpty()) throw new AppException(ErrorCode.BATCH_REQUIRED);
        if (requests.size() > MAX_BATCH_CREATE_LIMIT) throw new AppException(ErrorCode.BATCH_LIMIT_EXCEEDED);
    }

    private void updateStatus(ProductBatch batch, ProductStatus status) {
        batch.setStatus(status);
        batch.setDeletedAt(status == ProductStatus.DELETED ? LocalDateTime.now() : null);
    }

    private String generateBatchCode(Product product, long sequence) {
        long lp = (sequence - 1) / 999, np = (sequence - 1) % 999 + 1;
        char c1 = (char) ('A' + (lp / 26) % 26), c2 = (char) ('A' + lp % 26);
        return product.getProductCode() + "-" + c1 + c2 + String.format("%03d", np);
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
            case "cost_asc" -> Sort.by(Sort.Direction.ASC, "cost");
            case "cost_desc" -> Sort.by(Sort.Direction.DESC, "cost");
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