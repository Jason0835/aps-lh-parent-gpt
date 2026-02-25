package com.zlt.aps.maindata.service;


import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthPlanMonitorService.java
 * 描    述：IMpMonthPlanMonitorService月度硫化监控后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-24
 */
public interface IMpMonthPlanMonitorService extends IDocService<MpMonthPlanMonitor> {
    /**
     * 统计完成量
     *
     * @param finalVersion 参数
     * @return 完成量
     */
    List<MpMonthPlanMonitor> findCompleteQty(MpFactoryProductionVersion finalVersion);

    /**
     * 批量插入月度硫化监控表
     *
     * @param param     参数
     * @param finalList 定稿数据
     */
    void insertMonitorByFinalList(FactoryMonthPlanProductionFinalResult param, List<FactoryMonthPlanProductionFinalResult> finalList);
}
