package org.liangyou.modules.basic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.liangyou.modules.basic.entity.BaseCustomer;

@Mapper
public interface BaseCustomerMapper extends BaseMapper<BaseCustomer> {
}
