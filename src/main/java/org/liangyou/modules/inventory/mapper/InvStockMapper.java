package org.liangyou.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.liangyou.modules.inventory.entity.InvStock;
import java.math.BigDecimal;

@Mapper
public interface InvStockMapper extends BaseMapper<InvStock> {
    @Select("""
            SELECT COALESCE(SUM(r.base_qty), 0) FROM sale_order_reservation r
            JOIN biz_sale_out s ON s.id = r.sale_out_id
            WHERE r.warehouse_id = #{warehouseId} AND r.product_id = #{productId}
              AND r.status = 'active' AND s.order_status IN ('in_delivery', 'awaiting_return')
            """)
    BigDecimal inDeliveryQty(@Param("warehouseId") Long warehouseId, @Param("productId") Long productId);

    /** Only a never-used empty summary may be removed; historical rows keep their unique key. */
    @Delete("""
            DELETE FROM inv_stock WHERE id = #{id} AND deleted = 0
              AND total_qty = 0 AND available_qty = 0 AND locked_qty = 0
              AND NOT EXISTS (SELECT 1 FROM inv_batch b WHERE b.warehouse_id = inv_stock.warehouse_id AND b.product_id = inv_stock.product_id)
              AND NOT EXISTS (SELECT 1 FROM inv_stock_flow f WHERE f.warehouse_id = inv_stock.warehouse_id AND f.product_id = inv_stock.product_id)
              AND NOT EXISTS (SELECT 1 FROM biz_purchase_in_item p WHERE p.warehouse_id = inv_stock.warehouse_id AND p.product_id = inv_stock.product_id)
              AND NOT EXISTS (SELECT 1 FROM biz_sale_out_item s WHERE s.warehouse_id = inv_stock.warehouse_id AND s.product_id = inv_stock.product_id)
            """)
    int deleteUnusedZero(@Param("id") Long id);

}
