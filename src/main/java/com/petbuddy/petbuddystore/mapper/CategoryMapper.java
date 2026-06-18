package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.CategoryCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CategoryUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CategoryResponse;
import com.petbuddy.petbuddystore.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toCategory(CategoryCreationRequest request);

    CategoryResponse toCategoryResponse(Category category);
}