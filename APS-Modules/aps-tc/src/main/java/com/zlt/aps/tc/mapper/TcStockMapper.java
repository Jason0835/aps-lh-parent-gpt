package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TcStockMapper extends CommBaseMapper<TcStock> {
}
