package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直裁自动排程读取XWYY实际库存的只读Mapper。
 */
@Mapper
public interface Cd90EngineXwyyStockMapper extends BaseMapper<XwyyStock> {
}
