package com.zlt.aps.mp.engine.mapper;

import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanStatisticsMapper.java
 * 描    述：S2-0612.最终排产计划统计Mapper接口
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
public interface FactoryMpMonthPlanStatisticsEntityMapper extends CommBaseMapper<MpMonthPlanStatistics> {

}
