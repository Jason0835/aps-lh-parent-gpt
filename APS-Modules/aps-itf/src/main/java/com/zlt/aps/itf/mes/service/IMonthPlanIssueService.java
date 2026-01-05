package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanIssueService.java
 * 描    述：IMonthPlanIssueService月计划下发后端接口
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
public interface IMonthPlanIssueService {

    /**
     * 下发月计划
     *
     * @param monthPlanIssueList 列表
     * @return 列表
     */
    AjaxResult issueMonthPlan(List<FactoryMonthPlanProductionFinalResult> monthPlanIssueList);
}
