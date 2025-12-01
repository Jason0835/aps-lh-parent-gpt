package com.zlt.aps.monthplan.factory.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmStockUpPlan;
import com.zlt.aps.monthplan.api.domain.vo.MdmStockUpPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.api.domain.vo.StockUpPlanExcelVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmStockUpPlanService.java
 * 描    述：IMdmStockUpPlanService备货计划后端接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-18
 */
public interface IMdmStockUpPlanService extends IService<MdmStockUpPlan> {

    /**
     * 查询备货计划列表
     *
     * @param mdmStockUpPlan 备货计划
     * @return 备货计划集合
     */
    List<MdmStockUpPlanVo> selectMdmStockUpPlanList(MdmStockUpPlanVo mdmStockUpPlan);

    /**
     * 根据年份、月份获取年份和月份的备货计划信息
     *
     * @param year  年份
     * @param month 月份
     * @return
     */
    List<MdmStockUpPlan> getStockUpByYearAndMonth(Integer year, Integer month);

    /**
     * 生成备货计划
     *
     * @param queryCalcStockingParamVo
     * @return
     */
    AjaxResult createStockUpPlan(QueryCalcStockingParamVo queryCalcStockingParamVo);

    /**
     * 修改保存备货计划的备货量
     *
     * @param mdmStockUpPlan 备货计划
     * @return
     */
    AjaxResult saveStockUpPlan(MdmStockUpPlanVo mdmStockUpPlan);

    /**
     * 导入备货计划
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     */
    @Transactional
    AjaxResult importData(List<StockUpPlanExcelVo> list, boolean updateSupport, Long importLogId);
}
