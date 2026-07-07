package com.shanzai.recipe.modules.profile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BmiCalculatorTest {
    @Test
    void calculatesBmiWithTwoDecimals() {
        BigDecimal bmi = BmiCalculator.calculate(new BigDecimal("170"), new BigDecimal("65"));

        assertEquals(new BigDecimal("22.49"), bmi);
    }
}
