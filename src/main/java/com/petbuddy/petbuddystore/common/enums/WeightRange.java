package com.petbuddy.petbuddystore.common.enums;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;

public enum WeightRange {
    EXTRA_SMALL,    // Dưới 5kg
    SMALL,          // 5kg - 8kg
    MEDIUM,         // 8kg - 12kg
    LARGE,          // 12kg - 18kg
    EXTRA_LARGE,    // 18kg - 25kg
    EXTRA_EXTRA_LARGE // Trên 25kg
    ;

    public static WeightRange fromWeight(double weight) {
        if (weight <= 0 || weight > 100) {
            throw new AppException(ErrorCode.INVALID_PET_WEIGHT);
        }
        if (weight < 5.0)  return EXTRA_SMALL;
        if (weight < 8.0)  return SMALL;
        if (weight < 12.0) return MEDIUM;
        if (weight < 18.0) return LARGE;
        if (weight < 25.0) return EXTRA_LARGE;
        return EXTRA_EXTRA_LARGE;
    }
}
