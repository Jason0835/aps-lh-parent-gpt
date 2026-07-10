package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁库存管理 Mapper。
 */
@Mapper
public interface Cd15StockMapper extends CommBaseMapper<Cd15Stock> {
}
