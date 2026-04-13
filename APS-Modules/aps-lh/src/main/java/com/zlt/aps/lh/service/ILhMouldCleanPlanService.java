package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.bill.common.service.IDocService;

/**
 * 模具清洗计划Service接口
 *
 * @author APS Team
 * @since 2026/04/10
 */
public interface ILhMouldCleanPlanService extends IDocService<LhMouldCleanPlan> {

    /**
     * 从模具清洗预警同步生成计划
     *
     * @return 同步数量
     */
    int syncFromMouldCleanWarn();
}
