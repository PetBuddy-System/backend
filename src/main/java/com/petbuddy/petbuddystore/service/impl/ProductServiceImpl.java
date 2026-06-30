package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ProductCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBaseResponse;
import com.petbuddy.petbuddystore.dto.response.ProductManagementResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.mapper.ProductMapper;
import com.petbuddy.petbuddystore.mapper.PromotionMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.repository.ProductRepository;
import com.petbuddy.petbuddystore.repository.PromotionDetailRepository;
import com.petbuddy.petbuddystore.service.CategoryService;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.ProductService;
import com.petbuddy.petbuddystore.service.PromotionService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;
    ProductBatchRepository productBatchRepository;
    PromotionDetailRepository promotionDetailRepository;
    CategoryService categoryService;
    FileService fileService;
    ProductMapper productMapper;
    PromotionMapper promotionMapper;
    PromotionService promotionService;

    @Override
    @Transactional
    public ProductManagementResponse createProduct(ProductCreationRequest request, List<MultipartFile> images) {
        String productName = normalizeName(request.getName());
        if (productRepository.existsByNameIgnoreCaseAndStatusNot(productName, ProductStatus.DELETED)) {
            throw new AppException(ErrorCode.PRODUCT_EXISTED);
        }
        Category category = categoryService.getActiveCategoryEntityById(request.getCategoryId());
        Product product = productMapper.toProduct(request);
        product.setProductCode(generateProductCode());
        product.setCategory(category);
        uploadProductMediaFiles(product, images);
        return productMapper.toManagementResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductManagementResponse updateProduct(UUID productId, ProductUpdateRequest request, List<MultipartFile> images) {
        Product product = getProductEntityById(productId);
        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = normalizeName(request.getName());
            productRepository.findByNameIgnoreCaseAndStatusNot(newName, ProductStatus.DELETED)
                    .filter(existed -> !existed.getProductId().equals(productId))
                    .ifPresent(existed -> { throw new AppException(ErrorCode.PRODUCT_EXISTED); });
            request.setName(newName);
        }
        productMapper.updateProduct(product, request);

        if (request.getCategoryId() != null) {
            product.setCategory(categoryService.getActiveCategoryEntityById(request.getCategoryId()));
        }
        if (request.getStatus() != null) {
            updateStatus(product, request.getStatus());
        }

        // SỬA: Upload images và set mediaFiles
        uploadProductMediaFiles(product, images);

        return productMapper.toManagementResponse(productRepository.save(product));
    }

    @Override
    public Product getProductEntityById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    public Product getActiveProductEntityById(UUID productId) {
        Product product = getProductEntityById(productId);
        if (product.getStatus() == ProductStatus.DELETED) {
            throw new AppException(ErrorCode.PRODUCT_DELETED);
        }
        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_INACTIVE);
        }
        return product;
    }

    @Override
    public Product getProductEntityByName(String name) {
        return productRepository.findByNameIgnoreCaseAndStatusNot(normalizeName(name), ProductStatus.DELETED)
                .orElse(null);
    }

    @Override
    @Transactional
    public Product createProductFromImport(String name, String description, BigDecimal price, String brandName, Category category, String ingredients, String usageInstructions, List<MediaFile> mediaFiles) {
        Product product = Product.builder()
                .name(name.trim())
                .description(description)
                .price(price)
                .brandName(brandName)
                .category(category)
                .ingredients(ingredients)
                .usageInstructions(usageInstructions)
                .productCode(generateProductCode())
                .status(ProductStatus.ACTIVE)
                .mediaFiles(mediaFiles != null ? mediaFiles : new ArrayList<>())
                .build();

        if (mediaFiles != null) {
            mediaFiles.forEach(mf -> mf.setProduct(product));
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void updateLastBatchSequence(Product product, long lastBatchSequence) {
        product.setLastBatchSequence(lastBatchSequence);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductPublicResponse> getProductsForUser(String keyword, Long categoryId, String brandName, String sortBy, Pageable pageable) {
        Pageable sortedPageable = buildPageable(pageable, sortBy);
        Specification<Product> spec = buildProductSpec(keyword, categoryId, brandName, ProductStatus.ACTIVE);
        return productRepository.findAll(spec, sortedPageable)
                .map(product -> {ProductPublicResponse response = productMapper.toPublicResponse(product);
                    response.setTotalStock(getTotalStock(product));
                    response.setHasActivePromotion(promotionService.hasActivePromotion(product.getProductId()));
                    return response;});
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductManagementResponse> getProductsForManagement(String keyword, Long categoryId, String brandName, ProductStatus status, String sortBy, Pageable pageable, Integer nearExpiredDays) {
        Pageable sortedPageable = buildPageable(pageable, sortBy);
        Specification<Product> spec = buildProductSpec(keyword, categoryId, brandName, status, nearExpiredDays);
        return productRepository.findAll(spec, sortedPageable)
                .map(product -> {
                    ProductManagementResponse response = productMapper.toManagementResponse(product);
                    response.setTotalStock(getTotalStock(product));
                    response.setBatchCount(getBatchCount(product));
                    setNearExpiredInfo(product, response, nearExpiredDays);
                    response.setHasActivePromotion(promotionService.hasActivePromotion(product.getProductId()));
                    return response;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPublicResponse getProduct(UUID productId) {
        Product product = getProductEntityById(productId);
        ProductPublicResponse response = productMapper.toDetailPublicResponse(product);
        Optional.ofNullable(product.getCategory()).ifPresent(cat -> {
            response.setCategoryId(cat.getCategoryId());
            response.setCategoryName(cat.getName());
        });
        response.setTotalStock(getTotalStock(product));
        return setPromotionInfo(response, product);
    }

    @Override
    public ProductManagementResponse getProductManagement(UUID productId) {
        Product product = getProductEntityById(productId);
        ProductManagementResponse response = productMapper.toManagementResponse(product);
        Optional.ofNullable(product.getCategory()).ifPresent(cat -> {
            response.setCategoryId(cat.getCategoryId());
            response.setCategoryName(cat.getName());
        });
        response.setTotalStock(getTotalStock(product));
        response.setBatchCount(getBatchCount(product));
        return setPromotionInfo(response, product);
    }

    private Pageable buildPageable(Pageable pageable, String sortBy) {
        Sort sort = switch (sortBy == null ? "date_desc" : sortBy) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "date_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new AppException(ErrorCode.INVALID_SORT_OPTION);
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private void updateStatus(Product product, ProductStatus status) {
        if (status == ProductStatus.DELETED) {
            boolean hasActiveBatches = productBatchRepository.existsByProduct_ProductIdAndStatusIn(
                    product.getProductId(), List.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE));
            if (hasActiveBatches) {
                throw new AppException(ErrorCode.PRODUCT_HAS_BATCHES);
            }
            product.setDeletedAt(LocalDateTime.now());
        } else {
            product.setDeletedAt(null);
        }
        product.setStatus(status);
    }

    private String generateProductCode() {
        String code;
        do {
            code = "PRD" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (productRepository.existsByProductCode(code));
        return code;
    }

    private Specification<Product> buildProductSpec(String keyword, Long categoryId, String brandName, ProductStatus status) {
        return buildProductSpec(keyword, categoryId, brandName, status, null);
    }

    private Specification<Product> buildProductSpec(String keyword, Long categoryId, String brandName, ProductStatus status, Integer nearExpiredDays) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }
            if (brandName != null && !brandName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("brandName")), "%" + brandName.trim().toLowerCase() + "%"));
            }

            predicates.add(status != null ? cb.equal(root.get("status"), status) : cb.notEqual(root.get("status"), ProductStatus.DELETED));

            if (nearExpiredDays != null) {
                LocalDate threshold = LocalDate.now().plusDays(nearExpiredDays);
                var subquery = query.subquery(Long.class);
                var batchRoot = subquery.from(ProductBatch.class);
                subquery.select(cb.literal(1L));
                subquery.where(
                        cb.equal(batchRoot.get("product"), root),
                        cb.equal(batchRoot.get("status"), ProductStatus.ACTIVE),
                        cb.isNull(batchRoot.get("deletedAt")),
                        cb.greaterThan(batchRoot.get("stockQuantity"), 0),
                        cb.isNotNull(batchRoot.get("expiryDate")),
                        cb.lessThanOrEqualTo(batchRoot.get("expiryDate"), threshold)
                );
                predicates.add(cb.exists(subquery));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Integer getTotalStock(Product product) {
        if (product.getBatches() == null) return 0;
        return product.getBatches().stream()
                .filter(b -> b.getStatus() == ProductStatus.ACTIVE)
                .mapToInt(b -> b.getStockQuantity() != null ? b.getStockQuantity() : 0)
                .sum();
    }

    private Integer getBatchCount(Product product) {
        if (product.getBatches() == null) return 0;
        return (int) product.getBatches().stream()
                .filter(b -> b.getStatus() != ProductStatus.DELETED)
                .count();
    }

    private void uploadProductMediaFiles(Product product, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return;
        if (images.size() > 4) {
            throw new AppException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);
        }

        List<MediaFile> mediaFiles = images.stream()
                .filter(img -> img != null && !img.isEmpty())
                .map(fileService::uploadProductImage)
                .toList();

        if (!mediaFiles.isEmpty()) {
            mediaFiles.forEach(mf -> mf.setProduct(product));

            if (product.getMediaFiles() == null) {
                product.setMediaFiles(new ArrayList<>());
            }
            product.getMediaFiles().addAll(mediaFiles);
        }
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private <T extends ProductBaseResponse> T setPromotionInfo(T response, Product product) {

        Optional<PromotionDetail> detailOpt = promotionDetailRepository.findByProduct_ProductIdAndPromotion_Status(product.getProductId(), PromotionStatus.ACTIVE);
        if (detailOpt.isEmpty()) {
            response.setHasActivePromotion(false);
            return response;
        }

        PromotionDetail detail = detailOpt.get();
        var promotion = detail.getPromotion();

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            response.setHasActivePromotion(false);
            return response;
        }

        BigDecimal discountAmount = calculateDiscountAmount(
                product.getPrice(),
                detail.getDiscountType(),
                detail.getDiscountValue()
        );

        promotionMapper.updatePromotionInfo(response, promotion, detail);
        response.setHasActivePromotion(true);
        response.setDiscountAmount(discountAmount);
        response.setSalePrice(product.getPrice().subtract(discountAmount).max(BigDecimal.ZERO));
        return response;
    }

    private BigDecimal calculateDiscountAmount(BigDecimal price, DiscountType type, BigDecimal value) {
        if (price == null || value == null) {
            return BigDecimal.ZERO;
        }

        if (type == DiscountType.PERCENTAGE) {
            return price.multiply(value)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else if (type == DiscountType.FIXED) {
            return value.min(price);
        }
        return BigDecimal.ZERO;
    }

    private void setNearExpiredInfo(Product product, ProductManagementResponse response, Integer nearExpiredDays) {
        if (product.getBatches() == null || product.getBatches().isEmpty()) {
            response.setNearExpiredStock(0L);
            response.setNearExpiredBatchCount(0L);
            response.setNearestExpiryDate(null);
            return;
        }

        List<ProductBatch> activeBatches = product.getBatches().stream()
                .filter(b -> b.getStatus() == ProductStatus.ACTIVE)
                .filter(b -> b.getStockQuantity() != null && b.getStockQuantity() > 0)
                .filter(b -> b.getExpiryDate() != null)
                .toList();

        if (activeBatches.isEmpty()) {
            response.setNearExpiredStock(0L);
            response.setNearExpiredBatchCount(0L);
            response.setNearestExpiryDate(null);
            return;
        }

        LocalDate nearestExpiry = activeBatches.stream()
                .map(ProductBatch::getExpiryDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate threshold;
        if (nearExpiredDays != null && nearExpiredDays > 0) {
            threshold = LocalDate.now().plusDays(nearExpiredDays);
        } else {
            threshold = LocalDate.now();
        }

        long nearExpiredStock = activeBatches.stream()
                .filter(b -> {
                    if (nearExpiredDays != null && nearExpiredDays > 0) {
                        return b.getExpiryDate().isBefore(threshold) || b.getExpiryDate().isEqual(threshold);
                    } else {
                        return b.getExpiryDate().isAfter(LocalDate.now());
                    }
                })
                .mapToLong(b -> b.getStockQuantity() != null ? b.getStockQuantity() : 0)
                .sum();

        long nearExpiredBatchCount = activeBatches.stream()
                .filter(b -> {
                    if (nearExpiredDays != null && nearExpiredDays > 0) {
                        return b.getExpiryDate().isBefore(threshold) || b.getExpiryDate().isEqual(threshold);
                    } else {
                        return b.getExpiryDate().isAfter(LocalDate.now());
                    }
                })
                .count();

        response.setNearExpiredStock(nearExpiredStock);
        response.setNearExpiredBatchCount(nearExpiredBatchCount);
        response.setNearestExpiryDate(nearestExpiry);
    }
}