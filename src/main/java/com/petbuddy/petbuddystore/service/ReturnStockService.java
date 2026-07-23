package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.RestockReturnRequest;
import com.petbuddy.petbuddystore.dto.response.RestockReturnResponse;
import com.petbuddy.petbuddystore.model.ReturnItem;
import com.petbuddy.petbuddystore.model.ReturnRequest;

public interface ReturnStockService {

    void validateStockForExchange(ReturnItem returnItem);
    void reserveStockForExchange(ReturnRequest returnRequest);
    void confirmStockForExchange(ReturnRequest returnRequest);
    void releaseReservedStock(ReturnRequest returnRequest);
    void returnStockForExchange(ReturnRequest returnRequest);
    RestockReturnResponse getRestockInfo(Long returnRequestId);
    void processRestock(Long returnRequestId, RestockReturnRequest request);
}