package com.zlt.aps.mp.factory.mapper;

import com.ruoyi.common.datasource.service.IBaseMapper;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanStatisticsMapper.java
 * 描    述：S2-0612.最终排产计划统计Mapper接口--仅用于支持批量操作
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-02-05
 */
@Mapper
public interface BatchMpMonthPlanStatisticsEntityMapper extends IBaseMapper<MpMonthPlanStatistics> {

    /**
     * 插入单条记录
     *
     * @param record 排产计划统计实体
     * @return 插入数量
     */
    @Override
    int insert(MpMonthPlanStatistics record);
}
