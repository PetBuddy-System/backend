package com.petbuddy.petbuddystore.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.PetProfileCreationRequest;
import com.petbuddy.petbuddystore.dto.request.UserCreationRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateStatusRequest;
import com.petbuddy.petbuddystore.dto.response.BlogResponse;
import com.petbuddy.petbuddystore.dto.response.UserResponse;
import com.petbuddy.petbuddystore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User API", description = "Quản lí user (crud user)")
public class UserController {
    UserService userService;
    ObjectMapper objectMapper;

    @PostMapping("/customer")
    @Operation(description = "Tạo mới Customer")
    public ResponseEntity<ApiResponse<UserResponse>> createCustomer(@RequestPart("data") String requestJson,
                                                                    @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        UserCreationRequest request = objectMapper.readValue(requestJson, UserCreationRequest.class);
        request.setRole(Role.CUSTOMER);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", userService.createUser(request, images)));
    }

    @PostMapping("/employee")
    @Operation(description = "Tạo mới Manager hoặc Staff")
    public ResponseEntity<ApiResponse<UserResponse>> createEmployee(@RequestPart("data") String requestJson,
                                                                   @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        UserCreationRequest request = objectMapper.readValue(requestJson, UserCreationRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", userService.createUser(request, images)));
    }

    @GetMapping()
    @Operation(description = "Phân trang danh sách người dùng dành cho admin")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(@RequestParam(required = false) Role role,
                                                                    @RequestParam(required = false) StaffTask staffTask,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getUsers(role, staffTask, page, size)));
    }

    @GetMapping("/{userId}")
    @Operation(description = "Lấy thông tin user theo id")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getUserById(userId)));
    }

    @PutMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(description = "Update thông tin user theo id")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable String userId, @RequestPart(value = "data", required = false) String requestJson,
                                                                @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException{
        UserUpdateRequest request = objectMapper.readValue(requestJson, UserUpdateRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User updated successfully",
                        userService.updateUser(userId, request, images)));
    }

    @PutMapping("/status/{userId}")
    @Operation(description = "Update status user theo id")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(@PathVariable String userId, @RequestBody UserUpdateStatusRequest request){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("User updated successfully",userService.updateUserStatus(userId, request)));
    }

    @GetMapping("/me")
    @Operation(description = "Lấy thông tin user hiện tại dựa theo JWT token")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getCurrentUser()));
    }

    @GetMapping("/staff")
    @Operation(description = "Lấy danh sách toàn bộ staff")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllStaffs(){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getAllStaffs()));
    }

}
