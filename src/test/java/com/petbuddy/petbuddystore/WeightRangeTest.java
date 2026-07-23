package com.petbuddy.petbuddystore;

import com.petbuddy.petbuddystore.common.enums.WeightRange;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeightRangeTest {
    @Test
    void fromWeightRejectsInvalidWeights() {
        assertInvalidWeight(-1);
        assertInvalidWeight(0);
        assertInvalidWeight(100.1);
    }

    @Test
    void fromWeightAcceptsMaximumWeight() {
        assertEquals(WeightRange.EXTRA_EXTRA_LARGE, WeightRange.fromWeight(100));
    }

    private void assertInvalidWeight(double weight) {
        AppException exception = assertThrows(AppException.class, () -> WeightRange.fromWeight(weight));
        assertEquals(ErrorCode.INVALID_PET_WEIGHT, exception.getErrorCode());
    }
}
