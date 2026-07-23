package com.petbuddy.petbuddystore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.BlogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BlogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.BlogResponse;
import com.petbuddy.petbuddystore.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blogs")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Blog API", description = "Quản lí blog")
public class BlogController {
    BlogService blogService;
    ObjectMapper objectMapper;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(description = "Tạo mới Blog")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(@RequestPart("data") String requestJson,
                                                                @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        BlogCreationRequest request = objectMapper.readValue(requestJson, BlogCreationRequest.class);
        BlogResponse response = blogService.createBlog(request, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blog created successfully", response));
    }

    @GetMapping
    @Operation(description = "Phân trang danh sách blog")
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> getBlogs(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String label,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(blogService.getBlogs(keyword, label, page, size)));
    }

    @GetMapping("/{blogId}")
    @Operation(description = "Tìm blog theo id")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogById(@PathVariable String blogId){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(blogService.getBlogById(blogId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @PutMapping(value = "/{blogId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BlogResponse>> updateBlog(
            @PathVariable String blogId,
            @RequestPart("data") String requestJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {

        BlogUpdateRequest request = objectMapper.readValue(requestJson, BlogUpdateRequest.class);
        BlogResponse response = blogService.updateBlog(blogId, request, images);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Blog updated successfully", response));
    }
}
