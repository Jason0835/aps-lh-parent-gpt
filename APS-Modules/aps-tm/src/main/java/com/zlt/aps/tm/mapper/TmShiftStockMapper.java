package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmShiftStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎面自动滚动班次库存Mapper。
 */
@Mapper
public interface TmShiftStockMapper extends CommBaseMapper<TmShiftStock> {
}
