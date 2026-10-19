package org.liangyou.modules.basic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.liangyou.modules.basic.entity.BaseWarehouse;

@Mapper
public interface BaseWarehouseMapper extends BaseMapper<BaseWarehouse> {
    @Select("SELECT (SELECT COUNT(*) FROM stat_daily_summary WHERE warehouse_id = #{id}) + " +
            "(SELECT COUNT(*) FROM stat_monthly_summary WHERE warehouse_id = #{id})")
    long countSummaryReferences(@Param("id") Long id);
}
