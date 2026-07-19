package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
import com.petbuddy.petbuddystore.common.enums.UserStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.UserCreationRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateStatusRequest;
import com.petbuddy.petbuddystore.dto.response.UserResponse;
import com.petbuddy.petbuddystore.mapper.UserMapper;
import com.petbuddy.petbuddystore.model.Blog;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    FileService fileService;

    @Override
    public UserResponse createUser(UserCreationRequest request, List<MultipartFile> images) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
        Role role = request.getRole();
        if (role != Role.CUSTOMER && role != Role.MANAGER && role != Role.STAFF) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }

        if (role == Role.STAFF) {
            if (request.getStaffTask() == null) {
                throw new AppException(ErrorCode.STAFF_TASK_REQUIRED);
            }
        } else {
            request.setStaffTask(null);
        }

        User user =  userMapper.toUser(request);
        user.setStatus(UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(role);
        user.setStaffTask(request.getStaffTask());

        List<MediaFile> mediaFiles = new ArrayList<>();

        if (images != null && !images.isEmpty()){
            mediaFiles = images.stream()
                    .filter(image -> image != null && !image.isEmpty())
                    .map(file -> {
                        MediaFile mediaFile = fileService.uploadUserProfileImage(file);
                        mediaFile.setUser(user);
                        return mediaFile;
                    })
                    .collect(Collectors.toList());
        }
        user.setMediaFiles(mediaFiles);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public Page<UserResponse> getUsers(Role role, StaffTask staffTask, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage = userRepository.findUsers(role, staffTask, pageable);
        return userPage.map(userMapper::toUserResponse);
    }

    @Override
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId).
                orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(String userId, UserUpdateRequest request, List<MultipartFile> images) {
        User user = userRepository.findById(userId).
                orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        updateRoleAndStaffTask(user, request);

        if (images != null && !images.isEmpty()){
            List<MediaFile> mediaFiles = images.stream()
                    .filter(image -> image != null && !image.isEmpty())
                    .map(file -> {
                        MediaFile mediaFile = fileService.uploadUserProfileImage(file);
                        mediaFile.setUser(user);
                        return mediaFile;
                    })
                    .toList();

            if (!mediaFiles.isEmpty()) {
                user.getMediaFiles().clear();
                user.getMediaFiles().addAll(mediaFiles);
            }
        }
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateUserStatus(String userId, UserUpdateStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(request.getStatus());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public User getUserEntityById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    public UserResponse getCurrentUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    private Role resolveUpdatedRole(Role currentRole, Role requestedRole) {
        return requestedRole != null ? requestedRole : currentRole;
    }

    private void updateRoleAndStaffTask(User user, UserUpdateRequest request) {
        Role currentRole = user.getRole();
        Role updatedRole = resolveUpdatedRole(currentRole, request.getRole());

        if (updatedRole == Role.STAFF) {
            boolean changingToStaff = currentRole != Role.STAFF;

            if (changingToStaff && request.getStaffTask() == null) {
                throw new AppException(ErrorCode.STAFF_TASK_REQUIRED);
            }

            if (request.getStaffTask() == null && user.getStaffTask() == null) {
                throw new AppException(ErrorCode.STAFF_TASK_REQUIRED);
            }

            if (request.getStaffTask() != null) {
                user.setStaffTask(request.getStaffTask());
            }
        } else {
            user.setStaffTask(null);
        }
        user.setRole(updatedRole);
    }

    @Override
    public List<UserResponse> getAllStaffs() {
        return userRepository.findAllByRole(Role.STAFF)
                .stream()
                .map(userMapper::toUserResponse).toList();
    }
}
