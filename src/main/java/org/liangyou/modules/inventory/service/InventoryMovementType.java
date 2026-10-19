package org.liangyou.modules.inventory.service;

import org.liangyou.common.exception.BusinessException;

/** Stable ledger codes. API responses keep codes; views and spreadsheets use labels. */
public enum InventoryMovementType {
    PURCHASE_IN("purchase_in", "purchase_in", "采购入库"),
    PURCHASE_IN_REVERSE("purchase_in_reverse", "purchase_in_reverse", "采购入库撤销"),
    SALE_OUT("sale_out", "sale_out", "销售出库"),
    SALE_OUT_REVERSE("sale_out_reverse", "sale_out_reverse", "销售出库撤销"),
    INVENTORY_ADJUST("inventory_adjust", "adjust", "库存调整");

    private final String businessCode;
    private final String flowCode;
    private final String label;

    InventoryMovementType(String businessCode, String flowCode, String label) {
        this.businessCode = businessCode;
        this.flowCode = flowCode;
        this.label = label;
    }

    public String businessCode() { return businessCode; }
    public String flowCode() { return flowCode; }
    public String label() { return label; }

    public static InventoryMovementType fromBusinessCode(String code) {
        for (InventoryMovementType type : values()) if (type.businessCode.equals(code)) return type;
        throw new BusinessException(400, "inventory-movement-type-invalid");
    }

    public static String labelForCode(String code) {
        for (InventoryMovementType type : values())
            if (type.businessCode.equals(code) || type.flowCode.equals(code)) return type.label;
        return code == null ? "" : code;
    }
}
