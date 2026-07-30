package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时滚动班次库存只读Mapper。
 */
@Mapper
public interface Cd15EngineShiftStockMapper extends BaseMapper<Cd15ShiftStock> {
}
