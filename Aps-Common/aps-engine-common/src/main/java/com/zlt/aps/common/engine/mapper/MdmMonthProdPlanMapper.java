package com.zlt.aps.common.engine.mapper;
import java.util.Collection;

import java.util.List;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import org.apache.ibatis.annotations.Param;

/**
 * 主计划月度生产计划Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-06-24
 */
public interface MdmMonthProdPlanMapper 
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

    List<MdmMonthProdPlan> getByParams(MdmMonthProdPlan entity);

    /**
     * 新增主计划月度生产计划
     * 
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 结果
     */
    public int insertMdmMonthProdPlan(MdmMonthProdPlan mdmMonthProdPlan);

    int deleteByMonthPlanApsVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);

    int insertBatch(@Param("mdmMonthProdPlanCollection") List<MdmMonthProdPlan> mdmMonthProdPlanCollection);
    /**
     * 修改主计划月度生产计划
     * 
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 结果
     */
    public int updateMdmMonthProdPlan(MdmMonthProdPlan mdmMonthProdPlan);

    int updateByPrimaryKey(MdmMonthProdPlan entity);

    /**
     * 删除主计划月度生产计划
     * 
     * @param id 主计划月度生产计划ID
     * @return 结果
     */
    public int deleteMdmMonthProdPlanById(Long id);

    /**
     * 批量删除主计划月度生产计划
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMdmMonthProdPlanByIds(Long[] ids);

    List<MdmMonthProdPlan> selectAllByMonthPlanApsVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);

    List<MdmMonthProdPlan> selectAllByMonthPlanApsVersionOld(@Param("monthPlanApsVersion") String monthPlanApsVersion);

    /**
     * 根据月度计划生产排程版本进行月度计划明细汇总
     * @param monthPlanApsVersion
     * @return
     */
    public List<MdmMonthProdPlan> selectMonthTotalPlanQtyByApsVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);

    /**
     * 月底排计划获取新的月度计划初稿的数据
     * @param year 年
     * @param month 月
     * @return
     */
    List<MdmMonthProdPlan> selectMonthTotalPlanQtyByNextMonthDraft(@Param("year") String year,@Param("month") String month,@Param("isFinalized")String isFinalized,@Param("monthPlanApsVersion") String monthPlanApsVersion);
}
