package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmMustFinishPlan;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMustFinishPlanService.java
 * 描    述：IMdmMustFinishPlanService必须保证的客户月计划后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
public interface IMdmMustFinishPlanService {
    /**
     * 查询必须保证的客户月计划
     *
     * @param id 必须保证的客户月计划主键
     * @return 必须保证的客户月计划
     */
    public MdmMustFinishPlan selectMdmMustFinishPlanById(Long id);

    /**
     * 查询必须保证的客户月计划列表
     *
     * @param mdmMustFinishPlan 必须保证的客户月计划
     * @return 必须保证的客户月计划集合
     */
    public List<MdmMustFinishPlan> selectMdmMustFinishPlanList(MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 新增必须保证的客户月计划
     *
     * @param mdmMustFinishPlan 必须保证的客户月计划
     * @return 结果
     */
    @Transactional
    public int insertMdmMustFinishPlan(MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 修改必须保证的客户月计划
     *
     * @param mdmMustFinishPlan 必须保证的客户月计划
     * @return 结果
     */
    @Transactional
    public int updateMdmMustFinishPlan(MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 批量删除必须保证的客户月计划
     *
     * @param ids 需要删除的必须保证的客户月计划主键集合
     * @return 结果
     */

    @Transactional
    public int deleteMdmMustFinishPlanByIds(Long[] ids);

    /**
     * 校验必须保证的客户月计划唯一性
     */
    public String checkMdmMustFinishPlanUnique(MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 导入必须保证的客户月计划数据
     */
    @Transactional
    public AjaxResult importData(List<MdmMustFinishPlan> list, boolean updateSupport, Long importLogId);
}
