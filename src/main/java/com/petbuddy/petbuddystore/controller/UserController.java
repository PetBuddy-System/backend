package com.petbuddy.petbuddystore.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.PetProfileCreationRequest;
import com.petbuddy.petbuddystore.dto.request.UserCreationRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.UserUpdateStatusRequest;
import com.petbuddy.petbuddystore.dto.response.UserResponse;
import com.petbuddy.petbuddystore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully",
                        userService.createUser(request, Role.CUSTOMER, images)));
    }

    @PostMapping("/manager")
    @Operation(description = "Tạo mới Manager")
    public ResponseEntity<ApiResponse<UserResponse>> createManager(@RequestPart("data") String requestJson,
                                                                   @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        UserCreationRequest request = objectMapper.readValue(requestJson, UserCreationRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Manager created successfully",
                        userService.createUser(request, Role.MANAGER, images)));
    }

    @PostMapping("/staff")
    @Operation(description = "Tạo mới Staff")
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(@RequestPart("data") String requestJson,
                                                                 @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        UserCreationRequest request = objectMapper.readValue(requestJson, UserCreationRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff created successfully",
                        userService.createUser(request, Role.STAFF, images)));
    }

    @GetMapping("/customer")
    @Operation(description = "Lấy danh sách toàn bộ customer")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllCustomer(){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getAllCustomers()));
    }

    @GetMapping("/manager")
    @Operation(description = "Lấy danh sách toàn bộ manager")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllManagers(){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getAllManagers()));
    }

    @GetMapping("/staff")
    @Operation(description = "Lấy danh sách toàn bộ staff")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllStaffs(){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getAllStaffs()));
    }

    @GetMapping("/{userId}")
    @Operation(description = "Lấy thông tin user theo id")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(userService.getUserById(userId)));
    }

    @PutMapping("/{userId}")
    @Operation(description = "Update thông tin user theo id")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable String userId, @RequestPart("data") String requestJson,
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


}
