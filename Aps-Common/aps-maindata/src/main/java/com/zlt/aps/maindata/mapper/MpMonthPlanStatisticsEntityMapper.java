package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanStatisticsMapper.java
 * 描    述：S2-0612.最终排产计划统计Mapper接口
 *@author zlt
 *@date 2026-02-05
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpMonthPlanStatisticsEntityMapper extends CommBaseMapper<MpMonthPlanStatistics> {

    /**
     * 删除月计划统计结果
     * @param factoryCode
     * @param year
     * @param month
     * @param productionVersion
     */
    void deleteMonthPlanStatisticsByCondition(@Param("factoryCode") String factoryCode,
                                     @Param("year") String year,
                                     @Param("month") String month,
                                     @Param("productionVersion") String productionVersion,
                                              @Param("structureList") List<String> structureList);

}
