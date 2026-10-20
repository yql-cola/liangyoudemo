package org.liangyou.modules.inventory.service;

import org.liangyou.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Enforces the persisted DECIMAL precision before any inventory mutation. */
public final class BusinessNumbers {
    private BusinessNumbers() { }

    public static BigDecimal quantity(BigDecimal value, boolean positive) {
        if (value == null || value.signum() < 0 || (positive && value.signum() == 0)) invalid();
        try {
            BigDecimal result = value.setScale(4, RoundingMode.UNNECESSARY);
            if (result.precision() > 18) invalid();
            return result;
        } catch (ArithmeticException ex) { throw new BusinessException(400, "invalid-business-number"); }
    }

    public static BigDecimal baseQuantity(BigDecimal qty, BigDecimal rate) {
        if (rate == null || rate.signum() <= 0) invalid();
        return quantity(quantity(qty, true).multiply(rate), true);
    }

    public static BigDecimal amount(BigDecimal qty, BigDecimal price) {
        BigDecimal result = quantity(qty, true).multiply(quantity(price, false)).setScale(2, RoundingMode.HALF_UP);
        if (result.precision() > 18) invalid();
        return result;
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) invalid();
        BigDecimal result = value.setScale(2, RoundingMode.HALF_UP);
        if (result.precision() > 18) invalid();
        return result;
    }

    private static void invalid() { throw new BusinessException(400, "invalid-business-number"); }
}
