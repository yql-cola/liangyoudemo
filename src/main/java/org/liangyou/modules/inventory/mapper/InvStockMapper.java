package org.liangyou.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.liangyou.modules.inventory.entity.InvStock;

@Mapper
public interface InvStockMapper extends BaseMapper<InvStock> {
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
