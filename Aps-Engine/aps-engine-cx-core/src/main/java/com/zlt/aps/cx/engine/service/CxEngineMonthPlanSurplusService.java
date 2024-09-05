package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineMonthPlanSurplus;

import java.util.List;
import java.util.Map;

/**
  * 获取成型工序月度汇总表数据
  * @ClassName CxEngineMonthPlanSurplusService
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/23 15:36
  * @Version 1.0
**/
public interface CxEngineMonthPlanSurplusService {

    /**
     * 根据成型批次号获取月度汇总列表数据
     * @param monthPlanApsVersion 月度计划版本APS版本号
     * @return
     */
    Map<String,CxEngineMonthPlanSurplus> listCxMonthPlanSurplusByMonthPlanApsVersion(String monthPlanApsVersion);

    /**
     * 插单生成成型外胎汇总表数据
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    int insertCxMonthPlanSurplus(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus);

    /**
     * 根据条件查询数据列表
     * @param condition
     * @return
     */
    List<CxEngineMonthPlanSurplus> listCxEngineMonthPlanSurplus(CxEngineMonthPlanSurplus condition);

    /**
     * 更新插单计划量数据
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    int updateCxMonthPlanSurplus(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus);

    /**
     * 自动排程删除当天插单数据
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    int deleteMonthPlanSurplusByDataSource(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus);

    /**
     *  插单规格进行调量修正量，外胎汇总表计划调整量，月度剩余量进行更新
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    int updateMonthPlanSurplusBySapCodeVersion(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus);
}
