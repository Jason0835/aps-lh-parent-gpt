package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanPreProductionCapacity;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanPreProductionCapacityMapper.java
 * 描    述：分厂月生产计划产能预占Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-09
 */
@Mapper
public interface MonthPlanPreProductionCapacityMapper extends CommBaseMapper<MonthPlanPreProductionCapacity> {
    /**
     * 删除旧有数据
     *
     * @param deleteParam
     * @return
     */
    int deleteOldData(MonthPlanPreProductionCapacity deleteParam);
}
