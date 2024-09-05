package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMoldAdjustPlan;

import java.util.List;

/**
 * 硫化模具调整计划Mapper接口
 *
 * @author chen
 * @date 2022-03-23
 */
public interface LhMoldAdjustPlanMapper {
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
    public int insertLhMoldAdjustPlan(LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 修改硫化模具调整计划
     *
     * @param lhMoldAdjustPlan 硫化模具调整计划
     * @return 结果
     */
    public int updateLhMoldAdjustPlan(LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 删除硫化模具调整计划
     *
     * @param id 硫化模具调整计划ID
     * @return 结果
     */
    public int deleteLhMoldAdjustPlanById(Long id);

    /**
     * 批量删除硫化模具调整计划
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhMoldAdjustPlanByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<LhMoldAdjustPlan> list);
}
