package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.bill.common.service.IDocService;

/**
 * 模具交替计划Service接口
 *
 * @author APS Team
 * @since 2026/04/01
 */
public interface ILhMouldChangePlanService extends IDocService<LhMouldChangePlan> {

    String[] getQueryFormulas();
}
