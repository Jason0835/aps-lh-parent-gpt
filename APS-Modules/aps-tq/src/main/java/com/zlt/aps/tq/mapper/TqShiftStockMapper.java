package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqShiftStock;

/**
 * 胎圈自动滚动班次库存 Mapper。
 *
 * <p>对齐胎面 TmShiftStockMapper，承载自动滚动调量算法所需的班次库存快照查询。</p>
 *
 * @author APS
 */
public interface TqShiftStockMapper extends BaseMapper<TqShiftStock> {
}
