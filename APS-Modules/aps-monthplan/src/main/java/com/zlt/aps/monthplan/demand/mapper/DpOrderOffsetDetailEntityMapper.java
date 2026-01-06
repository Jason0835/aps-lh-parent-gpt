package com.zlt.aps.monthplan.demand.mapper;


import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpOrderOffsetDetailMapper.java
 * 描    述：S1-0604订单冲减分配Mapper接口
 *@author yelq
 *@ time 2025-12-30
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：nick
 *     修改内容：...
 */
@Mapper
public interface DpOrderOffsetDetailEntityMapper extends CommBaseMapper<DpOrderOffsetDetail> {

    /**
     * 批量更新生产量
     * @param list
     * @return int
     */
    int updateBatchProductionQty(@Param("list") List<DpOrderOffsetDetail> list);
}
