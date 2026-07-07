package com.shanzai.recipe.modules.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BmiCalculator {
    private BmiCalculator() {
    }

    public static BigDecimal calculate(BigDecimal heightCm, BigDecimal weightKg) {
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }
}
