package com.zlt.aps.monthplan.factory.service;


import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanPreProductionCapacityService.java
 * 描    述：IMonthPlanPreProductionCapacityService分厂月生产计划产能预占
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20250709
 */
public interface IMonthPlanPreProductionCapacityService {
    /**
     * 保存预占产能(模具产能预分配等)
     *
     * @param preAllocationCapacityList
     */
    void savePreProductionCapacity(List<MonthPlanManufacturingRequirementVo> preAllocationCapacityList);
}
