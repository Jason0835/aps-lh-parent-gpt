package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixRubberPlan;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 胶料日计划计划Service接口
 * 
 * @author zlt
 * @date 2021-11-10
 */
public interface MixRubberPlanService
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
    @Transactional
    public int insertMixRubberPlan(MixRubberPlan mixRubberPlan);

    /**
     * 修改胶料日计划计划
     * 
     * @param mixRubberPlan 胶料日计划计划
     * @return 结果
     */
    @Transactional
    public int updateMixRubberPlan(MixRubberPlan mixRubberPlan);

    /**
     * 批量删除胶料日计划计划
     * 
     * @param ids 需要删除的胶料日计划计划ID
     * @return 结果
     */
    @Transactional
    public int deleteMixRubberPlanByIds(Long[] ids);

    /**
     * 删除胶料日计划计划信息
     * 
     * @param id 胶料日计划计划ID
     * @return 结果
     */
    @Transactional
    public int deleteMixRubberPlanById(Long id);

    /**
     * 校验胶料日计划计划唯一性
     */
    public String checkMixRubberPlanUnique(MixRubberPlan mixRubberPlan);

    /**
     * 导入胶料日计划计划数据
     */
    @Transactional
    public AjaxResult importData(List<MixRubberPlan> list, boolean updateSupport, Long importLogId);
}
