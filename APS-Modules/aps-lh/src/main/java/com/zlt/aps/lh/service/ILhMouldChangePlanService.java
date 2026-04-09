package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 模具交替计划Service接口
 *
 * @author APS Team
 * @since 2026/04/01 11:
 */
public interface ILhMouldChangePlanService extends IDocService<LhMouldChangePlan> {

    String[] getQueryFormulas();

    /**
     * 排程发布
     * @param ids 记录ID列表
     * @return 发布结果
     */
    AjaxResult issueSchedule(List<Long> ids);
}
