package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.BlogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BlogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.BlogResponse;
import com.petbuddy.petbuddystore.mapper.BlogMapper;
import com.petbuddy.petbuddystore.model.Blog;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.BlogRepository;
import com.petbuddy.petbuddystore.service.BlogService;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogServiceImpl implements BlogService {
    BlogRepository blogRepository;
    BlogMapper blogMapper;
    UserService userService;
    FileService fileService;

    @Override
    public BlogResponse createBlog(BlogCreationRequest request, List<MultipartFile> images) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserEntityById(userId);

        if (blogRepository.existsByTitle(request.getTitle())) {
            throw new AppException(ErrorCode.BLOG_EXISTED);
        }

        Blog blog = blogMapper.toBlog(request);
        blog.setUser(user);

        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = images.stream()
                    .filter(image -> image != null && !image.isEmpty())
                    .map(fileService::uploadBlogImage)
                    .toList();

            blog.setImageUrls(imageUrls);
        }
        return blogMapper.toBlogResponse(blogRepository.save(blog));
    }

    @Override
    public Page<BlogResponse> getBlogs(String keyword, String label, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        Page<Blog> blogPage = blogRepository.findBlogs(keyword, label, pageable);
        return blogPage.map(blogMapper::toBlogResponse);
    }

    @Override
    public BlogResponse getBlogById(String blogId) {
        Blog blog = blogRepository.findById(blogId).
                orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_EXISTED));
        return blogMapper.toBlogResponse(blog);
    }

    @Override
    public BlogResponse updateBlog(String blogId, BlogUpdateRequest request, List<MultipartFile> images) {
        Blog blog = blogRepository.findById(blogId).
                orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_EXISTED));
        blogMapper.updateBlog(blog, request);

        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = images.stream()
                    .filter(image -> image != null && !image.isEmpty())
                    .map(fileService::uploadBlogImage)
                    .toList();

            if (!imageUrls.isEmpty()) {
                blog.setImageUrls(imageUrls);
            }
        }


        return blogMapper.toBlogResponse(blogRepository.save(blog));
    }
}
