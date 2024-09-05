package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhMoldAdjustPlan;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 硫化模具调整计划Service接口
 *
 * @author chen
 * @date 2022-03-23
 */
public interface LhMoldAdjustPlanService {
    /**
     * 查询硫化模具调整计划
     *
     * @param id 硫化模具调整计划ID
     * @return 硫化模具调整计划
     */
    public LhMoldAdjustPlan selectLhMoldAdjustPlanById(Long id);

    /**
     * 查询硫化模具调整计划列表
     *
     * @param lhMoldAdjustPlan 硫化模具调整计划
     * @return 硫化模具调整计划集合
     */
    public List<LhMoldAdjustPlan> selectLhMoldAdjustPlanList(LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 新增硫化模具调整计划
     *
     * @param lhMoldAdjustPlan 硫化模具调整计划
     * @return 结果
     */
    @Transactional
    public int insertLhMoldAdjustPlan(LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 修改硫化模具调整计划
     *
     * @param lhMoldAdjustPlan 硫化模具调整计划
     * @return 结果
     */
    @Transactional
    public int updateLhMoldAdjustPlan(LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 批量删除硫化模具调整计划
     *
     * @param ids 需要删除的硫化模具调整计划ID
     * @return 结果
     */
    @Transactional
    public int deleteLhMoldAdjustPlanByIds(Long[] ids);

    /**
     * 删除硫化模具调整计划信息
     *
     * @param id 硫化模具调整计划ID
     * @return 结果
     */
    @Transactional
    public int deleteLhMoldAdjustPlanById(Long id);

    /**
     * 校验硫化模具调整计划唯一性
     */
    public String checkLhMoldAdjustPlanUnique(LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 导入硫化模具调整计划数据
     */
    @Transactional
    public AjaxResult importData(List<LhMoldAdjustPlan> list, boolean updateSupport, Long importLogId);
}
