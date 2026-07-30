package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时滚动班次库存只读Mapper。
 */
@Mapper
public interface Cd90EngineShiftStockMapper extends BaseMapper<Cd90ShiftStock> {
}
