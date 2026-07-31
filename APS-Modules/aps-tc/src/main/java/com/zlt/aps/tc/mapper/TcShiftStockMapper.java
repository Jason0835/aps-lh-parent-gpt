package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcShiftStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎侧自动滚动班次库存Mapper。
 */
@Mapper
public interface TcShiftStockMapper extends CommBaseMapper<TcShiftStock> {
}
