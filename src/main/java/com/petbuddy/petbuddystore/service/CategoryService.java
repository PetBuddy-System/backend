package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.CategoryCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CategoryUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CategoryResponse;
import com.petbuddy.petbuddystore.model.Category;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> createCategories(List<CategoryCreationRequest> requests);

    List<CategoryResponse> getActiveCategories();

    List<CategoryResponse> getAllCategoriesForManagement();

    CategoryResponse getCategoryById(Long categoryId);

    CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request);

    Category getActiveCategoryEntityById(Long categoryId);

    Category getActiveCategoryEntityByName(String categoryName);
}