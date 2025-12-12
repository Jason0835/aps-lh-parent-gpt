package com.zlt.aps.maindata.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.RawMaterialRequirePlan;
import com.zlt.bill.common.service.IDocService;

import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawMaterialRequirePlanService.java
 * 描    述：IRawMaterialRequirePlanService原材料需求计划后端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IRawMaterialRequirePlanService  extends IDocService<RawMaterialRequirePlan>{

    /**
     * 生成原材料需求计划
     * @param year 年份
     * @param month 月份
     * @return 生成结果
     */
    AjaxResult generateRawMaterialRequirePlan(Integer year, Integer month);

    /**
     * 检查是否正在生成
     * @param year 年份
     * @param month 月份
     * @return 检查结果
     */
    AjaxResult checkGeneratingStatus(Integer year, Integer month);

    /**
     * 获取默认年月（当前年月+1）
     * @return 年月信息
     */
    Map<String, Integer> getDefaultYearMonth();

}
