package com.zlt.aps.tm.engine.mapper;

import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎面库存Mapper接口（引擎专用）
 */
@Mapper
public interface TmEngineStockMapper extends CommBaseMapper<TmStock> {

}
