package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.dto.request.UserCreationRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateStatusRequest;
import com.petbuddy.petbuddystore.dto.response.UserResponse;
import com.petbuddy.petbuddystore.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreationRequest request, Role role, List<MultipartFile> images);
    List<UserResponse> getAllCustomers();
    List<UserResponse> getAllManagers();
    List<UserResponse> getAllStaffs();
    UserResponse getUserById(String userId);
    User getUserEntityById(String userId);
    UserResponse updateUser(String userId, UserUpdateRequest request, List<MultipartFile> images);
    UserResponse updateUserStatus(String userId, UserUpdateStatusRequest request);
}
