package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.CageSize;
import com.petbuddy.petbuddystore.common.enums.CageStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.CageCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CageUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CageResponse;
import com.petbuddy.petbuddystore.service.CageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cages")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Cage API", description = "Quản lý lồng thú cưng")
public class CageController {
    CageService cageService;

    @PostMapping()
    @Operation(description = "Tạo mới lồng")
    public ResponseEntity<ApiResponse<List<CageResponse>>> createCage(@RequestBody @Valid CageCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cage created successfully", cageService.createCage(request)));
    }

    @GetMapping()
    @Operation(description = "Lấy danh sách toàn bộ lồng theo size hoặc status")
    public ResponseEntity<ApiResponse<Page<CageResponse>>> getCages(@RequestParam(required = false) CageSize cageSize,
                                                                    @RequestParam(required = false) CageStatus cageStatus,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(cageService.getCages(cageSize, cageStatus, page, size)));
    }

    @GetMapping("/{cageId}")
    @Operation(description = "Lấy thông tin lồng theo id")
    public ResponseEntity<ApiResponse<CageResponse>> getCage(@PathVariable Long cageId){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(cageService.getCageById(cageId)));
    }

    @PutMapping("/{cageId}")
    @Operation(description = "Update thông tin lồng theo id")
    public ResponseEntity<ApiResponse<CageResponse>> updateCage(@PathVariable Long cageId, @RequestBody @Valid CageUpdateRequest request){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cage updated successfully",cageService.updateCage(request,cageId)));
    }

}