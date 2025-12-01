package com.zlt.aps.monthplan.mdm.handler;

import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.maindata.domain.vo.SizeCapacityParamVo;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.BaseMoldingMachineCapacityVo;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SizeCapacityAllocationService.java
 * 描    述：寸口产能分配
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20250602
 */
public interface SizeCapacityAllocationService<T extends BaseMoldingMachineCapacityVo> {
    /**
     * 根据排产需求，进行成型寸口产能分配
     *
     * @param sizeCapacityRequireList    寸口产能需求集合
     * @param moldingMachineCapacityList 成型寸口产能集合
     * @param param                      分配参数
     * @return
     */
    List<SizeCapacityConfiguration> allocationMoldingMachineCapacity(List<MonthPlanManufacturingRequirementVo> sizeCapacityRequireList, List<T> moldingMachineCapacityList, SizeCapacityParamVo param);
}
