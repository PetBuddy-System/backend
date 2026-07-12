package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.dto.request.RestockReturnRequest;
import com.petbuddy.petbuddystore.dto.response.RestockReturnResponse;
import com.petbuddy.petbuddystore.service.ReturnStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/return-requests")
@RequiredArgsConstructor
public class ReturnRestockController {

    private final ReturnStockService returnStockService;

    @GetMapping("/{returnRequestId}/restock-info")
    public ResponseEntity<RestockReturnResponse> getRestockInfo(
            @PathVariable Long returnRequestId) {
        RestockReturnResponse response = returnStockService.getRestockInfo(returnRequestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{returnRequestId}/restock")
    public ResponseEntity<Void> processRestock(
            @PathVariable Long returnRequestId,
            @RequestBody RestockReturnRequest request) {
        returnStockService.processRestock(returnRequestId, request);
        return ResponseEntity.ok().build();
    }
}