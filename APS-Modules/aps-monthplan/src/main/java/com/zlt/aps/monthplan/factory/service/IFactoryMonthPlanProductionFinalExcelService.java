package com.zlt.aps.monthplan.factory.service;
/**
 * Copyright (c) 2022, All rights reserved。
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-1121
 */

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.factory.helper.ProductionPlanExcelImportHelper;

import java.util.List;

/**
 * 排产管理导入 数据保存业务接口，因要捕获异常，由需要事务，故而抽取数据存储动作到另外业务接口
 *
 * @author ZLT
 * @date 20251121
 *
 */
public interface IFactoryMonthPlanProductionFinalExcelService {

    /**
     * 保存 导入试制量试计划
     *
     * @param importList
     */
    void saveImportTrialProductionPlan(List<MonthPlanProductionFinalResult> importList);

    /**
     * 保存调整计划
     * @param excelHelper 导入助手
     * @param insertList 导入列表
     * @param successNum 成功条数
     * @return 结果
     */
    Integer saveImportAdjustPlan(ProductionPlanExcelImportHelper excelHelper,
                                 List<MonthPlanProductionFinalResult> insertList,
                                 Integer successNum);
}
