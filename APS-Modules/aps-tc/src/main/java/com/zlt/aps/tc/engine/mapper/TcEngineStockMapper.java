package com.zlt.aps.tc.engine.mapper;

import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎侧库存Mapper接口（引擎专用）
 */
@Mapper
public interface TcEngineStockMapper extends CommBaseMapper<TcStock> {

}
