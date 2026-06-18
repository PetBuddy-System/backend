package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.CategoryStatus;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CategoryCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CategoryUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CategoryResponse;
import com.petbuddy.petbuddystore.mapper.CategoryMapper;
import com.petbuddy.petbuddystore.model.Category;
import com.petbuddy.petbuddystore.repository.CategoryRepository;
import com.petbuddy.petbuddystore.repository.ProductRepository;
import com.petbuddy.petbuddystore.service.CategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    ProductRepository productRepository;
    CategoryMapper categoryMapper;

    @Override
    @Transactional
    public List<CategoryResponse> createCategories(List<CategoryCreationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_REQUIRED);
        }

        return requests.stream()
                .map(this::createOrRestoreCategory)
                .toList();
    }

    private CategoryResponse createOrRestoreCategory(CategoryCreationRequest request) {
        var existedCategory = categoryRepository.findByNameIgnoreCase(request.getName());

        if (existedCategory.isPresent()) {
            Category category = existedCategory.get();

            if (category.getStatus() == CategoryStatus.DELETED) {
                category.setStatus(CategoryStatus.ACTIVE);
                category.setDeletedAt(null);
                category.setDescription(request.getDescription());

                return categoryMapper.toCategoryResponse(categoryRepository.save(category));
            }

            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }

        Category category = categoryMapper.toCategory(request);
        category.setStatus(CategoryStatus.ACTIVE);
        category.setDeletedAt(null);

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE)
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getAllCategoriesForManagement() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long categoryId) {
        return categoryMapper.toCategoryResponse(getCategoryEntityById(categoryId));
    }

    @Override
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Category category = getCategoryEntityById(categoryId);

        if (request.getName() != null && !request.getName().isBlank()) {
            var existedCategory = categoryRepository.findByNameIgnoreCase(request.getName());

            if (existedCategory.isPresent()
                    && !existedCategory.get().getCategoryId().equals(categoryId)
                    && existedCategory.get().getStatus() != CategoryStatus.DELETED) {
                throw new AppException(ErrorCode.CATEGORY_EXISTED);
            }

            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            if (request.getStatus() == CategoryStatus.DELETED
                    && productRepository.existsByCategory_CategoryIdAndStatusIn(
                    categoryId,
                    List.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE))) {
                throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
            }

            category.setStatus(request.getStatus());

            if (request.getStatus() == CategoryStatus.DELETED) {
                category.setDeletedAt(LocalDateTime.now());
            } else {
                category.setDeletedAt(null);
            }
        }

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    public Category getActiveCategoryEntityById(Long categoryId) {
        Category category = getCategoryEntityById(categoryId);

        if (category.getStatus() == CategoryStatus.DELETED) {
            throw new AppException(ErrorCode.CATEGORY_DELETED);
        }

        return category;
    }

    @Override
    public Category getActiveCategoryEntityByName(String categoryName) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getStatus() == CategoryStatus.DELETED) {
            throw new AppException(ErrorCode.CATEGORY_DELETED);
        }

        return category;
    }

    private Category getCategoryEntityById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}