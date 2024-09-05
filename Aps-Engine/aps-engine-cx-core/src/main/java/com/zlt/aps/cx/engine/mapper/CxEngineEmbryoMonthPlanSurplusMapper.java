package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;

import java.util.List;

/**
 *  成型胎胚月度汇总表
 */
public interface CxEngineEmbryoMonthPlanSurplusMapper {

    /**
     * 成型胎胚维度月度汇总表列表
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    public List<CxEngineEmbryoMonthPlanSurplus> selectCxEmbryoMonthPlanSurplusList(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus);

    /**
     * 新增胎胚月度计划汇总表
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    int insertCxEmbryoMonthPlanSurplus(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus);

    /**
     * 更新胎胚月度计划汇总表计划量
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    int updateCxEmbryoMonthPlanSurplus(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus);

    /**
     * 自动排程删除当天插单胎胚汇总数据
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    int deleteEmbryoMonthPlanSurplusByDataSource(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus);

    /**
     * 插单调整更新计划调整量和月度剩余量
     * @param cxEngineEmbryoMonthPlanSurplus
     * @return
     */
    int updateMonthPlanSurplusByEmbryoCodeVersion(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus);
}




