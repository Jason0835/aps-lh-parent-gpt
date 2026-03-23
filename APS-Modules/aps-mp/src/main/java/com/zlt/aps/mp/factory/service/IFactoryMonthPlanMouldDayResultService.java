package com.zlt.aps.mp.factory.service;


import java.util.List;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.factory.dto.FactoryMonthPlanMouldDayResultExportVo;
import com.zlt.bill.common.service.IDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryMonthPlanMouldDayResultService.java
 * 描    述：IFactoryMonthPlanMouldDayResultServiceS2-0604.排产结果-生产计划排产结果后端接口
 *@author zlt
 *@date 2025-12-31
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IFactoryMonthPlanMouldDayResultService extends IDocService<FactoryMonthPlanMouldDayResult>{
    /**
     * 查询导出列表
     * @param factoryMonthPlanMouldDayResult
     * @param isAllMaterial
     * @return
     */
    List<FactoryMonthPlanMouldDayResultExportVo> getExportList(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult, boolean isAllMaterial);

    /**
     * 导出数据
     * @param list
     * @return
     */
    byte[] getFactoryMonthPlanMouldDayResultExportByte(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult, List<FactoryMonthPlanMouldDayResultExportVo> list);
}
