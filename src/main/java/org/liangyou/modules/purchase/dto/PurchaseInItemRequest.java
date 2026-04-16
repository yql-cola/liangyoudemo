package org.liangyou.modules.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PurchaseInItemRequest {

    @NotNull(message = "product-id-required")
    private Long productId;
    @NotNull(message = "unit-id-required")
    private Long unitId;
    @NotNull(message = "qty-required")
    private BigDecimal qty;
    @NotNull(message = "unit-price-required")
    private BigDecimal unitPrice;
    @NotNull(message = "amount-required")
    private BigDecimal amount;
    @NotBlank(message = "batch-no-required")
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    @NotNull(message = "inbound-time-required")
    private LocalDateTime inboundTime;
    private String remark;
}
