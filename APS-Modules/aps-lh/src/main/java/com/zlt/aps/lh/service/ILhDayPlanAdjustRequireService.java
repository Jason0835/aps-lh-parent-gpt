package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.bill.common.service.IDocService;

/**
 * 硫化日计划调整需求服务。
 */
public interface ILhDayPlanAdjustRequireService extends IDocService<LhDayPlanAdjustRequire> {

    /**
     * 查询月计划驱动的调整需求列表。
     *
     * @param queryVO 查询条件
     * @return 分页列表
     */
    TableDataInfo listPage(LhDayPlanAdjustRequire queryVO);

    /**
     * 保存当前行三次调整。
     *
     * @param entity 当前行数据
     */
    void saveRow(LhDayPlanAdjustRequire entity);

    /**
     * 获取查询公式。
     *
     * @return 查询公式
     */
    String[] getQueryFormulas();
}
