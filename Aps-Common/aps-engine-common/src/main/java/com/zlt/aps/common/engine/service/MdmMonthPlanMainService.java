package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;

import java.util.Date;
import java.util.List;

/**
 * planmainService接口
 * 
 * @author Joran.zhang
 * @date 2021-06-24
 */
public interface MdmMonthPlanMainService
{
    /**
     * 查询planmain
     * 
     * @param id planmainID
     * @return planmain
     */
    public MdmMonthPlanMain selectMdmMonthPlanMainById(Long id);

    /**
     * 查询planmain列表
     * 
     * @param mdmMonthPlanMain planmain
     * @return planmain集合
     */
    public List<MdmMonthPlanMain> selectMdmMonthPlanMainList(MdmMonthPlanMain mdmMonthPlanMain);

    /**
     * 新增planmain
     * 
     * @param mdmMonthPlanMain planmain
     * @return 结果
     */
    public int insertMdmMonthPlanMain(MdmMonthPlanMain mdmMonthPlanMain);

    /**
     * 修改planmain
     * 
     * @param mdmMonthPlanMain planmain
     * @return 结果
     */
    public int updateMdmMonthPlanMain(MdmMonthPlanMain mdmMonthPlanMain);

    /**
     * 批量删除planmain
     * 
     * @param ids 需要删除的planmainID
     * @return 结果
     */
    public int deleteMdmMonthPlanMainByIds(Long[] ids);

    /**
     * 删除planmain信息
     * 
     * @param id planmainID
     * @return 结果
     */
    public int deleteMdmMonthPlanMainById(Long id);

    int deleteByApsVersion(String apsVersion);

    int deleteByYearAndMonthAndIsFinal(String year, String month, String isFinal);

    /**
     * 根据排程日期获取月度计划主表中存在且定稿的数据
     * @param scheduleDate
     * @return
     */
    public MdmMonthPlanMain getValidPlanMainVersion(Date scheduleDate);

    /**
     * 查最新的主表信息
     * @return
     */
    public MdmMonthPlanMain selectNewestPlanMain();
}
