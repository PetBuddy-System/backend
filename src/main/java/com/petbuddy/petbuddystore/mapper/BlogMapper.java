package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.BlogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BlogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.BlogResponse;
import com.petbuddy.petbuddystore.model.Blog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BlogMapper {
    Blog toBlog(BlogCreationRequest request);

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "mediaFiles", target = "mediaFiles")
    BlogResponse toBlogResponse(Blog blog);

    void updateBlog(@MappingTarget Blog blog, BlogUpdateRequest request);
}
