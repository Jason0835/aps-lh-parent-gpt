package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixRubberPlan;

/**
 * 胶料日计划计划Mapper接口
 * 
 * @author zlt
 * @date 2021-11-10
 */
public interface MixRubberPlanMapper 
{
    /**
     * 查询胶料日计划计划
     * 
     * @param id 胶料日计划计划ID
     * @return 胶料日计划计划
     */
    public MixRubberPlan selectMixRubberPlanById(Long id);

    /**
     * 查询胶料日计划计划列表
     * 
     * @param mixRubberPlan 胶料日计划计划
     * @return 胶料日计划计划集合
     */
    public List<MixRubberPlan> selectMixRubberPlanList(MixRubberPlan mixRubberPlan);

    /**
     * 新增胶料日计划计划
     * 
     * @param mixRubberPlan 胶料日计划计划
     * @return 结果
     */
    public int insertMixRubberPlan(MixRubberPlan mixRubberPlan);

    /**
     * 修改胶料日计划计划
     * 
     * @param mixRubberPlan 胶料日计划计划
     * @return 结果
     */
    public int updateMixRubberPlan(MixRubberPlan mixRubberPlan);

    /**
     * 删除胶料日计划计划
     * 
     * @param id 胶料日计划计划ID
     * @return 结果
     */
    public int deleteMixRubberPlanById(Long id);

    /**
     * 批量删除胶料日计划计划
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixRubberPlanByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixRubberPlan> list);
}
