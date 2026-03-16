package com.zlt.aps.mp.factory.mapper;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.factory.dto.FactoryMonthPlanMouldDayResultExportVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanMouldDayResultMapper.java
 * 描    述：S2-0604.排产结果-生产计划排产结果Mapper接口
 *@author zlt
 *@date 2025-12-31
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface FactoryMonthPlanMouldDayResultEntityMapper extends CommBaseMapper<FactoryMonthPlanMouldDayResult> {
    /**
     * 查询导出列表
     * @param factoryMonthPlanMouldDayResult
     * @return
     */
    List<FactoryMonthPlanMouldDayResultExportVo> getExportList(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult);
}
