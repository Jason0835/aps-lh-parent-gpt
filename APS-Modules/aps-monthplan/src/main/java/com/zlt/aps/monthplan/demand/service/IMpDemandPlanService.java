package com.zlt.aps.monthplan.demand.service;

import java.util.List;
import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpDemandPlanService.java
 * 描    述：IMpDemandPlanService需求计划后端接口
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpDemandPlanService  extends IBaseService<MpDemandPlan>
{
    /**
     * 查询需求计划
     * 
     * @param id 需求计划主键
     * @return 需求计划
     */
    public MpDemandPlan selectMpDemandPlanById(Long id);

    /**
     * 查询需求计划列表
     * 
     * @param mpDemandPlan 需求计划
     * @return 需求计划集合
     */
    public List<MpDemandPlan> selectMpDemandPlanList(MpDemandPlan mpDemandPlan);

    /**
     * 批量查询需求计划列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 需求计划集合
     */
    public List<MpDemandPlan> selectMpDemandPlanByIds(List<Long> ids);


    /**
     * 新增需求计划
     * 
     * @param mpDemandPlan 需求计划
     * @return 结果
     */
    @Transactional
    public int insertMpDemandPlan(MpDemandPlan mpDemandPlan);

    /**
     * 修改需求计划
     * 
     * @param mpDemandPlan 需求计划
     * @return 结果
     */
    @Transactional
    public int updateMpDemandPlan(MpDemandPlan mpDemandPlan);

    /**
     * 批量删除需求计划
     * 
     * @param ids 需要删除的需求计划主键集合
     * @return 结果
     */
   
    @Transactional
    public int deleteMpDemandPlanByIds(Long[] ids);

    /**
     * 批量删除需求计划
     *
     * @param ids 需要删除的需求计划主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMpDemandPlanByIds(List<Long> ids);

    /**
     * 删除需求计划信息
     * 
     * @param id 需求计划主键
     * @return 结果
     */
    @Transactional
    public int deleteMpDemandPlanById(Long id);

    /**
     * 校验需求计划唯一性
     */
    public String checkMpDemandPlanUnique(MpDemandPlan mpDemandPlan);

    /**
     * 导入需求计划数据
     */
    @Transactional
    public AjaxResult importData(List<MpDemandPlan> list, boolean updateSupport, Long importLogId);
}
