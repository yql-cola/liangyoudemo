package org.liangyou.modules.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.liangyou.modules.inventory.entity.InvStock;

@Mapper
public interface InvStockMapper extends BaseMapper<InvStock> {
}
