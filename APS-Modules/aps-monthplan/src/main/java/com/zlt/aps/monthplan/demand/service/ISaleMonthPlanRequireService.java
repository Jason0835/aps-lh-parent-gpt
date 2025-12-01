package com.zlt.aps.monthplan.demand.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.api.domain.vo.SaleMonthPlanRequireReportVo;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISaleMonthPlanRequireService.java
 * 描    述：ISaleMonthPlanRequireService月度生产需求计划后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
public interface ISaleMonthPlanRequireService {
    /**
     * 根据查询条件，获取列表数据
     *
     * @param queryWrapper
     * @return
     */
    List<SaleMonthPlanRequire> getList(Wrapper<SaleMonthPlanRequire> queryWrapper);

    /**
     * 根据主键ID，批量删除数据
     *
     * @param ids
     * @return
     */
    int removeByIds(List<Long> ids);

    /**
     * 导入数据处理
     *
     * @param excelDataList excel解析后的数据
     * @param updateSupport 是否需要更新处理 true 更新 false不更新
     * @param importLogId 导入日志ID
     * @return
     */
    AjaxResult importData(List<SaleMonthPlanRequire> excelDataList, boolean updateSupport, Long importLogId);

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    List<String> versionList(SaleMonthPlanRequire saleMonthPlanRequire);

    /**
     * 根据条件查询统计数据
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    SaleMonthPlanRequireReportVo getSummaryVo(SaleMonthPlanRequire queryVO);
}
