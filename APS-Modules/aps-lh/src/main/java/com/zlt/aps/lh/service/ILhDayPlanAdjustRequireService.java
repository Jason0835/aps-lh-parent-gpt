package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.aps.lh.api.domain.vo.LhDayPlanAdjustRequireSummaryVo;
import com.zlt.bill.common.service.IDocService;

import java.time.YearMonth;
import java.util.List;

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
     * 汇总当前查询条件下全部月计划行的调整量。
     *
     * @param queryVO 查询条件
     * @return 调整1、调整2、调整3和调整后总合计
     */
    LhDayPlanAdjustRequireSummaryVo summary(LhDayPlanAdjustRequire queryVO);

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

    /**
     * 获取指定年月，工厂、Sku的硫化日计划调整信息
     *
     * @param yearMonth        年-月
     * @param factoryList      工厂信息
     * @param materialCodeList Sku编码
     * @return
     */
    List<LhDayPlanAdjustVo> getMonthPlanLhDayAdjustList(YearMonth yearMonth, List<String> factoryList, List<String> materialCodeList);
}
