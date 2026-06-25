package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程6点库存只读Mapper。
 */
@Mapper
public interface Cd90EngineStockMapper extends BaseMapper<Cd90Stock> {
}
