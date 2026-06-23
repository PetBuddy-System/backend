package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.BlogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BlogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.BlogResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface BlogService {
    BlogResponse createBlog(BlogCreationRequest request, List<MultipartFile> images);
    Page<BlogResponse> getBlogs(String keyword, String label, int pageNumber, int pageSize);
    BlogResponse getBlogById(String blogId);
    BlogResponse updateBlog(String blogId, BlogUpdateRequest request, List<MultipartFile> images);
}
