package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程6点库存只读Mapper。
 */
@Mapper
public interface Cd15EngineStockMapper extends BaseMapper<Cd15Stock> {
}
