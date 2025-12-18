package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmAreaCapaAllocationService.java
 * 描    述：IMdmAreaCapaAllocationService区域产能分配后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
public interface IMdmAreaCapaAllocationService extends IDocService<MdmAreaCapaAllocation> {

    /**
     * 复制
     *
     * @param entity 参数
     * @return 结果
     */
    AjaxResult copy(MdmAreaCapaAllocation entity);

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    AjaxResult checkBeforeCopy(MdmAreaCapaAllocation entity);
    /**
     * 查询区域产能分配是否有配置
     * @param createCondition 需求计划参数
     * @return 区域产能分配
     */
    List<MdmAreaCapaAllocation> findAreaCapaAllocation(DpDemandPlan createCondition);
}
