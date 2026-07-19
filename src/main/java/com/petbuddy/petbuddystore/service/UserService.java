package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
import com.petbuddy.petbuddystore.dto.request.UserCreationRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateStatusRequest;
import com.petbuddy.petbuddystore.dto.response.UserResponse;
import com.petbuddy.petbuddystore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreationRequest request, List<MultipartFile> images);
    UserResponse getUserById(String userId);
    User getUserEntityById(String userId);
    UserResponse updateUser(String userId, UserUpdateRequest request, List<MultipartFile> images);
    UserResponse updateUserStatus(String userId, UserUpdateStatusRequest request);
    UserResponse getCurrentUser();
    Page<UserResponse> getUsers(Role role, StaffTask staffTask, int page, int size);
    List<UserResponse> getAllStaffs();
}
