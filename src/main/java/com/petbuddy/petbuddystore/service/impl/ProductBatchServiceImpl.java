package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ImportRowRequest;
import com.petbuddy.petbuddystore.dto.request.ProductBatchCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductBatchUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ProductImportRequest;
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
import org.apache.poi.ss.util.CellReference;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductBatchServiceImpl implements ProductBatchService {

    static final int MAX_BATCH_CREATE_LIMIT = 10;
    static final int IMAGE_COL_START = 9;
    static final int IMAGE_COL_END = 12;

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
        return productBatchRepository.saveAllAndFlush(batches).stream().map(this::buildBatchResponse).toList();
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
    @Transactional
    public ProductBatchResponse updateBatch(UUID batchId, ProductBatchUpdateRequest request) {
        ProductBatch batch = getBatchEntityById(batchId);
        if (request.getStockQuantity() != null) {
            if (request.getStockQuantity() < 0) throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
            batch.setStockQuantity(request.getStockQuantity());
        }
        if (request.getExpiryDate() != null) batch.setExpiryDate(request.getExpiryDate());
        if (request.getStatus() != null) updateStatus(batch, request.getStatus());
        return buildBatchResponse(productBatchRepository.save(batch));
    }

    @Override
    @Transactional
    public void deleteDeletedBatchesOlderThan90Days() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        List<ProductBatch> oldDeletedBatches = productBatchRepository.findByStatusAndDeletedAtBefore(ProductStatus.DELETED, threshold);
        productBatchRepository.deleteAll(oldDeletedBatches);
    }

    // ==================== IMPORT ====================
    @Override
    @Transactional
    public ProductImportResponse importProductsAndBatches(ProductImportRequest request) {
        MultipartFile file = request.getFile();
        boolean confirm = request.isConfirm();
        validateExcelFile(file);
        log.info("Import file size: {}", file.getSize());

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<ImportRowRequest> validRows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet.getRow(0));

            byte[] excelBytes = file.getBytes();
            Map<Integer, List<byte[]>> rowImagesMap = getAllImagesByRow(excelBytes);

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

                if (name == null || name.isBlank()) { errors.add("Row " + rowNum + ": Product name is required."); continue; }
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) { errors.add("Row " + rowNum + ": Product price is invalid."); continue; }
                if (categoryName == null || categoryName.isBlank()) { errors.add("Row " + rowNum + ": Category name is required."); continue; }
                if (stockQuantity == null || stockQuantity < 0) { errors.add("Row " + rowNum + ": Stock quantity is invalid."); continue; }
                if (expiryDate != null && !expiryDate.isAfter(LocalDate.now())) { errors.add("Row " + rowNum + ": Expiry date is invalid."); continue; }

                Category category;
                try {
                    category = categoryService.getActiveCategoryEntityByName(categoryName);
                } catch (AppException e) {
                    errors.add("Row " + rowNum + ": Category '" + categoryName + "' does not exist or is inactive."); continue;
                }
                Product existingProduct = productService.getProductEntityByName(name);
                if (existingProduct != null) {
                    if (existingProduct.getStatus() == ProductStatus.INACTIVE) {
                        errors.add("Row " + rowNum + ": Product '" + name + "' is inactive. Please activate it first.");continue;
                    }
                    if (hasDifferentInfo(existingProduct, description, price, brandName, category, ingredients, usageInstructions)) {
                        warnings.add("Row " + rowNum + ": Product '" + name + "' already exists. Old info kept.");
                    }
                }

                List<byte[]> rowImages = rowImagesMap.getOrDefault(i, Collections.emptyList());
                validRows.add(new ImportRowRequest(rowNum, name, description, price, brandName, category,
                        stockQuantity, expiryDate, ingredients, usageInstructions, rowImages));
            }

            if (!errors.isEmpty() || !confirm) {
                return ProductImportResponse.builder()
                        .canImport(errors.isEmpty()).createdProducts(0).createdBatches(0)
                        .warnings(warnings).errors(errors).build();
            }

            int createdProducts = 0, createdBatches = 0;
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
                        .product(product).stockQuantity(rowData.getStockQuantity())
                        .expiryDate(rowData.getExpiryDate()).status(ProductStatus.ACTIVE).build();
                productBatchRepository.save(batch);
                productService.updateLastBatchSequence(product, nextNumber);
                batchCounter.put(product.getProductId(), nextNumber + 1);
                createdBatches++;
            }

            return ProductImportResponse.builder()
                    .canImport(true).createdProducts(createdProducts).createdBatches(createdBatches)
                    .warnings(warnings).errors(errors).build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Import products and batches failed", e);
            throw new AppException(ErrorCode.IMPORT_FAILED);
        }
    }

    private Map<Integer, List<byte[]>> getAllImagesByRow(byte[] excelBytes) throws Exception {
        Map<Integer, List<byte[]>> imageMap = new HashMap<>();
        Map<String, byte[]> zipEntries = readZipEntries(excelBytes);

        byte[] relsBytes = zipEntries.get("xl/_rels/cellimages.xml.rels");
        byte[] cellImagesBytes = zipEntries.get("xl/cellimages.xml");
        byte[] sheetBytes = zipEntries.get("xl/worksheets/sheet1.xml");
        if (relsBytes == null || cellImagesBytes == null || sheetBytes == null) return imageMap;

        // rId → path
        Map<String, String> rIdToPath = new HashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Id=\"(rId\\d+)\"[^>]*Target=\"([^\"]+)\"").matcher(new String(relsBytes));
        while (m.find()) rIdToPath.put(m.group(1), "xl/" + m.group(2));

        // imageId → rId
        String xml = new String(cellImagesBytes);
        List<String> ids = new ArrayList<>(), rids = new ArrayList<>();
        java.util.regex.Matcher nm = java.util.regex.Pattern.compile("name=\"(ID_[A-F0-9]+)\"").matcher(xml);
        java.util.regex.Matcher em = java.util.regex.Pattern.compile("r:embed=\"(rId\\d+)\"").matcher(xml);
        while (nm.find()) ids.add(nm.group(1));
        while (em.find()) rids.add(em.group(1));
        Map<String, String> imageIdToRid = new HashMap<>();
        for (int i = 0; i < Math.min(ids.size(), rids.size()); i++) imageIdToRid.put(ids.get(i), rids.get(i));

        // Cache ảnh
        Map<String, byte[]> rIdToBytes = new HashMap<>();

        // Duyệt sheet1.xml
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        org.w3c.dom.Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(sheetBytes));
        org.w3c.dom.NodeList cells = doc.getElementsByTagName("c");

        for (int i = 0; i < cells.getLength(); i++) {
            org.w3c.dom.Element cell = (org.w3c.dom.Element) cells.item(i);
            String ref = cell.getAttribute("r");
            org.w3c.dom.NodeList fl = cell.getElementsByTagName("f");
            if (fl.getLength() == 0) continue;
            String formula = fl.item(0).getTextContent();
            if (!formula.contains("DISPIMG")) continue;

            int s = formula.indexOf('"'), e = formula.indexOf('"', s + 1);
            String imageId = formula.substring(s + 1, e);
            CellReference cr = new CellReference(ref);
            int row = cr.getRow(), col = cr.getCol();

            if (col >= IMAGE_COL_START && col <= IMAGE_COL_END) {
                String rId = imageIdToRid.get(imageId);
                if (rId != null) {
                    byte[] img = rIdToBytes.get(rId);
                    if (img == null) {
                        String path = rIdToPath.get(rId);
                        if (path != null) { img = zipEntries.get(path); if (img != null) rIdToBytes.put(rId, img); }
                    }
                    if (img != null) imageMap.computeIfAbsent(row, k -> new ArrayList<>()).add(img);
                }
            }
        }
        return imageMap;
    }

    // ==================== HELPERS ====================
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
        for (int i = 0; i <= 8; i++) if (getCellString(row, i) != null) return false;
        return true;
    }

    private void validateHeader(Row header) {
        if (header == null) throw new AppException(ErrorCode.INVALID_EXCEL_TEMPLATE);
        String[] expected = {"Name","Description","Price","BrandName","CategoryName","StockQuantity","ExpiryDate","Ingredients","UsageInstructions","Image1","Image2","Image3","Image4"};
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
            default -> throw new AppException(ErrorCode.INVALID_SORT_OPTION);
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Specification<ProductBatch> buildBatchSpec(UUID productId, String keyword, ProductStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("product").get("productId"), productId));
            if (keyword != null && !keyword.isBlank()) predicates.add(cb.like(cb.lower(root.get("batchCode")), "%" + keyword.trim().toLowerCase() + "%"));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            else predicates.add(cb.notEqual(root.get("status"), ProductStatus.DELETED));
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

    private boolean hasDifferentInfo(Product product, String description, BigDecimal price, String brandName, Category category, String ingredients, String usageInstructions) {
        return !Objects.equals(product.getDescription(), description) || product.getPrice().compareTo(price) != 0
                || !Objects.equals(product.getBrandName(), brandName) || !Objects.equals(product.getCategory().getCategoryId(), category.getCategoryId())
                || !Objects.equals(product.getIngredients(), ingredients) || !Objects.equals(product.getUsageInstructions(), usageInstructions);
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new AppException(ErrorCode.FILE_REQUIRED);
        if (file.getSize() > 10 * 1024 * 1024) throw new AppException(ErrorCode.FILE_TOO_LARGE);
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) throw new AppException(ErrorCode.INVALID_FILE_TYPE);
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

    private Map<String, byte[]> readZipEntries(byte[] excelBytes) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(excelBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) entries.put(entry.getName(), zis.readAllBytes());
            }
        }
        return entries;
    }
}