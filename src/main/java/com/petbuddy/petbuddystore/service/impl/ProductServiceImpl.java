package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.FileType;
import com.petbuddy.petbuddystore.common.enums.PromotionType;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import com.petbuddy.petbuddystore.common.enums.ProductUnit;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ProductCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBaseResponse;
import com.petbuddy.petbuddystore.dto.response.ProductManagementResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.mapper.ProductMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.repository.ProductRepository;
import com.petbuddy.petbuddystore.repository.PromotionDetailRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.AuditService;
import com.petbuddy.petbuddystore.service.CategoryService;
import com.petbuddy.petbuddystore.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;
    ProductBatchRepository productBatchRepository;
    PromotionDetailRepository promotionDetailRepository;
    CategoryService categoryService;
    ProductMapper productMapper;
    AuditService auditService;
    UserRepository userRepository;

    @Override
    @Transactional
    public ProductManagementResponse createProduct(ProductCreationRequest request) {
        String productName = normalizeName(request.getName());
        if (productRepository.existsByNameIgnoreCaseAndStatusNot(productName, ProductStatus.DELETED)) {
            throw new AppException(ErrorCode.PRODUCT_EXISTED);
        }
        Category category = categoryService.getActiveCategoryEntityById(request.getCategoryId());
        Product product = productMapper.toProduct(request);
        product.setProductCode(generateProductCode());
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        User currentUser = getCurrentUser();
        auditService.logProductCreate(savedProduct, "CREATE_PRODUCT", null, currentUser);
        return productMapper.toManagementResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductManagementResponse updateProduct(UUID productId, ProductUpdateRequest request) {
        Product product = getProductEntityById(productId);
        Product oldProduct = productMapper.cloneProduct(product);
        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = normalizeName(request.getName());
            productRepository.findByNameIgnoreCaseAndStatusNot(newName, ProductStatus.DELETED)
                    .filter(existed -> !existed.getProductId().equals(productId))
                    .ifPresent(existed -> { throw new AppException(ErrorCode.PRODUCT_EXISTED); });
            request.setName(newName);
        }
        if (request.getSalePrice() != null) {
            BigDecimal maxBasePrice = productBatchRepository.findMaxBasePriceByProductId(productId, List.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE));
            if (maxBasePrice != null && request.getSalePrice().compareTo(maxBasePrice) < 0) {
                throw new AppException(ErrorCode.SALE_PRICE_LESS_THAN_BASE_PRICE);
            }
        }
        if (ProductStatus.DELETED.equals(request.getStatus())) {
            boolean hasBatch = productBatchRepository.existsByProduct_ProductIdAndStatusIn(productId, List.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE));
            if (hasBatch) {throw new AppException(ErrorCode.PRODUCT_HAS_BATCHES);}
        }
        productMapper.updateProduct(product, request);
        if (request.getCategoryId() != null) {product.setCategory(categoryService.getActiveCategoryEntityById(request.getCategoryId()));}
        if (request.getStatus() != null) {
            if (ProductStatus.DELETED.equals(request.getStatus())) {
                product.setDeletedAt(LocalDateTime.now());
            }
            product.setStatus(request.getStatus());
        }
        Product updatedProduct = productRepository.save(product);
        auditService.logProductUpdate(oldProduct, updatedProduct, request.getReason(), request.getNote(), getCurrentUser());
        return productMapper.toManagementResponse(updatedProduct);
    }
    @Override
    @Transactional
    public Product createProductFromImport(String name, String description, BigDecimal sale_price, String brandName, Category category, String ingredients, String usageInstructions, ProductUnit unit, List<MediaFile> mediaFiles) {
        Product product = Product.builder()
                .name(name.trim())
                .description(description)
                .salePrice(sale_price)
                .brandName(brandName)
                .category(category)
                .ingredients(ingredients)
                .usageInstructions(usageInstructions)
                .unit(unit)
                .productCode(generateProductCode())
                .status(ProductStatus.ACTIVE)
                .mediaFiles(mediaFiles != null ? mediaFiles : new ArrayList<>())
                .build();

        if (mediaFiles != null) {
            mediaFiles.forEach(mf -> mf.setProduct(product));
        }

        Product savedProduct = productRepository.save(product);
        User currentUser = getCurrentUser();
        auditService.logProductCreate(savedProduct, "CREATE_PRODUCT_IMPORT", null, currentUser);
        return savedProduct;
    }

    @Override
    @Transactional
    public void updateLastBatchSequence(Product product, long lastBatchSequence) {
        product.setLastBatchSequence(lastBatchSequence);
        productRepository.save(product);
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
    @Transactional(readOnly = true)
    public Page<ProductPublicResponse> getProductsForUser(String keyword, Long categoryId, String brandName, String sortBy, Pageable pageable) {
        Pageable sortedPageable = buildPageable(pageable, sortBy);
        Specification<Product> spec = buildProductSpecForUser(keyword, categoryId, brandName, ProductStatus.ACTIVE);

        return productRepository.findAll(spec, sortedPageable)
                .map(product -> {
                    ProductPublicResponse response = productMapper.toListResponse(product);
                    setPromotionInfo(response, product);
                    response.setThumbnailUrl(getThumbnailUrl(product));
                    return response;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductManagementResponse> getProductsForManagement(String keyword, Long categoryId, String brandName, ProductStatus status, String sortBy, Pageable pageable, Integer nearExpiredDays) {
        Pageable sortedPageable = buildPageable(pageable, sortBy);
        Specification<Product> spec = buildProductSpecForManager(keyword, categoryId, brandName, status, nearExpiredDays);

        return productRepository.findAll(spec, sortedPageable)
                .map(product -> {
                    ProductManagementResponse response = productMapper.toManagementResponse(product);
                    response.setTotalStock(getTotalStock(product));
                    response.setBatchCount(getBatchCount(product));
                    setNearExpiredInfo(product, response, nearExpiredDays);
                    setPromotionInfo(response, product);
                    response.setThumbnailUrl(getThumbnailUrl(product));
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
    @Transactional(readOnly = true)
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

    private String getThumbnailUrl(Product product) {
        if (product == null || product.getMediaFiles() == null || product.getMediaFiles().isEmpty()) {
            return null;
        }

        if (product.getThumbnailMediaId() != null) {
            return product.getMediaFiles().stream()
                    .filter(media -> Objects.equals(media.getMediaFileId(), product.getThumbnailMediaId()))
                    .map(MediaFile::getFileUrl)
                    .findFirst()
                    .orElse(null);
        }

        return product.getMediaFiles().stream()
                .filter(media -> media.getFileType() == FileType.IMAGE && media.getMediaFileId() != null)
                .max(Comparator.comparing(MediaFile::getMediaFileId))
                .map(MediaFile::getFileUrl)
                .orElse(null);
    }

    private Pageable buildPageable(Pageable pageable, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "date_desc";
        }
        Sort sort;
        switch (sortBy) {
            case "price_asc":
                sort = Sort.by(Sort.Direction.ASC, "salePrice");
                break;
            case "price_desc":
                sort = Sort.by(Sort.Direction.DESC, "salePrice");
                break;
            case "date_asc":
                sort = Sort.by(Sort.Direction.ASC, "createdAt");
                break;
            case "date_desc":
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            default:
                log.warn("Invalid sortBy value: {}, using default date_desc", sortBy);
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private String generateProductCode() {
        String code;
        do {
            code = "PRD" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (productRepository.existsByProductCode(code));
        return code;
    }

    private Specification<Product> buildProductSpecForUser(String keyword, Long categoryId, String brandName, ProductStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String[] terms = keyword.trim().toLowerCase().split("\\s+");
                for (String term : terms) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + term + "%"));
                }
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (brandName != null && !brandName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("brandName")), "%" + brandName.trim().toLowerCase() + "%"));
            }

            predicates.add(status != null ? cb.equal(root.get("status"), status) : cb.notEqual(root.get("status"), ProductStatus.DELETED));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Product> buildProductSpecForManager(String keyword, Long categoryId, String brandName, ProductStatus status, Integer nearExpiredDays) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String[] terms = keyword.trim().toLowerCase().split("\\s+");
                for (String term : terms) {
                    String searchTerm = "%" + term + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), searchTerm),
                            cb.like(cb.lower(root.get("productCode")), searchTerm)
                    ));
                }
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }
            if (brandName != null && !brandName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("brandName")), "%" + brandName.trim().toLowerCase() + "%"));
            }

            predicates.add(status != null ? cb.equal(root.get("status"), status) : cb.notEqual(root.get("status"), ProductStatus.DELETED));

            // ⭐ Filter sản phẩm gần hết hạn
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
                        // ⭐ SỬA: Nếu expiryDate null thì coi như không bao giờ hết hạn
                        cb.or(
                                cb.isNull(batchRoot.get("expiryDate")),
                                cb.lessThanOrEqualTo(batchRoot.get("expiryDate"), threshold)
                        )
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

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private <T extends ProductBaseResponse> T setPromotionInfo(T response, Product product) {
        Optional<PromotionDetail> detailOpt = promotionDetailRepository.findByProduct_ProductIdAndPromotion_Status(
                product.getProductId(), PromotionStatus.ACTIVE);

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
                product.getSalePrice(),
                detail.getPromotionType(),
                detail.getDiscountValue()
        );

        response.setHasActivePromotion(true);
        response.setPromotionName(promotion.getName());
        response.setPromotionType(detail.getPromotionType());
        response.setDiscountValue(detail.getDiscountValue());
        response.setDiscountAmount(discountAmount);
        response.setPromotionPrice(product.getSalePrice().subtract(discountAmount).max(BigDecimal.ZERO));

        return response;
    }

    private BigDecimal calculateDiscountAmount(BigDecimal price, PromotionType type, BigDecimal value) {
        if (price == null || value == null) {
            return BigDecimal.ZERO;
        }

        if (type == PromotionType.PERCENTAGE) {
            return price.multiply(value)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else if (type == PromotionType.FIXED_AMOUNT) {
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