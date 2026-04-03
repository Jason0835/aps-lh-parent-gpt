package com.zlt.aps.mps.mapper;


import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 成型库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
@Mapper
public interface CxStockMapper extends CommBaseMapper<CxStock> {


}
