package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineMonthPlanSurplus;

import java.util.List;

/**
  *  成型工序月度汇总表mapper
  * @ClassName CxEngineMonthPlanSurplusMapper
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/22 18:22
  * @Version 1.0
**/
public interface CxEngineMonthPlanSurplusMapper {

    /**
     * 根据成型排程批次号获取月度汇总数据
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    List<CxEngineMonthPlanSurplus> selectCxMonthPlanSurplusList(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus);

    /**
     * 插单生成成型外胎汇总表数据
     * @param cxEngineMonthPlanSurplus
     * @return
     */
    int insertCxMonthPlanSurplus(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus);

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
