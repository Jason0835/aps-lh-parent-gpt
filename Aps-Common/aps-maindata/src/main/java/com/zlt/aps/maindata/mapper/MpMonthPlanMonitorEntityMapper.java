package com.zlt.aps.maindata.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.core.dao.basemapper.CommBaseMapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanMonitorMapper.java
 * 描    述：月度硫化监控Mapper接口
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpMonthPlanMonitorEntityMapper extends CommBaseMapper<MpMonthPlanMonitor> {
    /**
     * 月度硫化监控报表
     * @param dto
     * @return
     */
    List<MpMonthPlanMonitor> listReport(MpMonthPlanMonitor queryVo);
}
