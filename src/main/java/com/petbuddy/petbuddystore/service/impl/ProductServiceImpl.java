package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ProductCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductDetailResponse;
import com.petbuddy.petbuddystore.dto.response.ProductManagementResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.mapper.ProductMapper;
import com.petbuddy.petbuddystore.model.Category;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.repository.ProductRepository;
import com.petbuddy.petbuddystore.service.CategoryService;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;
    ProductBatchRepository productBatchRepository;
    CategoryService categoryService;
    FileService fileService;
    ProductMapper productMapper;

    @Override
    @Transactional
    public ProductManagementResponse createProduct(ProductCreationRequest request, List<MultipartFile> images) {
        validateImageLimit(images);
        if (productRepository.existsByNameIgnoreCase(request.getName().trim())) {throw new AppException(ErrorCode.PRODUCT_EXISTED);}
        Category category = categoryService.getActiveCategoryEntityById(request.getCategoryId());
        Product product = productMapper.toProduct(request);
        product.setName(request.getName().trim());
        product.setProductCode(generateProductCode());
        product.setCategory(category);
        product.setStatus(ProductStatus.ACTIVE);
        product.setDeletedAt(null);
        if (hasImages(images)) {product.setImageUrls(new ArrayList<>(fileService.uploadProductImages(images)));}
        Product savedProduct = productRepository.save(product);
        return buildManagementResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductPublicResponse> getProductsForUser(String keyword, Long categoryId, String brandName, String sortBy, Pageable pageable) {
        Pageable sortedPageable = buildPageable(pageable, sortBy);
        Specification<Product> spec = buildProductSpec(keyword, categoryId, brandName, ProductStatus.ACTIVE);
        return productRepository.findAll(spec, sortedPageable)
                .map(this::buildPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductManagementResponse> getProductsForManagement(String keyword, Long categoryId, String brandName, ProductStatus status, String sortBy, Pageable pageable) {
        Pageable sortedPageable = buildPageable(pageable, sortBy);

        Specification<Product> spec = buildProductSpec(keyword, categoryId, brandName, status);

        return productRepository.findAll(spec, sortedPageable)
                .map(this::buildManagementResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(UUID productId) {
        Product product = getProductEntityById(productId);
        return buildDetailResponse(product);
    }

    @Override
    @Transactional
    public ProductManagementResponse updateProduct(UUID productId, ProductUpdateRequest request, List<MultipartFile> images) {
        Product product = getProductEntityById(productId);
        List<MultipartFile> safeImages = images == null ? null : new ArrayList<>(images);
        validateImageLimit(safeImages);
        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();

            productRepository.findByNameIgnoreCase(newName)
                    .filter(existedProduct -> !existedProduct.getProductId().equals(productId))
                    .ifPresent(existedProduct -> {throw new AppException(ErrorCode.PRODUCT_EXISTED);});
            product.setName(newName);
        }
        if (request.getDescription() != null) {product.setDescription(request.getDescription());}
        if (request.getPrice() != null) {product.setPrice(request.getPrice());}
        if (request.getBrandName() != null) {product.setBrandName(request.getBrandName());}
        if (request.getCategoryId() != null) {Category category = categoryService.getActiveCategoryEntityById(request.getCategoryId());product.setCategory(category);}
        if (hasImages(safeImages)) {product.setImageUrls(fileService.uploadProductImages(safeImages));}
        if (request.getStatus() != null) {updateStatus(product, request.getStatus());}
        Product savedProduct = productRepository.save(product);
        return buildManagementResponse(savedProduct);
    }

    @Override
    public Product getProductEntityById(UUID productId) {
        return productRepository.findById(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    public Product getActiveProductEntityById(UUID productId) {
        Product product = getProductEntityById(productId);
        if (product.getStatus() == ProductStatus.DELETED) {throw new AppException(ErrorCode.PRODUCT_DELETED);}
        if (product.getStatus() == ProductStatus.INACTIVE) {throw new AppException(ErrorCode.PRODUCT_INACTIVE);}
        return product;
    }

    @Override
    public Product getProductEntityByName(String name) {return productRepository.findByNameIgnoreCase(name.trim()).orElse(null);}

    @Override
    @Transactional
    public Product createProductFromImport(String name, String description, BigDecimal price, String brandName, Category category) {
        Product product = Product.builder()
                .name(name.trim())
                .description(description)
                .price(price)
                .brandName(brandName)
                .category(category)
                .productCode(generateProductCode())
                .status(ProductStatus.ACTIVE)
                .deletedAt(null).build();
        return productRepository.save(product);
    }

    private boolean hasImages(List<MultipartFile> images) {
        return images != null && images.stream().anyMatch(file -> file != null && !file.isEmpty());
    }
    private void validateImageLimit(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {return;}
        long validImageCount = images.stream().filter(file -> file != null && !file.isEmpty()).count();
        if (validImageCount > 4) {throw new AppException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);}
    }
    private void updateStatus(Product product, ProductStatus status) {

        if (status == ProductStatus.DELETED
                && productBatchRepository.existsByProduct_ProductIdAndStatusIn(
                product.getProductId(),
                List.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE)
        )) {
            throw new AppException(ErrorCode.PRODUCT_HAS_BATCHES);
        }
        product.setStatus(status);
        if (status == ProductStatus.DELETED) {
            product.setDeletedAt(LocalDateTime.now());
        } else {product.setDeletedAt(null);}
    }

    private String generateProductCode() {
        String code;
        do {
            code = "PRD" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 6).toUpperCase();
        } while (productRepository.existsByProductCode(code));
        return code;
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

    private Specification<Product> buildProductSpec(String keyword, Long categoryId, String brandName, ProductStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));}
            if (categoryId != null) {predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));}
            if (brandName != null && !brandName.isBlank()) {predicates.add(cb.like(cb.lower(root.get("brandName")), "%" + brandName.trim().toLowerCase() + "%"));}
            if (status != null) {predicates.add(cb.equal(root.get("status"), status));}
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ProductPublicResponse buildPublicResponse(Product product) {
        ProductPublicResponse response = productMapper.toPublicResponse(product);
        response.setThumbnail(getThumbnail(product));
        response.setTotalStock(getTotalStock(product));
        return response;
    }

    private ProductManagementResponse buildManagementResponse(Product product) {
        ProductManagementResponse response = productMapper.toManagementResponse(product);
        response.setProductCode(product.getProductCode());
        response.setThumbnail(getThumbnail(product));
        response.setTotalStock(getTotalStock(product));
        response.setBatchCount(getBatchCount(product));
        return response;
    }

    private ProductDetailResponse buildDetailResponse(Product product) {
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        response.setProductCode(product.getProductCode());
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getCategoryId());
            response.setCategoryName(product.getCategory().getName());
        }
        response.setTotalStock(getTotalStock(product));
        response.setBatchCount(getBatchCount(product));
        return response;
    }

    private String getThumbnail(Product product) {
        if (product.getImageUrls() == null || product.getImageUrls().isEmpty()) {return null;}
        return product.getImageUrls().getFirst();
    }

    private Integer getTotalStock(Product product) {
        if (product.getBatches() == null) {return 0;}
        return product.getBatches()
                .stream().filter(batch -> batch.getStatus() == ProductStatus.ACTIVE)
                .mapToInt(batch -> batch.getStockQuantity() == null ? 0 : batch.getStockQuantity()).sum();
    }

    private Integer getBatchCount(Product product) {if (product.getBatches() == null) {return 0;}
        return (int) product.getBatches().stream().filter(batch -> batch.getStatus() != ProductStatus.DELETED).count();}

}