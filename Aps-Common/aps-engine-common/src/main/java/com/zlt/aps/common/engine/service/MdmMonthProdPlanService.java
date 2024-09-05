package com.zlt.aps.common.engine.service;

import java.util.List;
import java.util.Map;

import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;

/**
 * 主计划月度生产计划Service接口
 * 
 * @author Joran.zhang
 * @date 2021-06-24
 */
public interface MdmMonthProdPlanService
{
    /**
     * 查询主计划月度生产计划
     * 
     * @param id 主计划月度生产计划ID
     * @return 主计划月度生产计划
     */
    public MdmMonthProdPlan selectMdmMonthProdPlanById(Long id);

    /**
     * 查询主计划月度生产计划列表
     * 
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 主计划月度生产计划集合
     */
    public List<MdmMonthProdPlan> selectMdmMonthProdPlanList(MdmMonthProdPlan mdmMonthProdPlan);

    public List<MdmMonthProdPlan> getByParams(MdmMonthProdPlan mdmMonthProdPlan);

    List<MdmMonthProdPlan> getByApsVersion(String apsVersion);

    List<MdmMonthProdPlan> getByApsVersionOld(String apsVersion);

    int update(MdmMonthProdPlan entity);

    void deleteByApsVersion(String apsVersion);

    /**
     * 根据月度计划生产排程版本进行月度计划明细汇总
     * @param monthPlanApsVersion
     * @return
     */
    public List<MdmMonthProdPlan> selectMonthTotalPlanQtyByApsVersion(String monthPlanApsVersion);

    void insertBatch(List<MdmMonthProdPlan> prodList);

    /**
     * 根据年月获取下个月初稿的数据集合
     * key sap+胎胚代码
     * @param year 年
     * @param month 月
     * @param isFinalized  是否定稿
     * @return
     */
    Map<String,MdmMonthProdPlan> nextMonthPlanDraft(String year,String month,String isFinalized,String monthPlanApsVersion);

    /**
     * 查询月度计划列表
     * @param monthPlanApsVersion
     * @return
     */
    Map<String,List<MdmMonthProdPlan>> selectMonthPlanListBymonthPlanApsVersion(String monthPlanApsVersion);
}
